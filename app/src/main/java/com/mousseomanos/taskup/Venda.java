package com.mousseomanos.taskup;

import java.util.List;

public class Venda {
    private Long id;
    private String dataHora;
    private String formaPagamento;
    private String statusPagamento;
    private Double valorTotal;
    private List<Item> itens;

    public Long getId() { return id; }
    public String getDataHora() { return dataHora; }
    public String getFormaPagamento() { return formaPagamento; }
    public String getStatusPagamento() { return statusPagamento; }
    public Double getValorTotal() { return valorTotal; }
    public List<Item> getItens() { return itens; }

    public static class Item {
        private String nomeProduto;
        private Integer quantidade;
        private Double subtotal;

        public String getNomeProduto() { return nomeProduto; }
        public Integer getQuantidade() { return quantidade; }
        public Double getSubtotal() { return subtotal; }
    }
}