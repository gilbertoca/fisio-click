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
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.service.ModalidadeService;

/**
 * Listagem de modalidades. Filtro por nome na tabela, encaminhado ao service
 * (getAllLikeNome(filters.get("nome")) pattern). Ação Visualizar abre o
 * painel da modalidade (ficha + catálogo de procedimentos amarrados).
 */
public class ModalidadeViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    private final ModalidadeService modalidadeService = new ModalidadeService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink viewLink = new PageLink("ver", "Visualizar", ModalidadeProcedimentoViewPage.class);
    private PageLink editLink = new PageLink("edit", "Editar", ModalidadeEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Excluir", this, "onDelete");

    public ModalidadeViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome", "Nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        table.addColumn(new Column("duracaoMinutos", "Duração (min)"));
        table.addColumn(new Column("valorBase", "Valor Base (R$)"));
        table.addColumn(new Column("capacidadeSimultanea", "Capacidade"));

        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions" no Modalidade: ordenar por esta coluna
        // estouraria no PropertyService (propriedade inexistente). Padrão do
        // EditTable canônico do Click: coluna de ações nunca é ordenável.
        actionsCol.setSortable(false);
        AbstractLink[] links = new AbstractLink[] { viewLink, editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        // Ações como ícone + tooltip nativo (title) — modo "só imagem" do
        // AbstractLink; asset nosso em assets/images.
        viewLink.setImageSrc("/assets/images/visualizar.svg");
        viewLink.setTitle("Visualizar");
        editLink.setImageSrc("/assets/images/editar.svg");
        editLink.setTitle("Editar");
        deleteLink.setImageSrc("/assets/images/excluir.svg");
        deleteLink.setTitle("Excluir");

        table.setDataProvider(new DataProvider<Modalidade>() {
            @Override
            public List<Modalidade> getData() {
                Map<String, String> filters = table.getFilters();
                return modalidadeService.getAllLikeNome(filters.get("nome"));
            }
        });

        form.add(table);
        addControl(viewLink);
        addControl(editLink);
        addControl(deleteLink);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Modalidades View");

        // Sem modalidade:excluir: a ação sai da coluna (o guard do onDelete
        // continua no servidor — a UI é só a porta de entrada).
        if (!temPermissao("modalidade:excluir")) {
            removeControl(deleteLink);
            Column colAcoes = table.getColumn("actions");
            colAcoes.setDecorator(new LinkDecorator(table, new AbstractLink[] { viewLink, editLink }, "id"));
        }
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        // Enforca-se no servidor: ocultar o link não protege contra URL
        // forjada (actionLink=delete&value=...).
        if (!temPermissao("modalidade:excluir")) {
            avisar("Você não tem permissão para excluir registros.");
            setRedirect(ModalidadeViewPage.class);
            return false;
        }
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            modalidadeService.delete(id);
            // PRG + flash de sessão: a mensagem de êxito sobrevive ao
            // redirect e é exibida uma única vez (o F5 não repete nem a
            // exclusão nem a mensagem).
            avisarExclusaoConcluida();
            setRedirect(ModalidadeViewPage.class);
        } catch (IllegalStateException ex) {
            // Modalidade com procedimentos/sessões vinculadas: avisa via
            // flash e volta à listagem (PRG), em vez de estourar a error page.
            avisar(ex.getMessage());
            setRedirect(ModalidadeViewPage.class);
        }
        return false;
    }

    @Override
    protected String getMensagemExclusao() {
        return "Modalidade excluída com êxito.";
    }

    /**
     * Permissões exigidas (AuthenticatedPage, any-of) — Fase 3: o gate de
     * acesso à página segue a matriz de permissões do módulo de segurança.
     */
    @Override
    protected String[] permissoesExigidas() {
        return new String[] { "modalidade:visualizar" };
    }
}
