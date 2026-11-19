package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Procedimento;
import com.gilbertoca.fisio.service.ModalidadeService;
import com.gilbertoca.fisio.service.ProcedimentoService;
import java.util.List;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.util.Bindable;

/**
 * Painel da modalidade: ficha resumida (nome, duração, valor base,
 * capacidade) e, logo abaixo, o catálogo de procedimentos amarrados a ela —
 * TODOS os estados (ATIVO e INATIVO), ordem do catálogo. Chega aqui pela
 * ação Visualizar da listagem de modalidades (ModalidadeViewPage).
 *
 * Ciclo Click canônico: o construtor monta os controles; onInit só prepara;
 * onGet carrega o modelo (Modalidade pelo id @Bindable — abrir a tela não
 * persiste nada); onRender publica o estado de exibição (a ficha e o id
 * levado pelo botão Editar do template). A tabela usa DataProvider — lê a
 * modalidade carregada no onGet no momento do render.
 *
 * Coluna de ações: Editar o procedimento (a edição vive na listagem de
 * procedimentos — o painel só apresenta o catálogo da modalidade).
 */
public class ModalidadeProcedimentoViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    /** Id da modalidade em foco, vindo do Visualizar da listagem. */
    @Bindable protected Integer id;

    private final ModalidadeService modalidadeService = new ModalidadeService();
    private final ProcedimentoService procedimentoService = new ProcedimentoService();

    private final Form formTable = new Form("formTable");
    private final Table table = new Table("table");
    private final PageLink editarLink = new PageLink("editar", "Editar", ProcedimentoEditPage.class);

    /** Modalidade carregada no onGet: base do painel e do provider da tabela. */
    private Modalidade modalidade;

    public ModalidadeProcedimentoViewPage() {
        formTable.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        table.addColumn(new Column("nome", "Nome"));
        table.addColumn(new Column("codigoTuss", "Código RBPF/TUSS"));
        table.addColumn(new Column("situacao", "Situação"));

        // Ações: Editar (o procedimento, na edição do catálogo)
        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions" no Procedimento: ordenar por esta coluna
        // estouraria no PropertyService (propriedade inexistente). Padrão do
        // EditTable canônico do Click: coluna de ações nunca é ordenável.
        actionsCol.setSortable(false);
        AbstractLink[] links = new AbstractLink[] { editarLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        table.setDataProvider(new DataProvider<Procedimento>() {
            @Override
            public List<Procedimento> getData() {
                if (modalidade == null) {
                    return List.of(); // id ausente/inválido: tabela vazia
                }
                return procedimentoService.getByModalidade(modalidade.getId());
            }
        });

        formTable.add(table);

        // Ação como ícone + tooltip nativo (title) — modo "só imagem" do
        // AbstractLink; asset nosso em assets/images.
        editarLink.setImageSrc("/assets/images/editar.svg");
        editarLink.setTitle("Editar");

        addControl(editarLink);
        addControl(formTable);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Modalidades");
    }

    /** Carga do modelo: a Modalidade do painel (ou volta para a listagem). */
    @Override
    public void onGet() {
        super.onGet();
        if (id == null) {
            setRedirect(ModalidadeViewPage.class);
            return;
        }
        modalidade = modalidadeService.get(id);
        if (modalidade == null) {
            // id inexistente na URL: sem painel, sem tabela — de volta à lista
            setRedirect(ModalidadeViewPage.class);
        }
    }

    @Override
    public void onRender() {
        super.onRender();
        if (modalidade != null) {
            addModel("modalidade", modalidade);
        }
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
