package com.gilbertoca.fisio.page;

import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.service.SessaoService;
import java.util.List;
import java.util.Map;
import org.apache.click.Context;
import org.apache.click.control.AbstractLink;
import org.apache.click.control.Column;
import org.apache.click.control.Form;
import org.apache.click.control.PageLink;
import org.apache.click.control.Table;
import org.apache.click.control.TablePaginator;
import org.apache.click.dataprovider.DataProvider;
import org.apache.click.extras.control.LinkDecorator;
import org.apache.click.util.Bindable;
import org.apache.click.util.HtmlStringBuffer;

/**
 * Lista de atendimentos (agenda atual e futura: TODOS os registros de sessão,
 * ordenados pela última inserção — as recém-agendadas no topo). Cada linha é
 * o atendimento de UM paciente; sessões em grupo (ex.: pilates) aparecem como
 * linhas consecutivas no mesmo slot, uma por paciente, cada uma com seu
 * prontuário individual.
 *
 * Prototype: o "profissional logado" é um atributo de sessão
 * (SESSION_PROFISSIONAL_ID). Quando a autorização por papel
 * (admin/gerencia/profissional) for modelada, este atributo passa a ser
 * derivado do papel no login. Filtro por nome do profissional na tabela,
 * encaminhado ao service. O filtro do cliente é feito na sala, na página de
 * atendimento (SessaoProntuarioEditPage).
 *
 * Coluna de ações via LinkDecorator (click-extras), dirigida pela situação da
 * sessão: Atender para fila + EM_ANDAMENTO, Editar somente para a fila,
 * Visualizar para os terminais. Links não aplicáveis à linha são desativados
 * via AbstractLink.setDisabled (renderizam <span class="disabled">) — ver
 * aplicavel(). Ação de deletar NÃO existe nesta lista do profissional:
 * sessões em atendimento ou realizadas têm registro clínico associado; a
 * recepção lida com cancelamento/estorno na agenda (SessaoViewPage).
 */
public class SessaoProntuarioViewPage extends AuthenticatedPage {

    private static final long serialVersionUID = 1L;

    /** Gancho futuro da autorização por papel. Por enquanto é um atributo de sessão livre. */
    public static final String SESSION_PROFISSIONAL_ID = "profissionalLogadoId";

    @Bindable protected Integer id;

    private final Form formTable = new Form("formTable");
    private final Table table = new Table("table");
    private final PageLink atenderLink = new PageLink("atender", "Atender", SessaoProntuarioEditPage.class);
    private final PageLink editLink = new PageLink("edit", "Editar", SessaoEditPage.class);
    private final PageLink visualizarLink = new PageLink("visualizar", "Visualizar", SessaoProntuarioEditPage.class);

    private final SessaoService sessaoService = new SessaoService();

    public SessaoProntuarioViewPage() {
        formTable.setNoLayout(true);
        table.setShowBanner(true);
        table.setSortable(true);
        table.setPaginator(new TablePaginator(table));

        Column inicioCol = new Column("dataHoraInicio", "Data Hora Início");
        // dataHoraInicio é LocalDateTime (java.time): suportado pelo
        // Column.setFormat desde o CLK-64 (render via DateTimeFormatter,
        // extraindo o padrão do estilo "{0,date,...}") — disponível no
        // artefato 2.8.1-SNAPSHOT consumido agora. Comportamento default
        // do Click no lugar do Decorator manual.
        inicioCol.setFormat("{0,date,dd/MM/yyyy HH:mm}");
        inicioCol.setSortable(true);
        table.addColumn(inicioCol);

        Column profissionalCol = comLinkProfissional(new Column("profissional.nome", "Profissional"));
        profissionalCol.setFilterBy("profissional.nome");
        table.addColumn(profissionalCol);

        table.addColumn(comLinkCliente(new Column("cliente.nome", "Cliente")));
        table.addColumn(new Column("modalidade.nome", "Modalidade"));
        table.addColumn(new Column("sessaoTipo", "Tipo"));
        table.addColumn(new Column("situacao", "Situação"));

        Column actionsCol = new Column("actions", "Ações");
        // Sem propriedade "actions" no Sessao: ordenar por esta coluna estouraria
        // no PropertyService (propriedade inexistente). Padrão do EditTable
        // canônico do Click: coluna de ações nunca é ordenável.
        actionsCol.setSortable(false);
        AbstractLink[] links = new AbstractLink[] { atenderLink, editLink, visualizarLink };
        // O LinkDecorator reutiliza as MESMAS instâncias de link em todas as
        // linhas, então o estado por linha é aplicado aqui: setDisabled é o
        // mecanismo nativo do AbstractLink — desativado renderiza
        // <span class="disabled"> (sem href, sem onclick); ativo segue pelo
        // initLink/renderActionLink do pai.
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

        table.setDataProvider(new DataProvider<Sessao>() {
            @Override
            public List<Sessao> getData() {
                Map<String, String> filters = table.getFilters();
                return sessaoService.listarAgenda(filters.get("profissional.nome"));
            }
        });

        formTable.add(table);

        // Ações como ícone (Bahunya não tem conjunto de ícones; assets são
        // nossos): AbstractLink em modo "só imagem" — <img> + tooltip nativo
        // pelo title. Desativado continua <span class="disabled"> com o
        // ícone atenuado (opacidade via app.css).
        atenderLink.setImageSrc("/assets/images/atender.svg");
        atenderLink.setTitle("Atender");
        editLink.setImageSrc("/assets/images/editar.svg");
        editLink.setTitle("Editar");
        visualizarLink.setImageSrc("/assets/images/visualizar.svg");
        visualizarLink.setTitle("Visualizar");

        addControl(atenderLink);
        addControl(editLink);
        addControl(visualizarLink);
        addControl(formTable);
    }

    /**
     * Ação disponível na linha, dirigida pela situação da sessão — mesma
     * máquina de estados do agregado (SessaoSituacao) e dos guards da sala
     * (SessaoProntuarioEditPage/SessaoService):
     * <ul>
     * <li><b>Atender</b>: fila (AGENDADA/CONFIRMADA) e EM_ANDAMENTO — a sala
     * exibe o painel de conferência ou o registro em edição.</li>
     * <li><b>Editar</b>: somente a fila — a recepção mexe no agendamento antes
     * do atendimento iniciar; nada editar em EM_ANDAMENTO ou terminais.</li>
     * <li><b>Visualizar</b>: terminais (REALIZADA/FALTOU/CANCELADA) — a
     * própria sala em modo histórico readonly.</li>
     * </ul>
     */
    private boolean aplicavel(AbstractLink link, SessaoSituacao situacao) {
        boolean fila = situacao == SessaoSituacao.AGENDADA
                || situacao == SessaoSituacao.CONFIRMADA;
        boolean terminal = situacao == SessaoSituacao.REALIZADA
                || situacao == SessaoSituacao.FALTOU
                || situacao == SessaoSituacao.CANCELADA;
        if (link == atenderLink) {
            return fila || situacao == SessaoSituacao.EM_ANDAMENTO;
        }
        if (link == editLink) {
            return fila;
        }
        if (link == visualizarLink) {
            return terminal;
        }
        return false;
    }

    @Override
    public void onInit() {
        super.onInit();
        addModel("title", "Atendimentos");
    }
}
