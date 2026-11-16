package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.model.Cliente;
import com.gilbertoca.fisio.model.Convenio;
import org.sql2o.Connection;
import java.util.List;
import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;

public class ClienteService {

    public ClienteService() {
    }

    // Relationship Helper Method: Fetches an individual health insurance context
    public Convenio getConvenioById(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.get")).addParameter("id", id).executeAndFetchFirst(Convenio.class);
        }
    }

    // Relationship Helper Method: Fetches all insurance plans to populate view dropdowns
    public List<Convenio> getAllConvenios() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("convenio.getAll")).executeAndFetch(Convenio.class);
        }
    }

    public Cliente get(Integer id) {
        if (id == null) {
            return null;
        }
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.get")).addParameter("id", id).executeAndFetchFirst(Cliente.class);
        }
    }

    public List<Cliente> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAll")).executeAndFetch(Cliente.class);
        }
    }

    public void create(Cliente cliente) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            Object generatedId = conn.createQuery(QueryLoader.get("cliente.create"))
                    .bind(cliente)
                    .addParameter("convenioId", cliente.getConvenio() != null ? cliente.getConvenio().getId() : null)
                    .executeUpdate()
                    .getKey(); // Captura o ID gerado
            if (generatedId != null) {
                cliente.setId(((Number) generatedId).intValue()); // Seta no objeto Java
            }
        }
    }

    public void update(Cliente cliente) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            conn.createQuery(QueryLoader.get("cliente.update"))
                    .bind(cliente)
                    .addParameter("convenioId", cliente.getConvenio() != null ? cliente.getConvenio().getId() : null)
                    .executeUpdate();
        }
    }

    /**
     * Guard de deleção na MESMA transação do DELETE (mesmo princípio de
     * ProcedimentoService.delete): cliente com sessões (histórico da agenda)
     * ou faturamentos vinculados não é apagável.
     */
    public void delete(Integer id) {
        try (Connection conn = DataSourceManager.getSql2o().beginTransaction()) {
            Long dependentes = conn.createQuery(QueryLoader.get("cliente.countDependentes"))
                    .addParameter("id", id)
                    .executeScalar(Long.class);
            if (dependentes != null && dependentes > 0) {
                throw new IllegalStateException(
                        "Cliente não pode ser excluído: existem sessões ou faturamentos vinculados a ele.");
            }
            conn.createQuery(QueryLoader.get("cliente.delete"))
                    .addParameter("id", id)
                    .executeUpdate();
            conn.commit();
        }
    }

    public List<Cliente> getAllLikeNome(String nome) {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("cliente.getAllLikeNome")).addParameter("nome", nome).executeAndFetch(Cliente.class);
        }
    }
}
