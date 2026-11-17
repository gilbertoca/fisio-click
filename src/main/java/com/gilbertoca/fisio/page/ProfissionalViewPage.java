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
import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.service.ProfissionalService;

/**
 * Profissional view page. Filtro por nome na tabela, encaminhado ao service
 * (clienteService.getAllLikeNome(filters.get("nome")) pattern).
 */
public class ProfissionalViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    private final ProfissionalService profissionalService = new ProfissionalService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Editar", ProfissionalEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Excluir", this, "onDelete");
    /** Painel do profissional: ficha + sessões dele (agendadas/atendidas). */
    private PageLink viewLink = new PageLink("view", "Visualizar", ProfissionalSessaoViewPage.class);

    public ProfissionalViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome", "Nome Completo");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        Column registroCol = new Column("registroNumero", "Registro");
        registroCol.setSortable(true);
        table.addColumn(registroCol);

        table.addColumn(new Column("telefone", "Telefone"));

        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions": ordenar por esta coluna estouraria no
        // PropertyService. Padrão do EditTable canônico: ações nunca ordenável.
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

        table.setDataProvider(new DataProvider<Profissional>() {
            @Override
            public List<Profissional> getData() {
                Map<String, String> filters = table.getFilters();
                return profissionalService.getAllLikeNome(filters.get("nome"));
            }
        });

        form.add(table);       
        addControl(editLink);
        addControl(deleteLink);
        addControl(viewLink);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Profissionais View");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            profissionalService.delete(id);
            // PRG + flash de sessão: a mensagem de êxito sobrevive ao
            // redirect e é exibida uma única vez (o F5 não repete nem a
            // exclusão nem a mensagem).
            avisarExclusaoConcluida();
            setRedirect(ProfissionalViewPage.class);
        } catch (IllegalStateException ex) {
            // Profissional com sessões vinculadas: o histórico da agenda não
            // é apagável — avisa via flash e volta à listagem (PRG), em vez
            // de estourar a error page.
            avisar(ex.getMessage());
            setRedirect(ProfissionalViewPage.class);
        }
        return false;
    }

    @Override
    protected String getMensagemExclusao() {
        return "Profissional excluído com êxito.";
    }
}
