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
 * Teste do ciclo real do painel do cliente (ClienteSessaoViewPage): GET pelo
 * ClickServlet com o id @Bindable, carga do cliente, tabela de sessões dele
 * (agendadas e atendidas) e os links por linha para a sala de atendimento.
 */
public class ClienteSessaoViewPageTest {

    private static MockContainer container;
    private static Integer clienteId;
    private static Integer sessaoAgendadaId;
    private static Integer sessaoRealizadaId;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_painel_cliente_page_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
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

    /** Cliente + profissional + modalidade + uma sessão AGENDADA e uma REALIZADA. */
    private static void seedCenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            clienteId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, email, cliente_situacao, endereco, bairro, cep, cidade) "
                    + "VALUES ('Carla Painel Test', '11122233344', '1990-05-10', '86999990011', 'carla@painel.com', 'ATIVO', 'Rua das Acácias, 123', 'Centro', '64000123', 'Teresina')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dr. Painel Test', '22233344455', '1980-03-20', 'painel@fisioclin.com', '86888777111', 'CREFITO-111-P', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('Pilates Painel', 50, 100.00, 4)", true)
                    .executeUpdate().getKey(Integer.class);

            sessaoAgendadaId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-03-10 08:00:00', '2026-03-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')", true)
                    .addParameter("clienteId", clienteId)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            sessaoRealizadaId = conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao, registro_dt_inicio, registro_dt_fim) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-02-10 08:00:00', '2026-02-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'REALIZADA', '2026-02-10 08:05:00', '2026-02-10 08:45:00')", true)
                    .addParameter("clienteId", clienteId)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    /**
     * O painel GET exibe a ficha do cliente e as DUAS sessões (AGENDADA e
     * REALIZADA), cada uma com link para a sala de atendimento.
     */
    @Test
    public void deveExibirFichaESessoesDoClienteNoGet() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        request.setParameter("id", clienteId.toString());
        container.setRequest(request);

        container.testPage(ClienteSessaoViewPage.class);

        String html = container.getResponse().getDocument();

        assertTrue("A ficha do cliente deveria estar no painel.",
                html.contains("Carla Painel Test"));
        // Endereço novo (V3) deve renderizar na ficha do painel
        assertTrue("O endereço deveria aparecer na ficha do painel.",
                html.contains("Rua das Acácias, 123"));
        assertTrue("A cidade deveria aparecer na ficha do painel.",
                html.contains("Teresina"));
        // A coluna situação renderiza o toString() português do enum
        assertTrue("A sessão AGENDADA deveria aparecer na tabela.",
                html.contains("Agendada"));
        assertTrue("A sessão REALIZADA deveria aparecer na tabela.",
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

        container.testPage(ClienteSessaoViewPage.class);

        assertEquals("Sem id o painel deve redirecionar à listagem de clientes.",
                "/mock/cliente-view.htm", container.getResponse().getRedirectUrl());
    }

    /** A listagem de clientes oferece a ação Visualizar por linha. */
    @Test
    public void deveListarAcaoVisualizarNaListagemDeClientes() {
        MockRequest request = new MockRequest();
        request.setSession(container.getSession());
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(ClienteViewPage.class);

        String html = container.getResponse().getDocument();
        assertTrue("A listagem deveria ter a ação Visualizar para o painel do cliente.",
                html.contains("cliente-sessao-view.htm?id=" + clienteId));
        assertTrue("O botão Novo cliente (macro renderBotaoNovo) deveria renderizar.",
                html.contains("<button>Novo cliente</button>"));
    }
}
