package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.AuthorizationService;
import javax.servlet.http.HttpServletRequest;
import org.apache.click.extras.security.AccessController;

/**
 * AccessController do Click (extras/security) plugado ao nosso modelo de
 * permissões: o recurso pedido (por ex. "sessao:visualizar") é verificado
 * contra as permissões do usuário autenticado — união dos papéis, com
 * wildcard (AuthorizationService). É quem o Menu usa para filtrar itens
 * (MenuFactory.getRootMenu(AccessController) + roles= no menu.xml).
 *
 * Contrato da interface: recurso null = recurso público → acesso concedido
 * (o item "home" e o par Entrar/Sair do menu não têm permissão).
 */
public class PermissaoAccessController implements AccessController {

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Override
    public boolean hasAccess(HttpServletRequest request, String recurso) {
        if (recurso == null) {
            return true;
        }
        javax.servlet.http.HttpSession sessao = request.getSession(false);
        if (sessao == null) {
            return false;
        }
        Object valor = sessao.getAttribute(AuthenticatedPage.SESSION_USER);
        if (valor instanceof Usuario usuario) {
            return authorizationService.temPermissao(usuario, recurso);
        }
        return false;
    }
}
