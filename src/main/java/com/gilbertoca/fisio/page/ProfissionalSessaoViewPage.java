package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.service.ProfissionalService;
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
 * Painel do profissional: ficha resumida (nome, cpf, contatos, registro) e,
 * logo abaixo, TODAS as sessões dele — agendadas e atendidas (todos os
 * estados do ciclo), mais recentes primeiro. Chega aqui pela ação Visualizar
 * da listagem de profissionais (ProfissionalViewPage).
 *
 * Ciclo Click canônico (espelho de ClienteSessaoViewPage): o construtor
 * monta os controles; onInit só prepara; onGet carrega o modelo
 * (Profissional pelo id @Bindable — abrir a tela não persiste nada);
 * onRender publica o estado de exibição (a ficha e o id levado pelo botão
 * Editar do template). A tabela de sessões usa DataProvider — lê o
 * profissional carregado no onGet no momento do render.
 *
 * Coluna de ações: um único link Visualizar por linha, SEM lógica por
 * situação — o destino (SessaoProntuarioEditPage, a sala de atendimento) já
 * se adapta ao estado da sessão: painel de conferência (AGENDADA), registro
 * em edição (EM_ANDAMENTO) ou histórico readonly (terminais).
 */
public class ProfissionalSessaoViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    /** Formato da coluna de início nas listagens (a data é LocalDateTime). */
    private static final DateTimeFormatter FORMATO_INICIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Id do profissional em foco, vindo do Visualizar da listagem. */
    @Bindable protected Integer id;

    private final ProfissionalService profissionalService = new ProfissionalService();
    private final SessaoService sessaoService = new SessaoService();

    private final Form formTable = new Form("formTable");
    private final Table table = new Table("table");
    private final PageLink verLink = new PageLink("ver", "Visualizar", SessaoProntuarioEditPage.class);

    /** Profissional carregado no onGet: base do painel e do provider. */
    private Profissional profissional;

    public ProfissionalSessaoViewPage() {
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

        table.addColumn(new Column("cliente.nome", "Cliente"));
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
                if (profissional == null) {
                    return List.of(); // id ausente/inválido: tabela vazia
                }
                return sessaoService.listarPorProfissional(profissional.getId());
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
        addModel("title", "Profissionais");
    }

    /** Carga do modelo: o Profissional do painel (ou volta para a listagem). */
    @Override
    public void onGet() {
        super.onGet();
        if (id == null) {
            setRedirect(ProfissionalViewPage.class);
            return;
        }
        profissional = profissionalService.get(id);
        if (profissional == null) {
            // id inexistente na URL: sem painel, sem tabela — de volta à lista
            setRedirect(ProfissionalViewPage.class);
        }
    }

    @Override
    public void onRender() {
        super.onRender();
        if (profissional != null) {
            addModel("profissional", profissional);
        }
    }
}
