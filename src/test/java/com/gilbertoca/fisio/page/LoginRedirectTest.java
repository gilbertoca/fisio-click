package com.gilbertoca.fisio.page;

import static org.junit.Assert.*;

import com.gilbertoca.fisio.infra.DataSourceManager;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Volta para a página originalmente pedida após o login: o gate estoca o
 * destino na sessão antes de expulsar o anônimo, e o login consome — com
 * guarda contra redirect externo (//host ou esquema).
 */
public class LoginRedirectTest {

    private static MockContainer container;

    @BeforeClass
    public static void setUpBeforeClass() {
        String jdbcUrl = "jdbc:h2:mem:fisio_login_redirect_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        container = new MockContainer("src/main/webapp");
        container.start();
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
        request.setAttribute("mock_mode_enabled", Boolean.TRUE);
        container.setRequest(request);
        return request;
    }

    /** Login pelo pipeline REAL (POST com form_name, como no navegador). */
    private static void logar(String senha) {
        MockRequest request = new MockRequest();
        request.setMethod("POST");
        request.setSession(container.getSession());
        request.setAttribute("mock_mode_enabled", Boolean.TRUE);
        request.setParameter("form_name", "form");
        request.setParameter("username", "admin");
        request.setParameter("password", senha);
        request.setParameter("login", "Entrar");
        container.setRequest(request);
        container.testPage(LoginPage.class);
    }

    @Test
    public void gateEstocaAPaginaPedidaELoginVoltaParaEla() {
        // Anônimo pede o editar de um usuário (com query string)...
        MockRequest request = novoGet();
        request.setParameter("id", "7");
        container.testPage(UsuarioEditPage.class);

        assertEquals("O gate expulsa o anônimo para o login.",
                "/mock/login.htm", container.getResponse().getRedirectUrl());
        assertEquals("O destino original fica na sessão (com query).",
                "/usuario-edit.htm?id=7",
                container.getSession().getAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN));

        // ...e depois de autenticar, volta para ele — e o estoque consome.
        logar("admin");

        assertEquals("O login deve devolver o usuário à página pedida.",
                "/mock/usuario-edit.htm?id=7", container.getResponse().getRedirectUrl());
        assertNull("O destino deve ser consumido (um uso só).",
                container.getSession().getAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN));
    }

    @Test
    public void loginSemDestinoVaiParaHomeEDestinoExternoECortado() {
        // Sem estoque: login cai na Home.
        logar("admin");
        assertEquals("Sem destino estocado, o login vai para a Home.",
                "/mock/home.htm", container.getResponse().getRedirectUrl());

        // Redirect externo forjado na sessão (URL forjada/ataque): cortado.
        container.getSession().setAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN, "//evil.com");
        logar("admin");
        assertEquals("Destino externo cai na Home.",
                "/mock/home.htm", container.getResponse().getRedirectUrl());
        assertNull("Mesmo cortado, o estoque consome.",
                container.getSession().getAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN));
    }
}
