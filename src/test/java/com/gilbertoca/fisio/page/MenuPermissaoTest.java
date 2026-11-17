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
 * Autorização no menu e no gate das páginas (Fase 3):
 *
 * 1. Menu filtrado: o Operador (papel 3) vê Agenda/Clientes, mas não
 *    Profissionais nem o submenu Configurações (Usuários/Permissões).
 * 2. Gate server-side: Operador digitando profissional-view.htm direto na
 *    URL não entra — aviso (flash) e volta para a Home.
 * 3. O admin (*) vê o menu completo.
 */
public class MenuPermissaoTest {

    private static MockContainer container;
    private static final int OPERADOR_ID = 2; // admin semente (V6) = 1

    @BeforeClass
    public static void setUpBeforeClass() {
        String jdbcUrl = "jdbc:h2:mem:fisio_menu_permissao_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            // Admin semente do V6 ocupa o id 1 — Operador com id explícito.
            conn.createQuery(
                    "INSERT INTO fisio.usuario (usuario_id, nome, username, email, senha, salt, ativo) "
                    + "VALUES (2, 'Operador Menu', 'operador', 'operador@menu.com', 'x', 'y', TRUE)")
                    .executeUpdate();
            conn.createQuery("INSERT INTO fisio.usuario_papel (usuario_id, papel_id) VALUES (2, 3)")
                    .executeUpdate();
            conn.commit();
        }

        container = new MockContainer("src/main/webapp");
        container.start();
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
        DataSourceManager.shutdown();
    }

    /** Sessão do usuário na sessão PERSISTENTE do container (o gate e o
     * AccessController do menu leem daqui). */
    private static void autenticar(int usuarioId, String username) {
        Usuario usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setUsername(username);
        container.getSession().setAttribute(AuthenticatedPage.SESSION_USER, usuario);
    }

    private static MockRequest novoGet() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setSession(container.getSession());
        container.setRequest(request);
        return request;
    }

    @Test
    public void operadorVeSoOQuePode_EOProfissionaisFicaFora() {
        autenticar(OPERADOR_ID, "operador");
        novoGet();
        container.testPage(HomePage.class);
        String html = container.getResponse().getDocument();

        assertTrue("Operador deve ver a Agenda.",
                html.contains("sessao-view.htm"));
        assertTrue("Operador deve ver Clientes.",
                html.contains("cliente-view.htm"));
        assertFalse("Operador NÃO deve ver Profissionais no menu.",
                html.contains("profissional-view.htm"));
        assertFalse("Operador NÃO deve ver o submenu Configurações.",
                html.contains("Configurações"));
    }

    @Test
    public void adminVeOMenuCompleto() {
        autenticar(1, "admin"); // semente do V6, papel * (Administrador)
        novoGet();
        container.testPage(HomePage.class);
        String html = container.getResponse().getDocument();

        assertTrue("Admin deve ver Profissionais.",
                html.contains("profissional-view.htm"));
        assertTrue("Admin deve ver o submenu Configurações (Usuários/Conectados).",
                html.contains("Configurações"));
        assertTrue(html.contains("usuario-view.htm"));
        assertTrue(html.contains("usuario-conectado-view.htm"));
    }

    @Test
    public void operadorNaoEntraEmProfissionaisPorUrlDireta() {
        autenticar(OPERADOR_ID, "operador");
        novoGet();
        container.testPage(ProfissionalViewPage.class);

        assertEquals("O gate deve devolver o Operador para a Home.",
                "/mock/home.htm", container.getResponse().getRedirectUrl());

        novoGet();
        container.testPage(HomePage.class);
        assertTrue("O bloqueio deve ser avisado (flash) na Home.",
                container.getResponse().getDocument()
                        .contains("Você não tem permissão para acessar este recurso."));
    }
}
