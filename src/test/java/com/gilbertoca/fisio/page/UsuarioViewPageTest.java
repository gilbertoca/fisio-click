package com.gilbertoca.fisio.page;

import static org.junit.Assert.*;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Usuario;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sql2o.Connection;

/**
 * Listagem de usuários (Fase 4, formato CredBem): colunas Nome, Usuário,
 * Papel (LISTAGG), Ativo em SIM/NÃO; ações SÓ o lápis de Editar (exclusão
 * de usuário não existe — o acesso se gerencia pelo Ativo); e nenhuma
 * credencial (hash/salt do seed do V6) vaza para o HTML.
 */
public class UsuarioViewPageTest {

    private static MockContainer container;

    @BeforeClass
    public static void setUpBeforeClass() {
        String jdbcUrl = "jdbc:h2:mem:fisio_usuario_view_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            // Admin semente do V6 = id 1. Operador (2, papel 3) e Gerente
            // (3, papel 2 = modulo:* de tudo, inclusive usuario).
            conn.createQuery(
                    "INSERT INTO fisio.usuario (usuario_id, nome, username, email, senha, salt, ativo) "
                    + "VALUES (2, 'Operador Listagem', 'operador', 'operador@lista.com', 'x', 'y', TRUE)")
                    .executeUpdate();
            conn.createQuery("INSERT INTO fisio.usuario_papel (usuario_id, papel_id) VALUES (2, 3)")
                    .executeUpdate();
            conn.createQuery(
                    "INSERT INTO fisio.usuario (usuario_id, nome, username, email, senha, salt, ativo) "
                    + "VALUES (3, 'Gerente Listagem', 'gerente', 'gerente@lista.com', 'x', 'y', FALSE)")
                    .executeUpdate();
            conn.createQuery("INSERT INTO fisio.usuario_papel (usuario_id, papel_id) VALUES (3, 2)")
                    .executeUpdate();
            conn.commit();
        }

        container = new MockContainer("src/main/webapp");
        container.start();

        // Admin semente do V6 (id 1, papel *) — quem lista usuários.
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setUsername("admin");
        container.getSession().setAttribute(AuthenticatedPage.SESSION_USER, admin);
    }

    @AfterClass
    public static void tearDown() {
        container.stop();
        DataSourceManager.shutdown();
    }

    private static MockRequest novoGet() {
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setSession(container.getSession());
        // Reamarca o modo mock no request trocado (ver UsuarioEditPageTest).
        request.setAttribute("mock_mode_enabled", Boolean.TRUE);
        container.setRequest(request);
        return request;
    }

    @Test
    public void deveListarComPapelAtivoESemCredenciais() {
        novoGet();
        container.testPage(UsuarioViewPage.class);
        String html = container.getResponse().getDocument();

        assertTrue("A listagem deve mostrar o operador.",
                html.contains("Operador Listagem"));
        assertTrue("A listagem deve mostrar o gerente.",
                html.contains("Gerente Listagem"));
        assertTrue("A coluna Papel deve mostrar o nome do papel (LISTAGG).",
                html.contains("Operador"));
        assertTrue("A coluna Ativo deve renderizar SIM (em PT).",
                html.contains("SIM"));
        assertTrue("A coluna Ativo deve renderizar NÃO (em PT).",
                html.contains("NÃO"));
        assertTrue("O botão Novo usuário mora no cabeçalho do card.",
                html.contains("Novo usuário"));
        assertTrue("O botão Conectados mora no cabeçalho do card.",
                html.contains("Conectados"));

        // O hash PBKDF2 do admin semente (V6) e o salt NUNCA aparecem.
        assertFalse("Credenciais não vazam para a listagem.",
                html.contains("7523a5b833bbdf91a0790ec2c795adbb0b955ca15ce13d6c9b6c64f76e3bde2c"));
        assertFalse("O salt não vaza para a listagem.",
                html.contains("3ec2a35cdcb56e5cf22e0f4a4d19b88b"));
    }

    @Test
    public void exclusaoNaoExisteNaListagem() {
        novoGet();
        container.testPage(UsuarioViewPage.class);
        String html = container.getResponse().getDocument();

        // Exclusão de usuário saiu da UI (padrão CredBem): só o lápis de
        // Editar — e nem o esqueleto do diálogo sobra no HTML.
        assertFalse("Nenhum ícone de excluir na listagem de usuários.",
                html.contains("excluir.svg"));
        assertFalse("Nenhum diálogo de exclusão na listagem de usuários.",
                html.contains("deleteDialog"));
        assertTrue("O lápis de Editar permanece.",
                html.contains("editar.svg"));
    }
}
