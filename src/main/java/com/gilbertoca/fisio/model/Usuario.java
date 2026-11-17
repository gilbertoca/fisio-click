package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Usuário do sistema (módulo de segurança): conta de acesso com
 * autenticação própria (senha PBKDF2 + salt) e papéis que carregam as
 * permissões. O vínculo com profissional é opcional (1:1): o usuário
 * clínico amarra a conta à própria ficha; o administrador não tem.
 */
public class Usuario implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String username;
    private String email;
    /** Hash PBKDF2 (hex) — nunca sai para a tela. */
    private String senha;
    /** Salt aleatório (hex) — par do hash. */
    private String salt;
    private boolean ativo = true;
    /** Vínculo opcional (FK profissional_id). */
    private Profissional profissional;
    /** Papéis carregados pelo service (não é coluna). */
    private List<Papel> papeis = new ArrayList<>();

    public Usuario() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Profissional getProfissional() { return profissional; }
    public void setProfissional(Profissional profissional) { this.profissional = profissional; }

    public List<Papel> getPapeis() { return papeis; }
    public void setPapeis(List<Papel> papeis) { this.papeis = papeis; }

    /** Nomes dos papéis (para exibição — "Gerente, Operador"). */
    public String getNomesPapeis() {
        return papeis.stream()
                .map(Papel::getNome)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Usuario other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return username != null ? username : super.toString();
    }
}
