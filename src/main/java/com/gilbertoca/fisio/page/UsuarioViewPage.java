package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Usuario;
import com.gilbertoca.fisio.service.UsuarioService;
import java.util.List;
import java.util.Map;
import org.apache.click.Context;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;

/**
 * Listagem de usuários, no formato CredBem: Nome, Usuário, Papel, Ativo
 * (SIM/NÃO) e ações — só o lápis de Editar. A exclusão de usuário NÃO
 * existe: o acesso se gerencia pelo Ativo (SIM/NÃO) na edição, o que
 * preserva o histórico do cadastro. No cabeçalho, os botões Conectados
 * (tela de sessões abertas) e Adicionar.
 */
public class UsuarioViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    private final UsuarioService usuarioService = new UsuarioService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Editar", UsuarioEditPage.class);

    public UsuarioViewPage() {
        form.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column nomeCol = new Column("nome", "Nome");
        nomeCol.setSortable(true);
        nomeCol.setFilterBy("nome");
        table.addColumn(nomeCol);

        Column usernameCol = new Column("username", "Usuário");
        usernameCol.setSortable(true);
        usernameCol.setFilterBy("username");
        table.addColumn(usernameCol);

        Column papelCol = new Column("nomesPapeis", "Papel");
        // Agregado do LISTAGG: não ordena (não é coluna da tabela).
        papelCol.setSortable(false);
        table.addColumn(papelCol);

        Column ativoCol = new Column("ativo", "Ativo");
        ativoCol.setSortable(false);
        // Boolean cru ("true"/"false") não é língua de gente: SIM/NÃO, PT.
        ativoCol.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                return ((Usuario) row).isAtivo() ? "SIM" : "NÃO";
            }
        });
        table.addColumn(ativoCol);

        Column actionsCol = new Column("actions", "Ações");
        // Padrão do EditTable canônico: coluna de ações nunca é ordenável.
        actionsCol.setSortable(false);
        actionsCol.setDecorator(new LinkDecorator(table, new AbstractLink[] { editLink }, "id"));
        table.addColumn(actionsCol);

        // Ação como ícone + tooltip nativo (title) — padrão das listagens.
        editLink.setImageSrc("/assets/images/editar.svg");
        editLink.setTitle("Editar");

        // Os filtros dos cabeçalhos (nome/username) alimentam o getAllLike.
        table.setDataProvider(new DataProvider<Usuario>() {
            @Override
            public List<Usuario> getData() {
                Map<String, String> filters = table.getFilters();
                return usuarioService.getAllLike(
                        filters.get("nome"), filters.get("username"));
            }
        });

        form.add(table);
        addControl(editLink);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Listagem de Usuários");
    }

    @Override
    protected String[] permissoesExigidas() {
        return new String[] { "usuario:visualizar" };
    }
}
