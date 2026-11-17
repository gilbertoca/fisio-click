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

    /** Listagem da tela (sem credenciais), filtros por nome/username
     * opcionais (LIKE) e os papéis agregados (LISTAGG) para a coluna. */
    public List<Usuario> getAllLike(String nome, String username) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("usuario.getAllLike"))
                    .addParameter("nome", nome == null || nome.isBlank() ? null : "%" + nome + "%")
                    .addParameter("username", username == null || username.isBlank() ? null : "%" + username + "%")
                    .executeAndFetch(Usuario.class);
        }
    }

    /** Username já em uso por OUTRO usuário? (formulário: mensagem amável
     * no lugar do estouro da constraint UNIQUE do V5.) */
    public boolean usernameEmUso(String username, Integer idExcluido) {
        if (username == null || username.isBlank()) {
            return false;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            Long count = conn.createQuery(QueryLoader.get("usuario.countByUsername"))
                    .addParameter("username", username.trim())
                    .addParameter("id", idExcluido)
                    .executeScalar(Long.class);
            return count != null && count > 0;
        }
    }

    /**
     * Cria o usuário: hash PBKDF2 da senha + papéis, TUDO numa transação —
     * ou o usuário nasce completo ou não nasce. Devolve o id gerado.
     */
    public Integer criar(Usuario usuario, String senhaPura, List<Integer> papeisIds) {
        if (senhaPura == null || senhaPura.isBlank()) {
            throw new IllegalStateException("Senha obrigatória na criação do usuário.");
        }
        PasswordHasher.Par par = PasswordHasher.create(senhaPura);
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Integer id = conn.createQuery(QueryLoader.get("usuario.create"), true)
                    .bind(usuario)
                    .addParameter("senha", par.hash())
                    .addParameter("salt", par.salt())
                    .executeUpdate().getKey(Integer.class);
            gravarPapeis(conn, id, papeisIds);
            conn.commit();
            return id;
        }
    }

    /**
     * Altera nome/username/email/ativo e a lista de papéis. A senha SÓ é
     * refeita se senhaPura vier preenchida (campo vazio = manter a atual).
     */
    public void alterar(Usuario usuario, String senhaPura, List<Integer> papeisIds) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            conn.createQuery(QueryLoader.get("usuario.update"))
                    .bind(usuario)
                    .executeUpdate();
            if (senhaPura != null && !senhaPura.isBlank()) {
                PasswordHasher.Par par = PasswordHasher.create(senhaPura);
                conn.createQuery(QueryLoader.get("usuario.updateSenha"))
                        .addParameter("id", usuario.getId())
                        .addParameter("senha", par.hash())
                        .addParameter("salt", par.salt())
                        .executeUpdate();
            }
            // Papéis são subtituídos por inteiro (checkboxes da tela).
            conn.createQuery(QueryLoader.get("usuario.deletePapeis"))
                    .addParameter("usuarioId", usuario.getId())
                    .executeUpdate();
            gravarPapeis(conn, usuario.getId(), papeisIds);
            conn.commit();
        }
    }

    /**
     * Exclusão de usuário NÃO EXISTE na tela (padrão CredBem): o acesso
     * se gerencia pelo Ativo (SIM/NÃO) no alterar — a conta desativada
     * preserva o histórico de sessões e a rastreabilidade.
     */

    /** Papéis do usuário, dentro da transação do criar/alterar. */
    private void gravarPapeis(Connection conn, Integer usuarioId, List<Integer> papeisIds) {
        if (papeisIds == null) {
            return;
        }
        for (Integer papelId : papeisIds) {
            conn.createQuery(QueryLoader.get("usuario.createPapel"))
                    .addParameter("usuarioId", usuarioId)
                    .addParameter("papelId", papelId)
                    .executeUpdate();
        }
    }
}
