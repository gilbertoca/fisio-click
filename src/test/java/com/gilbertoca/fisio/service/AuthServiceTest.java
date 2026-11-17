package com.gilbertoca.fisio.service;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Autenticação real contra fisio.usuario (banco em memória com as migrações
 * — o usuário semente admin/admin vem do V6, com hash PBKDF2).
 */
public class AuthServiceTest {

    private static AuthService authService;

    @BeforeClass
    public static void setUp() {
        String jdbcUrl = "jdbc:h2:mem:fisio_auth_service_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        com.gilbertoca.fisio.infra.DataSourceManager.initialize(jdbcUrl, "sa", "", "org.h2.Driver");
        com.gilbertoca.fisio.infra.DataSourceManager.runMigrations();
        authService = new AuthService();
    }

    @AfterClass
    public static void tearDown() {
        com.gilbertoca.fisio.infra.DataSourceManager.shutdown();
    }

    @Test
    public void deveAutenticarOUsuarioSementeDoV6() {
        com.gilbertoca.fisio.model.Usuario usuario = authService.authenticate("admin", "admin");
        assertNotNull("O admin semente (V6) deve autenticar.", usuario);
        assertEquals("Administrador", usuario.getNome());
        assertTrue("O usuário deve carregar os papéis para a autorização.",
                usuario.getPapeis().size() > 0);
        assertTrue("O papel do admin deve ser Administrador.",
                usuario.getPapeis().stream().anyMatch(p -> "Administrador".equals(p.getNome())));
    }

    @Test
    public void deveRecusarSenhaErrada() {
        assertNull("Senha errada não deve autenticar.",
                authService.authenticate("admin", "errada"));
    }

    @Test
    public void deveRecusarUsuarioInexistenteSemRevelar() {
        assertNull("Usuário inexistente não deve autenticar.",
                authService.authenticate("fantasma", "admin"));
    }

    @Test
    public void deveRecusarEntradasNulas() {
        assertNull(authService.authenticate(null, "admin"));
        assertNull(authService.authenticate("admin", null));
        assertNull(authService.authenticate(null, null));
    }

    @Test
    public void deveAutenticarComEspacosNoUsername() {
        // trim defensivo: " admin " deve entrar como "admin"
        assertNotNull("Username com espaços deve ser aparado antes da busca.",
                authService.authenticate("  admin  ", "admin"));
    }
}
