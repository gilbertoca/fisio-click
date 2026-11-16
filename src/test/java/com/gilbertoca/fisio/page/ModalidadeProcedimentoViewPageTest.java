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
 * Teste do ciclo real do painel da modalidade (ModalidadeProcedimentoViewPage):
 * GET pelo ClickServlet com o id @Bindable, carga da modalidade, ficha de
 * identidade e o catálogo de procedimentos amarrados a ela (TODOS os estados
 * do enum — ATIVO e INATIVO) com o link por linha para a edição.
 */
public class ModalidadeProcedimentoViewPageTest {

    private static MockContainer container;
    private static Integer modalidadeId;
    private static Integer procedimentoAtivoId;
    private static Integer procedimentoInativoId;

    @BeforeClass
    public static void setUpBeforeClass() {
        // Banco em memória compatível com Postgres
        String jdbcUrl = "jdbc:h2:mem:fisio_painel_modalidade_page_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
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

    /** Modalidade + dois procedimentos amarrados: um ATIVO e um INATIVO. */
    private static void seedCenario() {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            modalidadeId = conn.createQuery(
                    "INSERT INTO fisio.modalidade (nome, duracao_minutos, valor_base, capacidade_simultanea) "
                    + "VALUES ('RPG Painel', 50, 120.00, 2)", true)
                    .executeUpdate().getKey(Integer.class);

            procedimentoAtivoId = conn.createQuery(
                    "INSERT INTO fisio.procedimento (modalidade_id, nome, codigo_tuss, procedimento_situacao) "
                    + "VALUES (:modalidadeId, 'Alongamento Global Ativo', '40301012', 'ATIVO')", true)
                    .addParameter("modalidadeId", modalidadeId)
                    .executeUpdate().getKey(Integer.class);

            procedimentoInativoId = conn.createQuery(
                    "INSERT INTO fisio.procedimento (modalidade_id, nome, codigo_tuss, procedimento_situacao) "
                    + "VALUES (:modalidadeId, 'Técnica Retirada de Catálogo', '40301013', 'INATIVO')", true)
                    .addParameter("modalidadeId", modalidadeId)
                    .executeUpdate().getKey(Integer.class);

            conn.commit();
        }
    }

    /**
     * O painel GET exibe a ficha da modalidade e o catálogo completo — o
     * ATIVO entra (toString "Ativo") e o INATIVO também aparece (toString
     * "Inativo"): o painel mostra todos os estados do catálogo.
     */
    @Test
    public void deveExibirFichaECatalogoDaModalidadeNoGet() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setParameter("id", modalidadeId.toString());
        container.setRequest(request);

        container.testPage(ModalidadeProcedimentoViewPage.class);

        String html = container.getResponse().getDocument();

        assertTrue("A ficha da modalidade deveria estar no painel.",
                html.contains("RPG Painel"));
        assertTrue("A duração deveria aparecer na ficha.",
                html.contains("50 min"));
        assertTrue("O valor base deveria aparecer na ficha.",
                html.contains("120"));
        // A coluna situação renderiza o toString() português do enum
        assertTrue("O procedimento ATIVO deveria aparecer no catálogo.",
                html.contains("Alongamento Global Ativo"));
        assertTrue("O toString português 'Ativo' deveria renderizar.",
                html.contains("Ativo"));
        assertTrue("O procedimento INATIVO também entra no catálogo (todos os estados).",
                html.contains("Técnica Retirada de Catálogo"));
        assertTrue("O toString português 'Inativo' deveria renderizar.",
                html.contains("Inativo"));
        // Ações da tabela: link por linha para a edição do procedimento
        assertTrue("Cada procedimento deveria ter link para a edição.",
                html.contains("procedimento-edit.htm?id=" + procedimentoAtivoId)
                        && html.contains("procedimento-edit.htm?id=" + procedimentoInativoId));
    }

    /** GET sem id navega de volta: sem painel, sem tabela. */
    @Test
    public void deveRedirecionarAoListarSemId() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(ModalidadeProcedimentoViewPage.class);

        assertEquals("Sem id o painel deve redirecionar à listagem de modalidades.",
                "/mock/modalidade-view.htm", container.getResponse().getRedirectUrl());
    }

    /** A listagem de modalidades oferece a ação Visualizar por linha. */
    @Test
    public void deveListarAcaoVisualizarNaListagemDeModalidades() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        container.setRequest(request);

        container.testPage(ModalidadeViewPage.class);

        String html = container.getResponse().getDocument();
        assertTrue("A listagem deveria ter a ação Visualizar para o painel da modalidade.",
                html.contains("modalidade-procedimento-view.htm?id=" + modalidadeId));
        assertTrue("O botão Novo (macro renderBotaoNovo) deveria renderizar.",
                html.contains("<button>Nova modalidade</button>"));
    }
}
