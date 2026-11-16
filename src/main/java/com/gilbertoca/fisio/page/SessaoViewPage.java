package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.service.SessaoService;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.apache.click.Context;
import org.apache.click.control.ActionLink;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Decorator;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.util.ClickUtils;
import org.apache.click.util.HtmlStringBuffer;

/**
 * Agenda: TODOS os registros de sessão (atual e futura), ordenados pela
 * última inserção — as recém-agendadas aparecem no topo — com o grafo rico
 * (cliente, profissional, modalidade) carregado via Sql2o. Filtro por nome do
 * profissional na tabela, encaminhado ao service
 * (sessaoService.listarAgenda(filters.get("profissional.nome"))).
 *
 * Coluna de ações via LinkDecorator (click-extras), dirigida pela situação da
 * sessão: Edit somente para a fila e Delete (desfazer agendamento) somente
 * para AGENDADA. Atender NÃO está nesta tela — a recepção organiza o
 * agendamento; o acesso à sala de atendimento fica na lista do profissional
 * (SessaoProntuarioViewPage). Links não aplicáveis à linha são desativados
 * via AbstractLink.setDisabled (renderizam <span class="disabled">) — ver
 * aplicavel().
 */
public class SessaoViewPage extends BorderPage {

    private static final long serialVersionUID = 1L;

    /** Formato da coluna de início nas listagens (a data é LocalDateTime). */
    private static final DateTimeFormatter FORMATO_INICIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SessaoService sessaoService = new SessaoService();
    private Form form = new Form("formTable");
    private Table table = new Table("table");
    private PageLink editLink = new PageLink("edit", "Edit", SessaoEditPage.class);
    /** Desfaz o agendamento (erro de entrada da recepção): só sessão AGENDADA. */
    private ActionLink deleteLink = new ActionLink("delete", "Delete", this, "onDelete");

    public SessaoViewPage() {
        form.setNoLayout(true);
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

        table.addColumn(decorarCliente(new Column("cliente.nome", "Cliente")));

        Column profissionalCol = decorarProfissional(new Column("profissional.nome", "Profissional"));
        profissionalCol.setFilterBy("profissional.nome");
        table.addColumn(profissionalCol);

        table.addColumn(new Column("modalidade.nome", "Modalidade"));
        table.addColumn(new Column("sessaoTipo", "Tipo"));
        table.addColumn(new Column("situacao", "Situação"));

        Column actionsCol = new Column("actions", "Ações");
        AbstractLink[] links = new AbstractLink[] { editLink, deleteLink };
        // Mesma mecânica de SessaoProntuarioViewPage: o LinkDecorator
        // reutiliza as MESMAS instâncias de link em todas as linhas, então o
        // estado por linha é aplicado em renderActionLinks — setDisabled é o
        // mecanismo nativo do AbstractLink (desativado renderiza
        // <span class="disabled">, sem href e sem o onclick do modal de
        // exclusão); ativo segue pelo initLink/renderActionLink do pai.
        actionsCol.setDecorator(new LinkDecorator(table, links, "id") {
            @Override
            public String renderActionLinks(Object row, Context context) {
                Sessao linha = (Sessao) row;
                SessaoSituacao situacao = linha.getSituacao();
                HtmlStringBuffer buffer = new HtmlStringBuffer();
                boolean primeiro = true;
                for (AbstractLink link : linksArray) {
                    if (!primeiro) {
                        buffer.append(getLinkSeparator());
                    }
                    link.setDisabled(!aplicavel(link, situacao));
                    initLink(link, context, linha.getId());
                    renderActionLink(buffer, link, context, row, linha.getId());
                    primeiro = false;
                }
                return buffer.toString();
            }
        });
        table.addColumn(actionsCol);

        // The delete link opens the Bahunya modal. The href Click rendered
        // is preserved on the link and copied to the modal's confirm
        // anchor at click time.
        deleteLink.setAttribute("onclick", "return onDeleteClick(this);");

        table.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                Map<String, String> filters = table.getFilters();
                return sessaoService.listarAgenda(filters.get("profissional.nome"));
            }
        });

        form.add(table);
        addControl(editLink);
        addControl(deleteLink);
        addControl(table);
        addControl(form);
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Agenda do Dia");
    }

    /**
     * Navegação cruzada nas colunas de pessoas: o nome do cliente e o do
     * profissional renderizam como link para o painel correspondente
     * (ClienteSessaoViewPage/ProfissionalSessaoViewPage). Column.setDecorator
     * (Click nativo, sem extras): o HTML devolvido entra cru na célula;
     * ordenação e filtro continuam pela propriedade — o decorator só afeta o
     * desenho.
     */
    private Column decorarCliente(Column coluna) {
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

    private Column decorarProfissional(Column coluna) {
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

    /**
     * Ação disponível na linha, dirigida pela situação da sessão — mesma
     * máquina de estados do agregado (SessaoSituacao) e dos guards do
     * SessaoService. Papel da recepção: mexer no agendamento ANTES do
     * atendimento existir; histórico é imutável.
     * <ul>
     * <li><b>Edit</b>: somente a fila (AGENDADA/CONFIRMADA) — agendamento
     * ainda sem atendimento.</li>
     * <li><b>Delete</b> (desfazer agendamento): somente AGENDADA — mesmo
     * guard de SessaoService.delete; CONFIRMADA não é apagável.</li>
     * </ul>
     * EM_ANDAMENTO e terminais ficam com as duas ações desativadas: a sala
     * governa o atendimento, e o histórico não é editável.
     */
    private boolean aplicavel(AbstractLink link, SessaoSituacao situacao) {
        boolean fila = situacao == SessaoSituacao.AGENDADA
                || situacao == SessaoSituacao.CONFIRMADA;
        if (link == editLink) {
            return fila;
        }
        if (link == deleteLink) {
            return situacao == SessaoSituacao.AGENDADA;
        }
        return false;
    }

    /** Invoked by the ActionLink after the user confirms the modal. */
    public boolean onDelete() {
        Integer id = deleteLink.getValueInteger();
        if (id == null) {
            return false;
        }
        try {
            sessaoService.delete(id);
            setRedirect(SessaoViewPage.class);
        } catch (IllegalStateException ex) {
            // Histórico (EM_ANDAMENTO/REALIZADA/FALTOU/CANCELADA) não é
            // apagável: avisa a recepção em vez de estourar a error page.
            addModel("mensagem", ex.getMessage());
        }
        return false;
    }
}
