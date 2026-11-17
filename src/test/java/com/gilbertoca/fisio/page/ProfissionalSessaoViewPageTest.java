package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Usuario;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

/**
 * Teste do ciclo real do painel do profissional (ProfissionalSessaoViewPage)
 * e da navegação cruzada nas listagens de agendamento/atendimento: GET pelo
 * ClickServlet, carga do profissional, tabela de sessões dele (agendadas e
 * atendidas), links por linha para a sala e os links das colunas de
 * cliente/profissional nas listagens.
 */
public class ProfissionalSessaoViewPageTest {

    private static MockContainer container;
    private static Integer profissionalId;
    private static Integer clienteId;
    private static Integer sessaoAgendadaId;
    private static Integer sessaoRealizadaId;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_painel_profissional_page_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        container = new MockContainer("src/main/webapp");
        container.start();

        // Usuário autenticado na sessão persistente do container — o gate
        // de AuthenticatedPage (onSecurityCheck) exige o SESSION_USER.
        Usuario usuarioTeste = new Usuario();
        usuarioTeste.setId(1);
        usuarioTeste.setUsername("admin");
        container.getSession().setAttribute(AuthenticatedPage.SESSION_USER, usuarioTeste);

        seedCenario();
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
        DataSourceManager.shutdown();
    }

    /** Profissional + cliente + modalidade + uma sessão AGENDADA e uma REALIZADA. */
    private static void seedCenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            profissionalId = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dra. Painel Prof', '33344455566', '1985-09-01', 'profpainel@fisioclin.com', '86999992222', 'CREFITO-333-P', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            clienteId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Rafael Painel Prof', '44455566677', '1995-11-30', '86999993333', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates Prof', 50, 110.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            sessaoAgendadaId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-03-12 09:00:00', '2026-03-12 09:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("clienteId", clienteId)
                    .addParameter("profissionalId", profissionalId)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            sessaoRealizadaId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao, registro_dt_inicio, registro_dt_fim) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-02-12 09:00:00', '2026-02-12 09:50:00', 'TRATAMENTO', 'PARTICULAR', 'REALIZADA', '2026-02-12 09:05:00', '2026-02-12 09:45:00')", true)
                    .addParameter("clienteId", clienteId)
                    .addParameter("profissionalId", profissionalId)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    /** GET do painel: ficha do profissional e as DUAS sessões com links. */
    @Test
    public void deveExibirFichaESessoesDoProfissionalNoGet() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        request.setParameter("id", profissionalId.toString());
        container.setRequest(request);

        container.testPage(ProfissionalSessaoViewPage.class);

        String html = container.getResponse().getDocument();

        assertTrue("A ficha do profissional deveria estar no painel.",
                html.contains("Dra. Painel Prof"));
        assertTrue("A sessão AGENDADA deveria aparecer na tabela (label do enum).",
                html.contains("Agendada"));
        assertTrue("A sessão REALIZADA deveria aparecer na tabela (label do enum).",
                html.contains("Realizada"));
        assertTrue("Cada sessão deveria ter link para a sala de atendimento.",
                html.contains("sessao-prontuario-edit.htm?id=" + sessaoAgendadaId)
                        && html.contains("sessao-prontuario-edit.htm?id=" + sessaoRealizadaId));
    }

    /** GET sem id navega de volta: sem painel, sem tabela. */
    @Test
    public void deveRedirecionarAoListarSemId() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(ProfissionalSessaoViewPage.class);

        assertEquals("Sem id o painel deve redirecionar à listagem de profissionais.",
                "/mock/profissional-view.htm", container.getResponse().getRedirectUrl());
    }

    /** A listagem de profissionais oferece a ação Visualizar por linha. */
    @Test
    public void deveListarAcaoVisualizarNaListagemDeProfissionais() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(ProfissionalViewPage.class);

        String html = container.getResponse().getDocument();
        assertTrue("A listagem deveria ter a ação Visualizar para o painel do profissional.",
                html.contains("profissional-sessao-view.htm?id=" + profissionalId));
        assertTrue("O botão Novo profissional (macro renderBotaoNovo) deveria renderizar.",
                html.contains("<button>Novo profissional</button>"));
    }

    /**
     * Navegação cruzada na listagem de agendamento (SessaoViewPage): os nomes
     * de cliente e profissional renderizam como link para os painéis.
     */
    @Test
    public void deveLinkarPaineisNasColunasDaAgenda() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(SessaoViewPage.class);

        String html = container.getResponse().getDocument();
        assertTrue("O nome do cliente deveria linkar ao painel do cliente.",
                html.contains("cliente-sessao-view.htm?id=" + clienteId));
        assertTrue("O nome do profissional deveria linkar ao painel do profissional.",
                html.contains("profissional-sessao-view.htm?id=" + profissionalId));
    }

    /** Mesma navegação cruzada na listagem de atendimentos (sala). */
    @Test
    public void deveLinkarPaineisNasColunasDaListaDeAtendimentos() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(SessaoProntuarioViewPage.class);

        String html = container.getResponse().getDocument();
        assertTrue("O nome do cliente deveria linkar ao painel do cliente.",
                html.contains("cliente-sessao-view.htm?id=" + clienteId));
        assertTrue("O nome do profissional deveria linkar ao painel do profissional.",
                html.contains("profissional-sessao-view.htm?id=" + profissionalId));
    }
}
