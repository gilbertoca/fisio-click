package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.service.ClienteService;
import com.gilbertoca.fisio.service.SessaoService;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
import org.apache.click.util.Bindable;

/**
 * Painel do cliente: ficha resumida (nome, cpf, contatos, situação) e, logo
 * abaixo, TODAS as sessões dele — agendadas e atendidas (todos os estados do
 * ciclo), mais recentes primeiro. Chega aqui pela ação Visualizar da
 * listagem de clientes (ClienteViewPage).
 *
 * Ciclo Click canônico: o construtor monta os controles; onInit só prepara;
 * onGet carrega o modelo (Cliente pelo id @Bindable — abrir a tela não
 * persiste nada); onRender publica o estado de exibição (a ficha e o id
 * levado pelo botão Editar do template). A tabela de sessões usa
 * DataProvider — lê o cliente carregado no onGet no momento do render.
 *
 * Coluna de ações: um único link Visualizar por linha, SEM lógica por
 * situação — o destino (SessaoProntuarioEditPage, a sala de atendimento) já
 * se adapta ao estado da sessão: painel de conferência (AGENDADA), registro
 * em edição (EM_ANDAMENTO) ou histórico readonly (terminais).
 */
public class ClienteSessaoViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    /** Formato da coluna de início nas listagens (a data é LocalDateTime). */
    private static final DateTimeFormatter FORMATO_INICIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Id do cliente em foco, vindo do Visualizar da listagem. */
    @Bindable protected Integer id;

    private final ClienteService clienteService = new ClienteService();
    private final SessaoService sessaoService = new SessaoService();

    private final Form formTable = new Form("formTable");
    private final Table table = new Table("table");
    private final PageLink verLink = new PageLink("ver", "Visualizar", SessaoProntuarioEditPage.class);

    /** Cliente carregado no onGet: base do painel e do provider da tabela. */
    private Cliente cliente;

    public ClienteSessaoViewPage() {
        formTable.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column inicioCol = new Column("dataHoraInicio", "Data Hora Início");
        // dataHoraInicio é LocalDateTime (java.time): o setFormat do Column usa
        // MessageFormat, que só formata java.util.Date ("Cannot format given
        // Object as a Date"). O decorator formata com DateTimeFormatter —
        // a ordenação continua pelo valor cru da coluna.
        inicioCol.setDecorator(new Decorator() {
            @Override
            public String render(Object row, Context context) {
                return ((Sessao) row).getDataHoraInicio().format(FORMATO_INICIO);
            }
        });
        inicioCol.setSortable(true);
        table.addColumn(inicioCol);

        table.addColumn(new Column("profissional.nome", "Profissional"));
        table.addColumn(new Column("modalidade.nome", "Modalidade"));
        table.addColumn(new Column("sessaoTipo", "Tipo"));
        table.addColumn(new Column("situacao", "Situação"));

        // Ações: Visualizar (a sala, que se adapta ao estado da sessão)
        Column actionsCol = new Column("actions", "Ações");
        AbstractLink[] links = new AbstractLink[] { verLink };
        actionsCol.setDecorator(new LinkDecorator(table, links, "id"));
        table.addColumn(actionsCol);

        table.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                if (cliente == null) {
                    return List.of(); // id ausente/inválido: tabela vazia
                }
                return sessaoService.listarPorCliente(cliente.getId());
            }
        });

        formTable.add(table);
        addControl(verLink);
        addControl(table);
        addControl(formTable);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Clientes");
    }

    /** Carga do modelo: o Cliente do painel (ou volta para a listagem). */
    @Override
    public void onGet() {
        super.onGet();
        if (id == null) {
            setRedirect(ClienteViewPage.class);
            return;
        }
        cliente = clienteService.get(id);
        if (cliente == null) {
            // id inexistente na URL: sem painel, sem tabela — de volta à lista
            setRedirect(ClienteViewPage.class);
        }
    }

    @Override
    public void onRender() {
        super.onRender();
        if (cliente != null) {
            addModel("cliente", cliente);
        }
    }
}
