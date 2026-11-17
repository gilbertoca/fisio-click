package com.gilbertoca.fisio.page;

import static org.junit.Assert.*;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.ConectadosService;
import org.apache.click.MockContainer;
import org.apache.click.servlet.MockRequest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tela "Usuários Conectados" (Fase 4, formato CredBem): lista as sessões
 * abertas (Usuário, Papel, Host (IP), Último Acesso, Início da Sessão)
 * alimentadas pelo ConectadosService, e o logout remove a sessão da lista.
 */
public class UsuarioConectadoViewPageTest {

    private static MockContainer container;

    @BeforeClass
    public static void setUpBeforeClass() {
        String jdbcUrl = "jdbc:h2:mem:fisio_conectado_view_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        DataSourceManager.runMigrations();

        container = new MockContainer("src/main/webapp");
        container.start();

        // Admin semente do V6 (id 1, papel *) — quem vê conectados.
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setUsername("admin");
        container.getSession().setAttribute(AuthenticatedPage.SESSION_USER, admin);

        ConectadosService.limpar();
        // Duas sessões semeadas (a hora formatada no HTML: HH:mm dd/MM/yyyy).
        ConectadosService.registrar("sessao-operador", "operador",
                "Operador", "192.168.0.44");
        ConectadosService.registrar("sessao-gerente", "gerente",
                "Gerente", "10.0.0.7");
        // Batimento na sessão do operador (Último Acesso distinto).
        ConectadosService.atualizar("sessao-operador");
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
    public void deveListarSessoesAbertasComHostEDatas() {
        novoGet();
        container.testPage(UsuarioConectadoViewPage.class);
        String html = container.getResponse().getDocument();

        assertTrue("A sessão do operador deve estar listada.",
                html.contains("operador"));
        assertTrue("A sessão do gerente deve estar listada.",
                html.contains("gerente"));
        assertTrue("O papel deve acompanhar a sessão.",
                html.contains("Operador"));
        assertTrue("O host (IP) deve estar na tabela.",
                html.contains("192.168.0.44"));
        assertTrue("O host (IP) da outra sessão também.",
                html.contains("10.0.0.7"));
        assertTrue("Último Acesso e Início devem vir formatados (HH:mm dd/MM/yyyy).",
                html.matches("(?s).*\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4}.*"));
        assertTrue("O botão Voltar mora no cabeçalho do card.",
                html.contains("Voltar"));
    }

    @Test
    public void logoutRemoveASessaoDaLista() {
        assertNotNull("Sanidade: a sessão do operador está registrada.",
                ConectadosService.getConectados().stream()
                        .filter(c -> "operador".equals(c.username())).findAny());

        // Sessão PRÓPRIA (não a do container): o logout invalida a sessão,
        // e matar a do container quebraria os demais testes da suíte.
        MockRequest request = new MockRequest();
        request.setMethod("GET");
        request.setAttribute("mock_mode_enabled", Boolean.TRUE);
        container.setRequest(request);
        javax.servlet.http.HttpSession sessaoPropria = request.getSession(true);
        sessaoPropria.setAttribute(AuthenticatedPage.SESSION_USER, adminSemente());

        ConectadosService.registrar(sessaoPropria.getId(), "quemsai",
                "Operador", "10.0.0.9");

        container.testPage(LogoutPage.class);

        assertFalse("O logout deve remover a entrada do registro.",
                ConectadosService.getConectados().stream()
                        .anyMatch(c -> "quemsai".equals(c.username())));
    }

    private static Usuario adminSemente() {
        Usuario admin = new Usuario();
        admin.setId(1);
        admin.setUsername("admin");
        return admin;
    }
}
