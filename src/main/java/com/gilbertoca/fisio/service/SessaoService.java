package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Modalidade;
import com.gilbertoca.fisio.model.Profissional;
import com.gilbertoca.fisio.model.Sessao;
import com.gilbertoca.fisio.model.SessaoProcedimento;
import com.gilbertoca.fisio.model.enums.SessaoSituacao;
import com.gilbertoca.fisio.model.enums.SessaoTipo;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import org.sql2o.Connection;

/**
 * Dono do agregado Sessao (padrão ContratoService do GACE): CRUD de
 * agendamento, consultas de agenda e as TRANSIÇÕES DE SITUAÇÃO do ciclo
 * operacional — a gravação do registro inicia o atendimento
 * (salvarRegistro: AGENDADA/CONFIRMADA → EM_ANDAMENTO, marcando
 * registro_dt_inicio) e finalizarAtendimento define REALIZADA — como
 * ContratoService.consignar define a situação do contrato. Estado é efeito
 * de gravação real: abrir a tela de edição é navegação, não persiste nada.
 * O registro do atendimento é coluna da própria sessão (1:1): os textos
 * clínicos (registro_relato/avaliacao/conduta/orientacoes) e os horários
 * reais (registro_dt_inicio/fim), distintos dos dt_hora_* da agenda. O
 * compartilhamento de ambiente em modalidades de grupo (N sessões
 * individuais no mesmo slot) é governado aqui pela capacidade_simultanea.
 * O roteiro de condutas (sessao_procedimento) é coleção DEPENDENTE do
 * agregado: o get a carrega junto e a gravação a sincroniza na mesma
 * transação do registro.
 */
public class SessaoService {

