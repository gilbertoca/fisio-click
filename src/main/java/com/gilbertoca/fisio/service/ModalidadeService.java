package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Modalidade;
import org.sql2o.Connection;
import java.util.List;
import com.gilbertoca.fisio.infra.DataSourceManager;

public class ModalidadeService {

    public ModalidadeService() {  }
    
    public Modalidade get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.get")).addParameter("id", id).executeAndFetchFirst(Modalidade.class);
        }
    }

    public List<Modalidade> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.getAll")).executeAndFetch(Modalidade.class);
        }
    }

    public void create(Modalidade modalidade) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("modalidade.create")).bind(modalidade).executeUpdate();
        }
    }

    public void update(Modalidade modalidade) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("modalidade.update")).bind(modalidade).executeUpdate();
        }
    }

    /**
     * Guard de deleção na MESMA transação do DELETE (mesmo princípio de
     * ProcedimentoService.delete): modalidade com procedimentos amarrados
     * (catálogo) ou sessões (agenda) vinculadas não é apagável.
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Long dependentes = conn.createQuery(QueryLoader.get("modalidade.countDependentes"))
                    .addParameter("id", id)
                    .executeScalar(Long.class);
            if (dependentes != null && dependentes > 0) {
                throw new IllegalStateException(
                        "Modalidade não pode ser excluída: existem procedimentos ou sessões vinculadas a ela.");
            }
            conn.createQuery(QueryLoader.get("modalidade.delete"))
                    .addParameter("id", id)
                    .executeUpdate();
            conn.commit();
        }
    }

    public List<Modalidade> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("modalidade.getAllLikeNome")).addParameter("nome", nome).executeAndFetch(Modalidade.class);
        }
    }
}
