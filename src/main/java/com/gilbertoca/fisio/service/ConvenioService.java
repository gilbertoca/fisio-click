package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Convenio;
import org.sql2o.Connection;
import java.util.List;
import com.gilbertoca.fisio.infra.DataSourceManager;

public class ConvenioService {
    public ConvenioService() {}

    public Convenio get(Integer id) {
        if (id == null) return null;
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    public List<Convenio> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public void create(Convenio convenio) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("convenio.create")).bind(convenio).executeUpdate();
        }
    }

    public void update(Convenio convenio) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("convenio.update")).bind(convenio).executeUpdate();
        }
    }

    /**
     * Guard de deleção na MESMA transação do DELETE (mesmo princípio de
     * ProcedimentoService.delete): convênio com clientes amarrados (FK
     * restritiva) ou faturamentos (lote) vinculados não é apagável.
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Long dependentes = conn.createQuery(QueryLoader.get("convenio.countDependentes"))
                    .addParameter("id", id)
                    .executeScalar(Long.class);
            if (dependentes != null && dependentes > 0) {
                throw new IllegalStateException(
                        "Convênio não pode ser excluído: existem clientes ou faturamentos vinculados a ele.");
            }
            conn.createQuery(QueryLoader.get("convenio.delete"))
                    .addParameter("id", id)
                    .executeUpdate();
            conn.commit();
        }
    }

    public List<Convenio> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAllLikeNome")).addParameter("nome", nome).executeAndFetch(Convenio.class);
        }
    }
}
