package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.infra.DataSourceManager;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

import static org.junit.Assert.*;

/**
 * Evidência do flash de mensagem na exclusão (FlashAttribute do Click,
 * via BorderPage — padrão PRG): a ActionLink do DELETE grava a mensagem na
 * sessão e redireciona para a listagem; a listagem exibe a mensagem UMA
 * vez e o acesso seguinte já não a tem — o F5 não repete nem a mensagem
 * nem a ação. Cobre as duas trilhas: êxito (registro apagável) e bloqueio
 * (registro com dependentes).
 */
public class FlashMensagemExclusaoTest {

    private static MockContainer container;
    private static Integer clienteApagavelId;
    private static Integer clienteComSessaoId;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_flash_exclusao_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        container = new MockContainer("src/main/webapp");
        container.start();

        seedCenario();
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
        DataSourceManager.shutdown();
    }

    /** Dois clientes: um apagável (sem vínculos) e um com sessão agendada. */
    private static void seedCenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            clienteApagavelId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Cliente Apagavel Flash', '11199922288', '1992-01-15', '86123456789', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            clienteComSessaoId = conn.createQuery(
                    "INSERT INTO fisio.cliente (nome, cpf, dt_nascimento, telefone, cliente_situacao) "
                    + "VALUES ('Cliente Bloqueado Flash', '22299933388', '1988-07-20', '86234567890', 'ATIVO')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idProfissional = conn.createQuery(
                    "INSERT INTO fisio.profissional (nome, cpf, dt_nascimento, email, telefone, registro_numero, registro_orgao) "
                    + "VALUES ('Dr. Flash Test', '33399944488', '1980-03-20', 'flash@fisioclin.com', '86345678901', 'CREFITO-888-P', 'CREFITO-3')", true)
                    .executeUpdate().getKey(Integer.class);

            Integer idModalidade = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('RPG Flash', 50, 110.00, 2)", true)
                    .executeUpdate().getKey(Integer.class);

            conn.createQuery(
                    "INSERT INTO fisio.sessao (cliente_id, profissional_id, modalidade_id, dt_hora_inicio, dt_hora_fim, sessao_tipo, pagamento_origem, sessao_situacao) "
                    + "VALUES (:clienteId, :profissionalId, :modalidadeId, '2026-03-10 08:00:00', '2026-03-10 08:50:00', 'TRATAMENTO', 'PARTICULAR', 'AGENDADA')")
                    .addParameter("clienteId", clienteComSessaoId)
                    .addParameter("profissionalId", idProfissional)
                    .addParameter("modalidadeId", idModalidade)
                    .executeUpdate();

            conn.commit();
        }
    }

    /** Request GET amarrado à sessão PERSISTENTE do container: o flash mora
     * na sessão — sem isso cada MockRequest criaria a própria e o teste
     * perderia a mensagem entre as etapas. */
    private static MockRequest novoGet() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setSession(container.getSession());
        container.setRequest(request);
        return request;
    }

    /** Trilha do êxito: DELETE → redirect (PRG) → mensagem UMA vez → F5 limpa. */
    @Test
    public void deveExibirMensagemDeExitoUmaUnicaVez() {
        // 1) ActionLink do DELETE dispara onDelete → flash + redirect
        MockRequest exclusao = novoGet();
        exclusao.setParameter("actionLink", "delete");
        exclusao.setParameter("value", clienteApagavelId.toString());
        container.testPage(ClienteViewPage.class);

        assertEquals("A exclusão deve terminar em redirect (PRG), não na URL do ActionLink.",
                "/mock/cliente-view.htm", container.getResponse().getRedirectUrl());

        // 2) Listagem pós-redirect: mensagem de êxito e registro fora
        novoGet();
        container.testPage(ClienteViewPage.class);
        String html = container.getResponse().getDocument();
        assertTrue("A mensagem de êxito deveria aparecer na listagem.",
                html.contains("Cliente excluído com êxito."));
        assertFalse("O registro apagado deveria ter saído da listagem.",
                html.contains("Cliente Apagavel Flash"));

        // 3) F5 / navegação seguinte: flash consumido, mensagem não repete
        novoGet();
        container.testPage(ClienteViewPage.class);
        assertFalse("O flash deve ser de uma exibição: o F5 não repete a mensagem.",
                container.getResponse().getDocument().contains("Cliente excluído com êxito."));
    }

    /** Trilha do bloqueio: DELETE recusado → redirect (PRG) → aviso UMA vez
     * → F5 limpa, e o cliente com histórico permanece no cadastro. */
    @Test
    public void deveExibirMensagemDeBloqueioUmaUnicaVez() {
        MockRequest exclusao = novoGet();
        exclusao.setParameter("actionLink", "delete");
        exclusao.setParameter("value", clienteComSessaoId.toString());
        container.testPage(ClienteViewPage.class);

        assertEquals("Mesmo bloqueada, a exclusão deve voltar à listagem (PRG).",
                "/mock/cliente-view.htm", container.getResponse().getRedirectUrl());

        novoGet();
        container.testPage(ClienteViewPage.class);
        String html = container.getResponse().getDocument();
        assertTrue("O bloqueio deveria ser avisado na listagem.",
                html.contains("não pode ser excluído"));
        assertTrue("O cliente com histórico permanece no cadastro.",
                html.contains("Cliente Bloqueado Flash"));

        novoGet();
        container.testPage(ClienteViewPage.class);
        assertFalse("O flash deve ser de uma exibição: o F5 não repete o aviso.",
                container.getResponse().getDocument().contains("não pode ser excluído"));
    }
}
