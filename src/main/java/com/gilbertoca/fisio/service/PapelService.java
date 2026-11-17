package com.gilbertoca.fisio.service;

import com.gilbertoca.fisio.infra.DataSourceManager;
import com.gilbertoca.fisio.infra.QueryLoader;
import com.gilbertoca.fisio.model.Papel;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.sql2o.Connection;

/**
 * Leitura dos papéis e do vocabulário de permissões. Papéis são
 * vocabulário fixo do módulo de segurança (seed do V6), e cada permissão
 * nasce via SQL assim que surge um recurso (tela|relatório) — nenhum dos
 * dois tem CRUD por tela.
 */
public class PapelService {

    private static final Map<String, String> MODULOS = Map.of(
            "cliente", "Cliente",
            "convenio", "Convênio",
            "modalidade", "Modalidade",
            "procedimento", "Procedimento",
            "profissional", "Profissional",
            "sessao", "Sessão",
            "usuario", "Usuário",
            "faturamento", "Faturamento");

    private static final Map<String, String> ACOES = Map.of(
            "adicionar", "Adicionar",
            "editar", "Editar",
            "excluir", "Excluir",
            "visualizar", "Visualizar",
            "imprimir", "Imprimir",
            "atender", "Atender");

    /** Todos os papéis, na ordem do seed — opções do select da tela. */
    public List<Papel> getAll() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("papel.getAll"))
                    .executeAndFetch(Papel.class);
        }
    }

    /** O vocabulário inteiro de permissões (modulo:acao), sem o curinga
     * universal — é a base do card "Permissões" (SIM/NÃO por recurso). */
    public List<String> getAllPermissoes() {
        try (Connection conn = DataSourceManager.getSql2o().open()) {
            return conn.createQuery(QueryLoader.get("permissao.getAll"))
                    .executeScalarList(String.class);
        }
    }

    /** "cliente:editar" → "Cliente Editar" — rótulo PT do card. */
    public static String rotulo(String permissao) {
        if (permissao == null || permissao.isBlank()) {
            return "";
        }
        String[] partes = permissao.split(":", 2);
        String modulo = MODULOS.getOrDefault(partes[0], capitalizar(partes[0]));
        if (partes.length == 1) {
            return modulo;
        }
        String acao = ACOES.getOrDefault(partes[1], capitalizar(partes[1]));
        return modulo + " " + acao;
    }

    private static String capitalizar(String palavra) {
        if (palavra == null || palavra.isBlank()) {
            return "";
        }
        return palavra.substring(0, 1).toUpperCase(Locale.ROOT)
                + palavra.substring(1);
    }
}
