package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Procedimento;
import java.util.List;
import org.sql2o.Connection;

/**
 * Dono do catálogo de procedimentos (técnicas/recursos). O ROTEIRO da sessão
 * (sessao_procedimento) NÃO mora aqui: é coleção dependente do agregado
 * Sessao e é lido/gravado pelo SessaoService, na mesma transação do registro
 * do atendimento. Este service só responde pelo catálogo — inclusive pelas
 * sugestões de conduta da modalidade em atendimento.
 *
 * Regra de integridade: procedimento usado em roteiro clínico NÃO é apagável
 * (FK restritiva e histórico do atendimento) — vira INATIVO e continua
 * valendo nos roteiros antigos. Ver delete().
 */
public class ProcedimentoService {

    public ProcedimentoService() {  }

    public Procedimento get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("procedimento.get"))
                    .addParameter("id", id)
                    .executeAndFetchFirst(Procedimento.class);
        }
    }

    public List<Procedimento> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("procedimento.getAll"))
                    .executeAndFetch(Procedimento.class);
        }
    }

    public List<Procedimento> getAllLikeNome(String nome) {
        return getAllFiltrado(nome, null, null);
    }

    /**
     * Catálogo filtrado pelas colunas da listagem: nome, código RBPF/TUSS e
     * modalidade. Texto em branco é ignorado na query.
     */
    public List<Procedimento> getAllFiltrado(String nome, String codigo, String modalidadeNome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("procedimento.getAllFiltrado"))
                    .addParameter("nome", textoOuNulo(nome))
                    .addParameter("codigo", textoOuNulo(codigo))
                    .addParameter("modalidadeNome", textoOuNulo(modalidadeNome))
                    .executeAndFetch(Procedimento.class);
        }
    }

    /** Texto de filtro: em branco vira null (condição desligada na query). */
    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /**
     * Catálogo completo de UMA modalidade (todos os estados — ATIVO e
     * INATIVO), ordem do catálogo. Painel da modalidade
     * (ModalidadeProcedimentoViewPage). Query sem join: a amarra é a própria
     * modalidade em foco.
     */
    public List<Procedimento> getByModalidade(Integer modalidadeId) {
        if (modalidadeId == null) {
            return List.of();
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("procedimento.getByModalidade"))
                    .addParameter("modalidadeId", modalidadeId)
                    .executeAndFetch(Procedimento.class);
        }
    }

    public void create(Procedimento procedimento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("procedimento.create"))
                    .bind(procedimento)
                    .executeUpdate();
        }
    }

    public void update(Procedimento procedimento) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("procedimento.update"))
                    .bind(procedimento)
                    .executeUpdate();
        }
    }

    /**
     * Guard de deleção na MESMA transação do DELETE: se o procedimento já
     * entrou em algum roteiro clínico, a exclusão é bloqueada — o histórico
     * do atendimento é imutável (mesmo princípio de SessaoService.delete).
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Long uso = conn.createQuery(QueryLoader.get("procedimento.countUsoEmSessoes"))
                    .addParameter("id", id)
                    .executeScalar(Long.class);
            if (uso != null && uso > 0) {
                throw new IllegalStateException(
                        "Procedimento não pode ser excluído: já foi utilizado em roteiros de atendimento. Inative-o no catálogo.");
            }
            conn.createQuery(QueryLoader.get("procedimento.delete"))
                    .addParameter("id", id)
                    .executeUpdate();
            conn.commit();
        }
    }

    /**
     * Sugestões de conduta para a sessão em atendimento: itens ATIVO de uso
     * geral (sem amarra de modalidade) OU amarrados à modalidade da sessão.
     * modalidadeId null devolve apenas os de uso geral.
     */
    public List<Procedimento> getSugestoesPorModalidade(Integer modalidadeId) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("procedimento.getSugestoesPorModalidade"))
                    .addParameter("modalidadeId", modalidadeId)
                    .executeAndFetch(Procedimento.class);
        }
    }
}
