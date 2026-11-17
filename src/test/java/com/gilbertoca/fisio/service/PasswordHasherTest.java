package com.gilbertoca.fisio.service;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * PasswordHasher: PBKDF2 nativo JDK, formato igual ao seed do V6
 * (salt hex 16 bytes, 100.000 iterações, hash hex 32 bytes).
 */
public class PasswordHasherTest {

    @Test
    public void deveVerificarASenhaCorreta() {
        PasswordHasher.Par par = PasswordHasher.create("segredo");
        assertTrue("A senha correta deve verificar.",
                PasswordHasher.verify("segredo", par.salt(), par.hash()));
    }

    @Test
    public void deveRecusarSenhaErrada() {
        PasswordHasher.Par par = PasswordHasher.create("segredo");
        assertFalse("A senha errada não deve verificar.",
                PasswordHasher.verify("errada", par.salt(), par.hash()));
    }

    @Test
    public void deveGerarSaltUnicoPorInclusao() {
        PasswordHasher.Par par1 = PasswordHasher.create("mesmaSenha");
        PasswordHasher.Par par2 = PasswordHasher.create("mesmaSenha");
        assertNotEquals("Hashes da mesma senha devem divergir (salt aleatório).",
                par1.hash(), par2.hash());
        assertNotEquals("Salts devem ser independentes.",
                par1.salt(), par2.salt());
    }

    @Test
    public void deveVerificarOFormatoDoSeedDoV6() {
        // usuário semente admin/admin (V6__insert_permissao.sql)
        assertTrue("O seed do admin (V6) deve verificar neste formato.",
                PasswordHasher.verify("admin",
                        "3ec2a35cdcb56e5cf22e0f4a4d19b88b",
                        "7523a5b833bbdf91a0790ec2c795adbb0b955ca15ce13d6c9b6c64f76e3bde2c"));
    }

    @Test
    public void deveRecusarEntradasNulas() {
        assertFalse(PasswordHasher.verify(null, "s", "h"));
        assertFalse(PasswordHasher.verify("x", null, "h"));
        assertFalse(PasswordHasher.verify("x", "s", null));
    }
}
