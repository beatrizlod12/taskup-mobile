package com.mousseomanos.taskup;

public class Produto {
    private Long id;
    private String nome;
    private Double preco;
    private Integer quantidade;
    private Boolean ativo;

    public Produto() { }

    public Produto(String nome, Double preco, Integer quantidade) {
        this.nome = nome;
        this.preco = preco;
        this.quantidade = quantidade;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public Double getPreco() { return preco; }
    public Integer getQuantidade() { return quantidade; }
    public Boolean getAtivo() { return ativo; }
}