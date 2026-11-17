package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Papel;
import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.AuthService;
import com.gilbertoca.fisio.service.ConectadosService;
import com.gilbertoca.fisio.service.UsuarioService;
import org.apache.click.control.Form;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.util.Bindable;

/**
 * Tela de entrada (módulo de segurança). Autentica contra fisio.usuario
 * (senha PBKDF2 + salt) e guarda o objeto Usuario na sessão — é ele que
 * autoriza as páginas (AuthenticatedPage) e, na Fase 3, as permissões.
 */
public class LoginPage extends BorderPage {

    private final AuthService authService = new AuthService();

    private Form form = new Form("form");
    private TextField username = new TextField("username", "Usuário", true);
    private PasswordField password = new PasswordField("password", "Senha", true);
    @Bindable
    protected String action;

    public LoginPage() {
        form.setNoLayout(true);
        username.setFocus(true);
        form.add(username);
        form.add(password);
        form.add(new Submit("login", "Entrar", this, "onLogin"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Entrar · Fisio");
    }

    public boolean onLogin() {
        if (form.isValid()) {
            Usuario usuario = authService.authenticate(username.getValue(), password.getValue());
            if (usuario != null) {
                getContext().setSessionAttribute(AuthenticatedPage.SESSION_USER, usuario);
                // Registro da sessão aberta — alimenta a tela
                // "Usuários Conectados" (Usuário, Papel, Host, Início).
                StringBuilder nomesPapeis = new StringBuilder();
                for (Papel papel : new UsuarioService().getPapeis(usuario.getId())) {
                    if (nomesPapeis.length() > 0) {
                        nomesPapeis.append(", ");
                    }
                    nomesPapeis.append(papel.getNome());
                }
                ConectadosService.registrar(getContext().getSession().getId(),
                        usuario.getUsername(), nomesPapeis.toString(),
                        getContext().getRequest().getRemoteAddr());
                // Volta para a página originalmente pedida, se houver —
                // só caminho interno (nada de //host ou esquema externo).
                Object destino = getContext().getSessionAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN);
                getContext().removeSessionAttribute(AuthenticatedPage.REDIRECT_APOS_LOGIN);
                if (destino instanceof String caminho
                        && caminho.startsWith("/") && !caminho.startsWith("//")) {
                    setRedirect(caminho);
                } else {
                    setRedirect(HomePage.class);
                }
                return false;
            }
            form.setError("Usuário ou senha inválidos.");
        }
        return true;
    }

    @Override
    public void onGet() {
        // ?action=logout: desloga e volta para a própria tela de entrada
        if ("logout".equals(action)) {
            ConectadosService.remover(getContext().getSession().getId());
            getContext().getSession().invalidate();
            setRedirect(LoginPage.class);
        }
    }
}
