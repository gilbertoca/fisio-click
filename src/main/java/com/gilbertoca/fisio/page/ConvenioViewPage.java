package com.gilbertoca.fisio.page;

import org.apache.click.control.AbstractLink;
import org.apache.click.control.ActionLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

import java.util.List;
import java.util.Map;
import com.gilbertoca.fisio.model.Convenio;
import com.gilbertoca.fisio.service.ConvenioService;

/**
 * Convênio view page. Filtro por nome na tabela, encaminhado ao service
 * (clienteService.getAllLikeNome(filters.get("nome")) pattern).
 */
public class ConvenioViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    private final ConvenioService convenioService = new ConvenioService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Editar", ConvenioEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Excluir", this, "onDelete");

    public ConvenioViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome", "Nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        table.addColumn(new Column("cnpj", "CNPJ"));

        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions": ordenar por esta coluna estouraria no
        // PropertyService. Padrão do EditTable canônico: ações nunca ordenável.
        actionsCol.setSortable(false);
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        // Ações como ícone + tooltip nativo (title) — modo "só imagem" do
        // AbstractLink; asset nosso em assets/images.
        editLink.setImageSrc("/assets/images/editar.svg");
        editLink.setTitle("Editar");
        deleteLink.setImageSrc("/assets/images/excluir.svg");
        deleteLink.setTitle("Excluir");

        table.setDataProvider(new DataProvider<Convenio>() {
            @Override
            public List<Convenio> getData() {
                Map<String, String> filters = table.getFilters();
                return convenioService.getAllLikeNome(filters.get("nome"));
            }
        });

        form.add(table);
        addControl(editLink);
        addControl(deleteLink);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Convênios View");

        // Sem convenio:excluir: a ação sai da coluna (o guard do onDelete
        // continua no servidor — a UI é só a porta de entrada).
        if (!temPermissao("convenio:excluir")) {
            removeControl(deleteLink);
            Column colAcoes = table.getColumn("actions");
            colAcoes.setDecorator(new LinkDecorator(table, new AbstractLink[] { editLink }, "id"));
        }
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        // Enforca-se no servidor: ocultar o link não protege contra URL
        // forjada (actionLink=delete&value=...).
        if (!temPermissao("convenio:excluir")) {
            avisar("Você não tem permissão para excluir registros.");
            setRedirect(ConvenioViewPage.class);
            return false;
        }
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            convenioService.delete(id);
            // PRG + flash de sessão: a mensagem de êxito sobrevive ao
            // redirect e é exibida uma única vez (o F5 não repete nem a
            // exclusão nem a mensagem).
            avisarExclusaoConcluida();
            setRedirect(ConvenioViewPage.class);
        } catch (IllegalStateException ex) {
            // Convênio com clientes/faturamentos vinculados: avisa via flash
            // e volta à listagem (PRG), em vez de estourar a error page.
            avisar(ex.getMessage());
            setRedirect(ConvenioViewPage.class);
        }
        return false;
    }

    @Override
    protected String getMensagemExclusao() {
        return "Convênio excluído com êxito.";
    }

    /**
     * Permissões exigidas (AuthenticatedPage, any-of) — Fase 3: o gate de
     * acesso à página segue a matriz de permissões do módulo de segurança.
     */
    @Override
    protected String[] permissoesExigidas() {
        return new String[] { "convenio:visualizar" };
    }
}