    public SessaoService() {}

    
    // ============================================================================
    // RICH RELATIONSHIP LOOKUP HELPERS FOR MULTI-ENTITY VIEW BINDING
    // ============================================================================
    public Cliente getClienteById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAllClientes() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public Profissional getProfissionalById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.get")).addParameter("id", id).executeAndFetchFirst(Profissional.class);
        }
    }

    public List<Profissional> getAllProfissionais() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("profissional.getAll")).executeAndFetch(Profissional.class);
        }
    }

    public Modalidade getModalidadeById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.get")).addParameter("id", id).executeAndFetchFirst(Modalidade.class);
        }
    }

    public List<Modalidade> getAllModalidades() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.getAll")).executeAndFetch(Modalidade.class);
        }
    }

    // ============================================================================
    // CORE APPOINTMENT TRANSACTION CRUD & STATE GUARD VALS
    // ============================================================================
    /**
     * Sessão completa: a linha da agenda + registro do atendimento + roteiro
     * de condutas. O roteiro é coleção dependente do agregado, então vem
     * junto — quem carrega a Sessao pela sala já tem o estado do picklist sem
     * uma segunda consulta orquestrada pela página.
     */
    public Sessao get(Integer id) {
        if (id == null) {
            return null;
        }
        Sessao sessao;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            sessao = conn.createQuery(QueryLoader.get("sessao.get")).addParameter("id", id).executeAndFetchFirst(Sessao.class);
        }
        if (sessao != null) {
            sessao.setProcedimentos(carregarRoteiro(id));
        }
        return sessao;
    }

    /**
     * Agenda: TODOS os registros de sessão (atual e futura), ordenados pela
     * última inserção — as recém-agendadas aparecem no topo. Atalho para a
     * listagem filtrada apenas pelo nome do profissional.
     */
    public List<Sessao> listarAgenda(String profissionalNome) {
        return listarAgendaFiltrada(
                textoOuNulo(profissionalNome) == null
                        ? Map.of()
                        : Map.of("profissional.nome", profissionalNome));
    }

    /**
     * Agenda com os filtros de coluna da Table (Column.setFilterBy): data/hora,
     * cliente, profissional, modalidade, tipo e situação. Texto em branco é
     * ignorado. Tipo e situação aceitam o rótulo exibido (com ou sem acento),
     * o nome persistido ou um trecho dele.
     */
    public List<Sessao> listarAgendaFiltrada(Map<String, String> filtros) {
        Map<String, String> f = filtros == null ? Map.of() : filtros;
        FiltroEnum tipo = filtroEnum(SessaoTipo.class, f.get("tipo"));
        FiltroEnum situacao = filtroEnum(SessaoSituacao.class, f.get("situacao"));
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getAll"))
                    .addParameter("inicio", textoOuNulo(f.get("inicio")))
                    .addParameter("clienteNome", textoOuNulo(f.get("cliente.nome")))
                    .addParameter("profissionalNome", textoOuNulo(f.get("profissional.nome")))
                    .addParameter("modalidadeNome", textoOuNulo(f.get("modalidade.nome")))
                    .addParameter("tipoExato", tipo.exato)
                    .addParameter("tipoLike", tipo.like)
                    .addParameter("situacaoExato", situacao.exato)
                    .addParameter("situacaoLike", situacao.like)
                    .executeAndFetch(Sessao.class);
        }
    }

    /** Texto de filtro: em branco vira null (condição desligada na query). */
    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /**
     * Traduz o texto digitado no filtro para o valor persistido do enum.
     * Rótulo ou nome completo vira igualdade — evita que "Avaliação" case
     * também com "Reavaliação"; texto parcial vira LIKE sobre o name().
     */
    private static <E extends Enum<E>> FiltroEnum filtroEnum(Class<E> tipo, String texto) {
        String alvo = normalizar(texto);
        if (alvo == null) {
            return new FiltroEnum(null, null);
        }
        for (E constante : tipo.getEnumConstants()) {
            if (alvo.equals(normalizar(constante.name()))
                    || alvo.equals(normalizar(constante.toString()))) {
                return new FiltroEnum(constante.name(), null);
            }
        }
        return new FiltroEnum(null, alvo.replace(' ', '_'));
    }

    /** Minúsculas, sem acentos e com espaços colapsados. */
    private static String normalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    /** Par de filtros do enum: igualdade (rótulo reconhecido) ou trecho. */
    private static final class FiltroEnum {
        final String exato;
        final String like;

        FiltroEnum(String exato, String like) {
            this.exato = exato;
            this.like = like;
        }
    }

    /**
     * Painel do cliente: TODAS as sessões dele (agendadas e atendidas —
     * todos os estados do ciclo), mais recentes primeiro. clienteId null
     * retorna lista vazia (página sem id navega de volta à listagem).
     */
    public List<Sessao> listarPorCliente(Integer clienteId) {
        if (clienteId == null) {
            return List.of();
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getAllPorCliente"))
                    .addParameter("clienteId", clienteId)
                    .executeAndFetch(Sessao.class);
        }
    }

    /**
     * Painel do profissional: TODAS as sessões dele (agendadas e atendidas —
     * todos os estados do ciclo), mais recentes primeiro. profissionalId null
     * retorna lista vazia (página sem id navega de volta à listagem).
     */
    public List<Sessao> listarPorProfissional(Integer profissionalId) {
        if (profissionalId == null) {
            return List.of();
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getAllPorProfissional"))
                    .addParameter("profissionalId", profissionalId)
                    .executeAndFetch(Sessao.class);
        }
    }

    /** Cockpit da home (quadro agendamento): a fila da recepção — sessões
     * ainda não atendidas, as recém-criadas primeiro. */
    public List<Sessao> getUltimasNaFila(int limite) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getUltimasNaFila"))
                    .addParameter("limite", limite)
                    .executeAndFetch(Sessao.class);
        }
    }

    /** Cockpit da home (quadro atendimento): os últimos atendimentos com
     * registro iniciado — EM_ANDAMENTO em curso e REALIZADAS recentes. */
    public List<Sessao> getUltimosAtendimentos(int limite) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao.getUltimosAtendimentos"))
                    .addParameter("limite", limite)
                    .executeAndFetch(Sessao.class);
        }
    }

    /**
     * Agenda uma nova sessão. A verificação de conflito de horário e o INSERT
     * executam na mesma transação: se duas requisições concorrentes passarem
     * pela checagem, o commit do perdedor falha no estado do vencedor ou o
     * rollback do primeiro evita a sobreposição.
     */
    public void create(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            // 1. Guard validations reusing our clean internal lookup methods
            if (sessao.getCliente() == null || getClienteById(sessao.getCliente().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: O cliente informado não existe.");
            }
            if (sessao.getProfissional() == null || getProfissionalById(sessao.getProfissional().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: O Profissional informado não existe.");
            }
            if (sessao.getModalidade() == null || getModalidadeById(sessao.getModalidade().getId()) == null) {
                throw new IllegalArgumentException("Operação cancelada: A Modalidade informado não existe.");
            }
            // Concurrency boundary validation: conflito entre modalidades e
            // capacidade de grupo da modalidade (ex.: pilates 4 pacientes)
            validarConflitoAgenda(conn, sessao, null);

            conn.createQuery(QueryLoader.get("sessao.create")).bind(sessao).executeUpdate();
            conn.commit();
        }
    }

    /**
     * Atualiza a sessão reaproveitando a mesma barreira de concorrência do
     * create: checagem de conflito (excluindo a própria sessão) + UPDATE na
     * mesma transação.
     */
    public void update(Sessao sessao) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            validarConflitoAgenda(conn, sessao, sessao.getId());

            conn.createQuery(QueryLoader.get("sessao.update")).bind(sessao).executeUpdate();
            conn.commit();
        }
    }

    /**
     * Regra de agenda com sessões em grupo:
     * 1. overlap com modalidade DIFERENTE é sempre conflito;
     * 2. overlap na MESMA modalidade é permitido até esgotar a capacidade
     *    simultânea da modalidade (ex.: pilates 4 pacientes na mesma aula).
     * A sessão em edição (sessaoExcluir) é excluída da contagem; na criação
     * é null.
     */
    private void validarConflitoAgenda(Connection conn, Sessao sessao, Integer sessaoExcluir) {
        Modalidade modalidade = getModalidadeById(sessao.getModalidadeId());
        int capacidade = (modalidade != null && modalidade.getCapacidadeSimultanea() != null)
                ? modalidade.getCapacidadeSimultanea() : 1;

        Long outrasModalidades = conn.createQuery(QueryLoader.get("sessao.verificarConflitoOutraModalidade"))
                .addParameter("profissionalId", sessao.getProfissionalId())
                .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                .addParameter("dataHoraFim", sessao.getDataHoraFim())
                .addParameter("modalidadeId", sessao.getModalidadeId())
                .addParameter("sessaoId", sessaoExcluir)
                .executeScalar(Long.class);

        if (outrasModalidades != null && outrasModalidades > 0) {
            throw new IllegalStateException("O profissional já possui atendimento de outra modalidade neste horário.");
        }

        Long mesmoGrupo = conn.createQuery(QueryLoader.get("sessao.verificarCapacidadeGrupo"))
                .addParameter("profissionalId", sessao.getProfissionalId())
                .addParameter("dataHoraInicio", sessao.getDataHoraInicio())
                .addParameter("dataHoraFim", sessao.getDataHoraFim())
                .addParameter("modalidadeId", sessao.getModalidadeId())
                .addParameter("sessaoId", sessaoExcluir)
                .executeScalar(Long.class);

        if (mesmoGrupo != null && mesmoGrupo >= capacidade) {
            if (capacidade == 1) {
                // Atendimento individual: mensagem tradicional de conflito de horário
                throw new IllegalStateException("O profissional já possui atendimento neste horário.");
            }
            throw new IllegalStateException("Grupo cheio: capacidade de " + capacidade
                    + " atendimentos simultâneos nesta modalidade foi atingida.");
        }
    }

    /**
     * Exclusão permitida apenas para sessões ainda AGENDADA. Estados
     * REALIZADA, FALTOU e CANCELADA compõem o histórico operacional/clínico
     * e não são apagáveis.
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            String situacaoAtual = conn.createQuery(QueryLoader.get("sessao.getSituacao"))
                    .addParameter("id", id)
                    .executeScalar(String.class);

            if (situacaoAtual == null) {
                return; // sessão inexistente: delete é idempotente
            }

            SessaoSituacao situacao = SessaoSituacao.valueOf(situacaoAtual);
            if (situacao != SessaoSituacao.AGENDADA) {
                throw new IllegalStateException(
                        "Sessão com situação " + situacao + " integra o histórico e não pode ser apagada.");
            }

            conn.createQuery(QueryLoader.get("sessao.delete")).addParameter("id", id).executeUpdate();
            conn.commit();
        }
    }

    // =========================================================================
    // Fluxo da sala: transições de situação do agregado (dono: SessaoService)
    // =========================================================================

    /** Registro do atendimento editável na sala: fila (AGENDADA/CONFIRMADA),
     * EM_ANDAMENTO (rascunho) ou REALIZADA (consolidação). Terminais FALTOU e
     * CANCELADA são bloqueados. */
    private static void validarEstadoParaRegistro(SessaoSituacao situacao) {
        if (situacao == SessaoSituacao.FALTOU || situacao == SessaoSituacao.CANCELADA) {
            throw new IllegalStateException(
                    "Registro do atendimento indisponível: sessão " + situacao + ".");
        }
    }

    /**
     * Grava o registro do atendimento a partir do MODELO carregado/editado na
     * sala (textos clínicos + roteiro de condutas) — uma transação, um UPDATE
     * do registro, um sync do roteiro. É esta GRAVAÇÃO que inicia o
     * atendimento: a primeira gravação a partir da fila
     * (AGENDADA/CONFIRMADA) avança a sessão para EM_ANDAMENTO e marca o
     * início real (registro_dt_inicio, preservado via COALESCE). Em REALIZADA
     * consolida o texto sem nova transição.
     */
    public Sessao salvarRegistro(Sessao sessao) {
        return gravarAtendimento(sessao, false);
    }

    /**
     * Fim da sessão na sala: grava o registro do MODELO da tela (textos +
     * roteiro) E avança para REALIZADA marcando o fim real — tudo na mesma
     * transação. A conduta exigida é a do MODELO: o que o profissional
     * digitou conta mesmo sem um Salvar anterior (o POST do Finalizar bind
     * as textareas da mesma tela). A regra de exibição mora no agregado
     * (Sessao.podeFinalizar); o guard aqui revalida a situação do BANCO.
     */
    public Sessao finalizarAtendimento(Sessao sessao) {
        return gravarAtendimento(sessao, true);
    }

    /**
     * Núcleo único da sala: valida a máquina de estados sobre a situação
     * GRAVADA (nunca a do request), grava registro + roteiro e aplica a
     * transição pedida (iniciar, quando ainda está na fila; finalizar, quando
     * o botão foi o Finalizar) — tudo em uma transação.
     */
    private Sessao gravarAtendimento(Sessao sessao, boolean finalizar) {
        if (sessao == null || sessao.getId() == null) {
            throw new IllegalArgumentException("Operação cancelada: sessão não informada.");
        }
        Integer sessaoId = sessao.getId();
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            SessaoSituacao atual = carregarSituacao(conn, sessaoId);
            validarEstadoParaRegistro(atual);

            if (finalizar) {
                if (!atual.podeTransicionarPara(SessaoSituacao.REALIZADA)) {
                    throw new IllegalStateException(
                            "Não é possível finalizar atendimento a partir de " + atual + ".");
                }
                if (!sessao.possuiConduta()) {
                    throw new IllegalStateException(
                            "Finalização exige a conduta registrada do paciente.");
                }
            }

            if (atual == SessaoSituacao.AGENDADA || atual == SessaoSituacao.CONFIRMADA) {
                if (!atual.podeTransicionarPara(SessaoSituacao.EM_ANDAMENTO)) {
                    throw new IllegalStateException(
                            "Não é possível iniciar atendimento a partir de " + atual + ".");
                }
                conn.createQuery(QueryLoader.get("sessao.iniciarAtendimento"))
                        .addParameter("id", sessaoId)
                        .executeUpdate();
            }

            if (finalizar) {
                conn.createQuery(QueryLoader.get("sessao.finalizarAtendimento"))
                        .addParameter("id", sessaoId)
                        .executeUpdate();
            }

            conn.createQuery(QueryLoader.get("sessao.updateRegistro"))
                    .addParameter("id", sessaoId)
                    .addParameter("registroRelato", sessao.getRegistroRelato())
                    .addParameter("registroAvaliacao", sessao.getRegistroAvaliacao())
                    .addParameter("registroConduta", sessao.getRegistroConduta())
                    .addParameter("registroOrientacoes", sessao.getRegistroOrientacoes())
                    .executeUpdate();

            // Roteiro: coleção DEPENDENTE do agregado — o que está no modelo é
            // o estado do roteiro, e entra na MESMA transação do registro.
            // null = agregado carregado sem o roteiro (não sincroniza).
            if (sessao.getProcedimentos() != null) {
                sincronizarRoteiro(conn, sessaoId,
                        sessao.getProcedimentos().stream()
                                .map(SessaoProcedimento::getProcedimentoId)
                                .toList());
            }

            conn.commit();
        }
        return get(sessaoId);
    }

    // =========================================================================
    // Internos
    // =========================================================================

    /** Roteiro da sessão com o grafo do catálogo (procedimento + modalidade). */
    private List<SessaoProcedimento> carregarRoteiro(Integer sessaoId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("sessao_procedimento.getBySessao"))
                    .addParameter("sessaoId", sessaoId)
                    .executeAndFetch(SessaoProcedimento.class);
        }
    }

    /**
     * Sincroniza o roteiro com as condutas do modelo: remove as que saíram e
     * insere as que entraram, SEM abrir conexão nem comitar — roda na
     * transação de gravarAtendimento (registro + situação + roteiro: ou tudo,
     * ou nada). Condutas novas entram sem observação (a execução fica no
     * registro_conduta).
     */
    private void sincronizarRoteiro(Connection conn, Integer sessaoId, List<Integer> escolhidos) {
        List<SessaoProcedimento> roteiro = conn.createQuery(QueryLoader.get("sessao_procedimento.getBySessao"))
                .addParameter("sessaoId", sessaoId)
                .executeAndFetch(SessaoProcedimento.class);

        for (SessaoProcedimento item : roteiro) {
            if (!escolhidos.contains(item.getProcedimentoId())) {
                conn.createQuery(QueryLoader.get("sessao_procedimento.delete"))
                        .addParameter("sessaoId", sessaoId)
                        .addParameter("procedimentoId", item.getProcedimentoId())
                        .executeUpdate();
            }
        }
        for (Integer idEscolhido : escolhidos) {
            boolean jaNoRoteiro = roteiro.stream()
                    .anyMatch(item -> idEscolhido.equals(item.getProcedimentoId()));
            if (!jaNoRoteiro) {
                conn.createQuery(QueryLoader.get("sessao_procedimento.insert"))
                        .addParameter("sessaoId", sessaoId)
                        .addParameter("procedimentoId", idEscolhido)
                        .addParameter("observacao", (Object) null)
                        .executeUpdate();
            }
        }
    }

    private SessaoSituacao carregarSituacao(Connection conn, Integer sessaoId) {
        String situacaoStr = conn.createQuery(QueryLoader.get("sessao.getSituacao"))
                .addParameter("id", sessaoId)
                .executeScalar(String.class);
        if (situacaoStr == null) {
            throw new IllegalArgumentException("Sessão informada não existe.");
        }
        return SessaoSituacao.valueOf(situacaoStr);
    }
}
