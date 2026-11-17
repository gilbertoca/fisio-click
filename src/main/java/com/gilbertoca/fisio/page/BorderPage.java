package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import org.apache.click.Context;
import org.apache.click.Page;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.extras.control.Menu;
import org.apache.click.extras.control.MenuFactory;
import org.apache.click.util.ClickUtils;

/**
 * Base layout page. Every page that needs the shared border template
 * should extend this class.
 *
 * The root menu is loaded from /WEB-INF/menu.xml via MenuFactory and
 * registered as a page control so its head elements are deployed.
 *
 * Two model values are exposed to every template:
 *   - $authenticated : Boolean, true if a user is logged in
 *   - $title         : String, default page title (pages may override)
 *
 * Both are set in onInit(), which is the correct lifecycle phase for
 * request-dependent initialization.
 */
public abstract class BorderPage extends Page {

    protected Menu rootMenu;

    /** Chave do flash de mensagem na sessão — FlashAttribute do Click: o
     * atributo se apaga sozinho no primeiro acesso, então a mensagem (êxito
     * ou bloqueio) é exibida uma única vez (o F5 não repete nem mensagem
     * nem ação). */
    private static final String FLASH_MENSAGEM = "flash.mensagem";

    /** AccessController do menu: criado uma vez e propagado a TODOS os itens
     * — os do menu.xml (via MenuFactory) e os construídos aqui (Entrar/Sair).
     * Menu sem controller estoura IllegalStateException no isUserInRoles()
     * da macro e o template renderiza pela metade. */
    private final PermissaoAccessController menuAccessController = new PermissaoAccessController();

    public BorderPage() {
        // MenuFactory propaga o AccessController a todos os menus: cada item
        // com roles= (menu.xml) é filtrado pela permissão do usuário.
        MenuFactory menuFactory = new MenuFactory();
        rootMenu = menuFactory.getRootMenu(menuAccessController);
    }

    @Override
    public void onInit() {
        super.onInit();

        Context ctx = getContext();
        boolean authenticated = ctx.hasSession()
                && ctx.getSessionAttribute(AuthenticatedPage.SESSION_USER) != null;
        addModel("authenticated", Boolean.valueOf(authenticated));

        Menu authMenu = new Menu(authenticated ? "Sair" : "Entrar");
        authMenu.setPath(authenticated ? "logout.htm" : "login.htm");
        authMenu.setImageSrc("/assets/images/user.svg"); // ícone do menu de autenticação
        // O item de autenticação é público: recurso null no contrato do
        // AccessController. Mas PRECISA do controller — sem ele, o
        // isUserInRoles() da macro estoura e o template renderiza pela metade.
        authMenu.setAccessController(menuAccessController);
        rootMenu.add(authMenu);
        addControl(rootMenu);
        
        // Default page title. Pages can override by calling
        // addModel("title", "...") in their own onInit().
        if (getModel().get("title") == null) {
            addModel("title", "Click + Sql2o");
        }
    }

    @Override
    public String getTemplate() {
        return "/WEB-INF/velocity/border-template.htm";
    }

    /**
     * Publica o flash de mensagem (uma exibição): o valor gravado por
     * {@link #avisar(String)} via Context.setFlashAttribute sobrevive ao
     * redirect e é removido da sessão NO PRÓPRIO ACESSO (FlashAttribute do
     * Click) — o F5 da listagem volta sem mensagem e sem repetir a ação.
     */
    @Override
    public void onRender() {
        super.onRender();
        Object flash = getContext().getSessionAttribute(FLASH_MENSAGEM);
        if (flash != null) {
            addModel("mensagem", flash);
        }
    }

    /** Grava na sessão (FlashAttribute) a mensagem a exibir na PRÓXIMA
     * renderização — o par do redirect (padrão PRG). */
    protected void avisar(String mensagem) {
        if (mensagem != null) {
            getContext().setFlashAttribute(FLASH_MENSAGEM, mensagem);
        }
    }

    /** Grava o texto de êxito da exclusão ({@link #getMensagemExclusao()})
     * como flash — exibido APÓS o redirect, uma única vez. */
    protected void avisarExclusaoConcluida() {
        avisar(getMensagemExclusao());
    }

    /** Texto de êxito da exclusão, específico da página (default: nada). */
    protected String getMensagemExclusao() {
        return null;
    }

    /**
     * Navegação cruzada nas colunas de pessoas das listagens de sessão: o
     * nome do cliente renderiza como link para o painel dele
     * (ClienteSessaoViewPage). Column.setDecorator (Click nativo, sem
     * extras): o HTML devolvido entra cru na célula; ordenação e filtro
     * continuam pela propriedade — o decorator só afeta o desenho.
     */
    protected static Column comLinkCliente(Column coluna) {
        coluna.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                Sessao linha = (Sessao) row;
                Integer clienteId = linha.getCliente().getId();
                String nome = ClickUtils.escapeHtml(linha.getCliente().getNome());
                return "<a href=\"" + context.getRequest().getContextPath()
                        + "/cliente-sessao-view.htm?id=" + clienteId + "\">" + nome + "</a>";
            }
        });
        return coluna;
    }

    /** {@link #comLinkCliente(Column)} para a coluna de profissional. */
    protected static Column comLinkProfissional(Column coluna) {
        coluna.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                Sessao linha = (Sessao) row;
                Integer profissionalId = linha.getProfissional().getId();
                String nome = ClickUtils.escapeHtml(linha.getProfissional().getNome());
                return "<a href=\"" + context.getRequest().getContextPath()
                        + "/profissional-sessao-view.htm?id=" + profissionalId + "\">" + nome + "</a>";
            }
        });
        return coluna;
    }
}
