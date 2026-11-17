package com.gilbertoca.fisio.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Papel do módulo de segurança: agrupa permissões (modulo:acao) e é
 * distribuído aos usuários via usuario_papel.
 */
public class Papel implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String nome;
    private String descricao;

    public Papel() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Papel other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return nome != null ? nome : super.toString();
    }
}
