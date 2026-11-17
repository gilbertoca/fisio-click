package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.AuthorizationService;
import com.gilbertoca.fisio.service.ConectadosService;
import org.apache.click.Context;

/**
 * Base para páginas protegidas. Dois níveis no mesmo gate
 * (onSecurityCheck — a porta de entrada canônica do Click):
 *
 * 1. Autenticação: exige um Usuario na sessão; sem ele, volta para o login.
 * 2. Autorização: a página declara as permissões exigidas
 *    (permissoesExigidas, formato modulo:acao) — qualquer uma delas permite
 *    a entrada (any-of); sem nenhuma, avisa e volta para a Home.
 */
public abstract class AuthenticatedPage extends BorderPage {

    public static final String SESSION_USER = "authenticatedUser";

    /** Sessão: a página originalmente pedida (o login volta para ela). */
    public static final String REDIRECT_APOS_LOGIN = "redirectAposLogin";

    private final AuthorizationService authorizationService = new AuthorizationService();

    @Override
    public boolean onSecurityCheck() {
        Context ctx = getContext();
        Object valorSessao = ctx.hasSession() ? ctx.getSessionAttribute(SESSION_USER) : null;
        if (!(valorSessao instanceof Usuario usuario)) {
            // Guarda a página pedida (com query string — o editar com id
            // sobrevive ao login) para o login devolver o usuário a ela.
            // O MockRequest do fork junta parâmetros com a ENTIDADE
            // "&amp;" (bug do mock); request real nunca traz "&amp;"
            // literal — normalizar e cortar resíduo no fim.
            String queryString = ctx.getRequest().getQueryString();
            if (queryString != null) {
                queryString = queryString.replace("&amp;", "&");
                while (queryString.endsWith("&") || queryString.endsWith("?")) {
                    queryString = queryString.substring(0, queryString.length() - 1);
                }
            }
            String destino = getPath() + (queryString == null || queryString.isBlank()
                    ? "" : "?" + queryString);
            ctx.setSessionAttribute(REDIRECT_APOS_LOGIN, destino);
            setRedirect(LoginPage.class);
            return false;
        }

        // Batimento da sessão aberta: atualiza o "Último Acesso" da tela
        // Usuários Conectados (sem registro — sessão criada fora do login
        // — não faz nada).
        if (ctx.hasSession()) {
            ConectadosService.atualizar(ctx.getSession().getId());
        }

        String[] exigidas = permissoesExigidas();
        if (exigidas != null && exigidas.length > 0 && !temAlgumaPermissao(usuario, exigidas)) {
            // Autenticado, mas sem a permissão do módulo: nem error page,
            // nem tela vazia — avisa (flash) e volta para a Home.
            avisar("Você não tem permissão para acessar este recurso.");
            setRedirect(HomePage.class);
            return false;
        }
        return true;
    }

    /**
     * Permissões exigidas pela página (any-of), formato <code>modulo:acao</code>.
     * Default: null = só autenticação (páginas gerais, como a Home).
     */
    protected String[] permissoesExigidas() {
        return null;
    }

    private boolean temAlgumaPermissao(Usuario usuario, String[] exigidas) {
        for (String exigida : exigidas) {
            if (authorizationService.temPermissao(usuario, exigida)) {
                return true;
            }
        }
        return false;
    }

    /** O usuário autenticado desta requisição (null se não houver). */
    protected Usuario getUsuario() {
        Context ctx = getContext();
        Object valorSessao = ctx.hasSession() ? ctx.getSessionAttribute(SESSION_USER) : null;
        return valorSessao instanceof Usuario usuario ? usuario : null;
    }

    /** A permissão (modulo:acao) está concedida ao usuário autenticado? */
    protected boolean temPermissao(String permissao) {
        return authorizationService.temPermissao(getUsuario(), permissao);
    }

    public static boolean isAuthenticated(Context context) {
        return context.hasSession()
                && context.getSessionAttribute(SESSION_USER) != null;
    }
}
