package com.mousseomanos.taskup;

import java.util.List;

public class VendaRequest {
    private String formaPagamento;
    private List<Item> itens;

    public VendaRequest(String formaPagamento, List<Item> itens) {
        this.formaPagamento = formaPagamento;
        this.itens = itens;
    }

    public static class Item {
        private Long produtoId;
        private Integer quantidade;

        public Item(Long produtoId, Integer quantidade) {
            this.produtoId = produtoId;
            this.quantidade = quantidade;
        }
    }
}