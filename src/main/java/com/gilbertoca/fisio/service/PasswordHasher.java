package com.gilbertoca.fisio.service;

import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de senha: PBKDF2WithHmacSHA256 (NIST SP 800-132) via JDK — zero
 * dependência. Salt aleatório de 16 bytes (hex) gerado por inclusão;
 * 100.000 iterações (o seed do V6 usa estes mesmos parâmetros: o usuário
 * admin semente foi gerado com este formato).
 *
 * Uso: create() gera o par salt/hash hex para a tela salvar;
 * verify() compara a senha informada contra o par armazenado —
 * comparação em tempo constante para não vazar timing.
 */
public class PasswordHasher {

    /** Iterações do PBKDF2. Se um dia subir, hashes antigos deixam de
     * verificar — migrar senha na próxima alteração. */
    private static final int ITERACOES = 100_000;
    private static final int BYTES_HASH = 32;
    private static final int BYTES_SALT = 16;

    /** Par salt/hash em hex, pronto para as colunas fisio.usuario.salt/senha. */
    public record Par(String salt, String hash) {
    }

    /** Gera salt aleatório + hash da senha — um par por usuário. */
    public static Par create(String senha) {
        byte[] salt = new byte[BYTES_SALT];
        new SecureRandom().nextBytes(salt);
        return new Par(HexFormat.of().formatHex(salt), HexFormat.of().formatHex(hash(senha, salt)));
    }

    /** Verifica a senha contra o par armazenado (colunas em hex). */
    public static boolean verify(String senha, String saltHex, String hashHex) {
        if (senha == null || saltHex == null || hashHex == null) {
            return false;
        }
        byte[] hashArmazenado = HexFormat.of().parseHex(hashHex);
        byte[] hashCalculado = hash(senha, HexFormat.of().parseHex(saltHex));
        // MessageDigest.isEqual: tempo constante (não vaza quanto acertou)
        return java.security.MessageDigest.isEqual(hashArmazenado, hashCalculado);
    }

    private static byte[] hash(String senha, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, ITERACOES, BYTES_HASH * 8);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao calcular o hash PBKDF2", e);
        }
    }
}
