package com.gilbertoca.fisio.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro das sessões abertas do sistema — a tela "Usuários Conectados"
 * do CredBem. Em memória (por design: sessão é efêmera), chaveada pelo id
 * da sessão HTTP, alimentada no login e atualizada a cada página
 * autenticada (último acesso).
 *
 * Vazamento limitado e aceito: sessões que expiram sem logout ficam na
 * memória até o restart — o registro é minúsculo e inofensivo.
 */
public class ConectadosService {

    /** Linha da tela: Usuário, Papel, Host (IP), Último Acesso, Início. */
    public record Conectado(String username, String papel, String host,
            LocalDateTime ultimoAcesso, LocalDateTime inicio) {
    }

    private static final Map<String, Conectado> CONECTADOS = new ConcurrentHashMap<>();

    /** Marca a entrada da sessão (no login bem-sucedido). */
    public static void registrar(String sessionId, String username,
            String papel, String host) {
        LocalDateTime agora = LocalDateTime.now();
        CONECTADOS.put(sessionId, new Conectado(username, papel, host, agora, agora));
    }

    /** Atualiza o último acesso (a cada página autenticada). Sem registro
     * (sessão criada fora do login), não faz nada. */
    public static void atualizar(String sessionId) {
        Conectado atual = CONECTADOS.get(sessionId);
        if (atual != null) {
            CONECTADOS.put(sessionId, new Conectado(atual.username(),
                    atual.papel(), atual.host(), LocalDateTime.now(), atual.inicio()));
        }
    }

    /** Remove o registro (no logout), antes de invalidar a sessão. */
    public static void remover(String sessionId) {
        CONECTADOS.remove(sessionId);
    }

    /** As sessões abertas, do início mais antigo para o mais recente. */
    public static List<Conectado> getConectados() {
        List<Conectado> lista = new ArrayList<>(CONECTADOS.values());
        lista.sort((a, b) -> a.inicio().compareTo(b.inicio()));
        return lista;
    }

    /** Para testes: esvazia o registro entre casos. */
    public static void limpar() {
        CONECTADOS.clear();
    }
}
