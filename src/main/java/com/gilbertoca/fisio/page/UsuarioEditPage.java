package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.AuthorizationService;
import com.gilbertoca.fisio.service.PapelService;
import com.gilbertoca.fisio.service.UsuarioService;
import java.util.ArrayList;
import java.util.List;
import org.apache.click.control.FieldSet;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.Option;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Select;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;
import org.apache.click.util.Bindable;

/**
 * Manutenção de usuário, no formato CredBem: card "Login" com os dados da
 * conta (Papel é UM por usuário — select; Ativo é SIM/NÃO) e card
 * "Permissões" SOMENTE NA EDIÇÃO, somente leitura — lista
 * "Recurso Ação - SIM/NÃO" que espelha o papel. Sem JavaScript: ao salvar
 * (Atualizar), o PRG volta para a própria tela e a lista re-renderiza com
 * as permissões do papel vigente.
 *
 * Senha: obrigatória NA CRIAÇÃO; na edição, campo vazio = manter a atual
 * (preencher = refaz o hash PBKDF2). Não há confirmação de senha (o
 * CredBem não tem) — a recuperação é pelo próprio fluxo de redefinição.
 */
public class UsuarioEditPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    // Campos em escopo de pacote: o teste da tela os aciona direto
    // (padrão SessaoEditPageTest — testPage + handler).

    final HiddenField idField = new HiddenField("id", Integer.class);
    @Bindable
    protected Integer id;
    final Form form = new Form("form");

    final TextField nome;
    final TextField username;
    final PasswordField senha;
    final TextField email;
    /** Um papel por usuário (CredBem) — substitui a lista de papéis. */
    final Select papel;
    /** SIM/NÃO: a conta desativada preserva o cadastro (não há excluir). */
    final Select ativo;

    private final UsuarioService usuarioService = new UsuarioService();
    private final PapelService papelService = new PapelService();
    private final AuthorizationService authorizationService = new AuthorizationService();

    public UsuarioEditPage() {
        form.setNoLayout(true);

        FieldSet cardLogin = new FieldSet("cardLogin", "Login");
        nome = new TextField("nome", "Nome:", true);
        nome.setFocus(true);
        username = new TextField("username", "Usuário:", true);
        email = new TextField("email", "E-mail:", true);
        senha = new PasswordField("senha", "Senha:");
        papel = new Select("papel", "Papel:");
        papel.add(new Option("", "-- Selecione --"));
        for (com.gilbertoca.fisio.model.Papel p : papelService.getAll()) {
            papel.add(new Option(String.valueOf(p.getId()), p.getNome()));
        }
        ativo = new Select("ativo", "Ativo:");
        ativo.add(new Option("true", "SIM"));
        ativo.add(new Option("false", "NÃO"));
        ativo.setValue("true");
        form.add(idField);
        cardLogin.add(nome);
        cardLogin.add(username);
        cardLogin.add(senha);
        cardLogin.add(email);
        cardLogin.add(papel);
        cardLogin.add(ativo);
        form.add(cardLogin);
        form.add(new Submit("save", "Salvar", this, "onSaveClick"));
        form.add(new Submit("cancel", "Voltar", this, "onCancelClick"));
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Manutenção de Usuário");
    }

    @Override
    public void onGet() {
        if (id == null) {
            return;
        }
        Usuario usuario = usuarioService.get(id);
        if (usuario == null) {
            return;
        }
        // Campos copiados à mão: form.copyFrom(usuario) levaria o HASH
        // para o campo senha — nunca. O id vai para o HiddenField: é ele
        // que volta no POST (o @Bindable sozinho morre na re-submissão —
        // sem isso o POST vira "criação" e estoura a PK).
        idField.setValueObject(usuario.getId());
        nome.setValue(usuario.getNome());
        username.setValue(usuario.getUsername());
        email.setValue(usuario.getEmail());
        List<com.gilbertoca.fisio.model.Papel> papeis = usuarioService.getPapeis(id);
        if (!papeis.isEmpty()) {
            // No select simples, o Option renderiza "selected" contra o
            // value do campo (Option.render) — value E selectedValues.
            String papelId = String.valueOf(papeis.get(0).getId());
            papel.setValue(papelId);
            papel.setSelectedValues(List.of(papelId));
        }
        String valorAtivo = String.valueOf(usuario.isAtivo());
        ativo.setValue(valorAtivo);
        ativo.setSelectedValues(List.of(valorAtivo));
    }

    /**
     * Card "Permissões" (só na edição): lista somente leitura do
     * vocabulário inteiro julgado contra as permissões efetivas do
     * usuário. No onRender — e não no onGet — para sobreviver ao re-render
     * de erro de validação (o POST não passa pelo onGet).
     */
    @Override
    public void onRender() {
        super.onRender();
        if (id == null) {
            return;
        }
        List<String> concedidas = authorizationService.getPermissoes(id);
        List<String> permissoes = new ArrayList<>();
        for (String permissao : papelService.getAllPermissoes()) {
            permissoes.add(PapelService.rotulo(permissao) + " - "
                    + (authorizationService.concede(concedidas, permissao) ? "SIM" : "NÃO"));
        }
        addModel("permissoes", permissoes);
    }

    public boolean onSaveClick() {
        if (!form.isValid()) {
            return true;
        }
        // Papel obrigatório: sem ele o usuário não entra em módulo algum.
        List<String> valoresPapel = papel.getSelectedValues();
        if (valoresPapel.isEmpty() || valoresPapel.get(0).isBlank()) {
            form.setError("Selecione o papel.");
            return true;
        }
        Integer papelId = Integer.valueOf(valoresPapel.get(0));

        // Senha: obrigatória NA CRIAÇÃO; na edição, vazio = manter a atual.
        String senhaPura = senha.getValue();
        if (id == null && (senhaPura == null || senhaPura.isBlank())) {
            form.setError("Informe a senha inicial do usuário.");
            return true;
        }

        // Constraint UNIQUE do V5 como mensagem amável, não como error page.
        if (usuarioService.usernameEmUso(username.getValue(), id)) {
            form.setError("Este username já está em uso.");
            return true;
        }

        Usuario usuario = new Usuario();
        usuario.setNome(nome.getValue());
        usuario.setUsername(username.getValue().trim());
        usuario.setEmail(email.getValue());
        usuario.setAtivo(Boolean.parseBoolean(ativo.getValue()));
        List<Integer> papeisIds = List.of(papelId);

        // O PRG volta para a PRÓPRIA tela (CredBem): o card Permissões
        // re-renderiza com o papel vigente — sem JavaScript.
        if (id != null) {
            usuario.setId(id);
            usuarioService.alterar(usuario, senhaPura, papeisIds);
            setRedirect("usuario-edit.htm?id=" + id);
        } else {
            Integer novoId = usuarioService.criar(usuario, senhaPura, papeisIds);
            setRedirect("usuario-edit.htm?id=" + novoId);
        }
        return false;
    }

    public boolean onCancelClick() {
        setRedirect(UsuarioViewPage.class);
        return false;
    }

    @Override
    protected String[] permissoesExigidas() {
        return new String[] { "usuario:adicionar", "usuario:editar" };
    }
}
