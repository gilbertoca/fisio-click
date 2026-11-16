package com.gilbertoca.fisio.page;

import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.Table;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

import java.util.List;
import org.apache.click.control.ActionLink;
import org.apache.click.control.PageLink;
import org.apache.click.control.TablePaginator;
import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.service.ClienteService;
import java.util.Map;

/**
 * Cliente view page.
 */
public class ClienteViewPage extends BorderPage {

    private final ClienteService clienteService = new ClienteService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Editar", ClienteEditPage.class);
    private ActionLink deleteLink = new ActionLink("delete", "Excluir", this, "onDelete");
    /** Painel do cliente: ficha + sessões dele (agendadas/atendidas). */
    private PageLink viewLink = new PageLink("view", "Visualizar", ClienteSessaoViewPage.class);
    public ClienteViewPage() {
        form.setNoLayout(true);
        // Bahunya's native table classes. Do NOT use Table.CLASS_ITS, which
        // is Click's own non-Bahunya class nome.
        //table.setClass("table");
        //table.setPageSize(5);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome", "Nome Completo");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        Column emailCol = new Column("email", "Email");
        emailCol.setSortable(true);
        table.addColumn(emailCol);

        table.addColumn(new Column("telefone", "Telefone"));

        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions": ordenar por esta coluna estouraria no
        // PropertyService. Padrão do EditTable canônico: ações nunca ordenável.
        actionsCol.setSortable(false);
        AbstractLink[] links = new AbstractLink[] { viewLink, editLink, deleteLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        // The delete link opens the Bahunya modal. The href Click rendered
        // is preserved on the link and copied to the modal's confirm
        // anchor at click time.
        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        // Ações como ícone + tooltip nativo (title) — modo "só imagem" do
        // AbstractLink; asset nosso em assets/images.
        viewLink.setImageSrc("/assets/images/visualizar.svg");
        viewLink.setTitle("Visualizar");
        editLink.setImageSrc("/assets/images/editar.svg");
        editLink.setTitle("Editar");
        deleteLink.setImageSrc("/assets/images/excluir.svg");
        deleteLink.setTitle("Excluir");

        table.setDataProvider(new DataProvider<Cliente>() {
            @Override
            public List<Cliente> getData() {
                Map<String, String> filters = table.getFilters(); // Returns {"nome" -> "John"}
                List<Cliente> all = clienteService.getAllLikeNome(filters.get("nome"));
                return all;
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
        addModel("title", "Clientes View");
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            clienteService.delete(id);
            // PRG + flash de sessão: a mensagem de êxito sobrevive ao
            // redirect e é exibida uma única vez (o F5 não repete nem a
            // exclusão nem a mensagem).
            avisarExclusaoConcluida();
            setRedirect(ClienteViewPage.class);
        } catch (IllegalStateException ex) {
            // Cliente com sessões/faturamentos vinculados: o histórico não
            // é apagável — avisa via flash e volta à listagem (PRG), em vez
            // de estourar a error page.
            avisar(ex.getMessage());
            setRedirect(ClienteViewPage.class);
        }
        return false;
    }

    @Override
    protected String getMensagemExclusao() {
        return "Cliente excluído com êxito.";
    }
}
