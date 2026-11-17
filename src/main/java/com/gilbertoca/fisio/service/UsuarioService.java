package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Papel;
import com.gilbertoca.fisio.model.Usuario;
import java.util.List;
import org.sql2o.Connection;

/**
 * Acesso ao cadastro de usuários (módulo de segurança). As credenciais
 * (senha/salt) só entram nas queries de autenticação — as de tela (Fase 4)
 * operam sem elas.
 */
public class UsuarioService {

    /** Busca por username — a via do login. Traz as credenciais para o
     * AuthService verificar o hash. */
    public Usuario getByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("usuario.getByUsername"))
                    .addParameter("username", username)
                    .executeAndFetchFirst(Usuario.class);
        }
    }

    /** Papéis do usuário (ordenados por nome) — base das permissões. */
    public List<Papel> getPapeis(Integer usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("usuario.getPapeis"))
                    .addParameter("usuarioId", usuarioId)
                    .executeAndFetch(Papel.class);
        }
    }

    /** Consulta sem credenciais (exibição). */
    public Usuario get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("usuario.get"))
                    .addParameter("id", id)
                    .executeAndFetchFirst(Usuario.class);
        }
    }
}
