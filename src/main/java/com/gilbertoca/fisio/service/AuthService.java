package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.model.Usuario;

/**
 * Autenticação real contra fisio.usuario: usuário ativo, hash PBKDF2 +
 * salt verificados pelo PasswordHasher (JDK, zero dependência). O usuário
 * semente (admin) nasce da migração V6 com o mesmo formato.
 *
 * Sucesso devolve o Usuario (com os papéis carregados) — é ele que mora
 * na sessão (AuthenticatedPage.SESSION_USER); a string de login antiga
 * deixou de ser suficiente para autorização (Fase 3).
 */
public class AuthService {

    private final UsuarioService usuarioService = new UsuarioService();

    /**
     * Autentica e devolve o usuário carregado, ou null (usuário inexistente,
     * inativo ou senha errada). A mensagem única é proposital: não revelar
     * qual dos fatores falhou.
     */
    public Usuario authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        Usuario usuario = usuarioService.getByUsername(username.trim());
        if (usuario == null || !usuario.isAtivo()) {
            return null;
        }
        if (!PasswordHasher.verify(password, usuario.getSalt(), usuario.getSenha())) {
            return null;
        }
        usuario.setPapeis(usuarioService.getPapeis(usuario.getId()));
        return usuario;
    }
}
