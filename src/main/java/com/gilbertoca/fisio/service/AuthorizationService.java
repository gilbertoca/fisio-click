package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Usuario;
import java.util.List;
import org.sql2o.Connection;

/**
 * Autorização: resolve as permissões efetivas do usuário (união das
 * permissões dos papéis dele) e responde se uma permissão
 * <code>modulo:acao</code> é concedida.
 *
 * Casamento de wildcard, no formato do seed (V6):
 * <ul>
 *   <li><code>*</code> — concede tudo (Administrador);</li>
 *   <li><code>modulo:*</code> — concede toda ação do módulo (Gerente);</li>
 *   <li><code>modulo:acao</code> — concede a ação exata.</li>
 * </ul>
 *
 * É este serviço que a página (AuthenticatedPage) e o menu
 * (PermissaoAccessController) consultam — ninguém monta esta lógica
 * duas vezes.
 */
public class AuthorizationService {

    /** Permissões concedidas ao usuário (nomes, sem duplicar entre papéis). */
    public List<String> getPermissoes(Integer usuarioId) {
        if (usuarioId == null) {
            return List.of();
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("usuario.getPermissoes"))
                    .addParameter("usuarioId", usuarioId)
                    .executeScalarList(String.class);
        }
    }

    /** A permissão exigida (modulo:acao) está entre as concedidas? */
    public boolean temPermissao(Usuario usuario, String exigida) {
        if (usuario == null || exigida == null) {
            return false;
        }
        return concede(getPermissoes(usuario.getId()), exigida);
    }

    /**
     * Alguma das permissões concedidas (com wildcard) casa com a exigida?
     * Versão em lote da temPermissao — o card "Permissões" da tela de
     * usuário julga o vocabulário inteiro com ela.
     */
    public boolean concede(List<String> concedidas, String exigida) {
        if (concedidas == null || exigida == null) {
            return false;
        }
        for (String concedida : concedidas) {
            if (casa(concedida, exigida)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Casamento de wildcard entre uma permissão concedida e a exigida.
     * Estático e puro: testável sem banco.
     */
    static boolean casa(String concedida, String exigida) {
        if (concedida == null || exigida == null) {
            return false;
        }
        if ("*".equals(concedida) || concedida.equals(exigida)) {
            return true;
        }
        // "modulo:*" concede toda ação do módulo ("sessao:*" casa "sessao:atender")
        if (concedida.endsWith(":*")) {
            String modulo = concedida.substring(0, concedida.length() - 1); // "sessao:"
            return exigida.startsWith(modulo);
        }
        return false;
    }
}
