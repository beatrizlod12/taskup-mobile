package com.mousseomanos.taskup.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mousseomanos.taskup.model.VendaRepository;
import com.mousseomanos.taskup.model.Produto;
import com.mousseomanos.taskup.model.Venda;
import com.mousseomanos.taskup.model.VendaRequest;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VendaViewModel extends ViewModel {

    private final VendaRepository repo = new VendaRepository();
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    // ---- Estado da tela ----
    private final MutableLiveData<List<Produto>> produtos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Map<Long, Integer>> carrinho = new MutableLiveData<>(new LinkedHashMap<>());
    private final MutableLiveData<String> itensPedido = new MutableLiveData<>("Nenhum item adicionado");
    private final MutableLiveData<String> total = new MutableLiveData<>("Total: R$ 0,00");
    private final MutableLiveData<Boolean> enviando = new MutableLiveData<>(false);

    // ---- Eventos (acontecem uma vez; a tela avisa quando já tratou) ----
    private final MutableLiveData<String> mensagem = new MutableLiveData<>();
    private final MutableLiveData<Venda> vendaCriada = new MutableLiveData<>();
    private final MutableLiveData<String> historico = new MutableLiveData<>();

    public LiveData<List<Produto>> getProdutos() { return produtos; }
    public LiveData<Map<Long, Integer>> getCarrinho() { return carrinho; }
    public LiveData<String> getItensPedido() { return itensPedido; }
    public LiveData<String> getTotal() { return total; }
    public LiveData<Boolean> getEnviando() { return enviando; }
    public LiveData<String> getMensagem() { return mensagem; }
    public LiveData<Venda> getVendaCriada() { return vendaCriada; }
    public LiveData<String> getHistorico() { return historico; }

    // ---------- Produtos ----------
    public void carregarProdutos() {
        repo.listarProdutos(new VendaRepository.Retorno<List<Produto>>() {
            @Override public void sucesso(List<Produto> lista) {
                List<Produto> ativos = new ArrayList<>();
                if (lista != null) {
                    for (Produto p : lista) {
                        if (p.getAtivo() == null || p.getAtivo()) ativos.add(p); // só ativos
                    }
                }
                produtos.setValue(ativos);
                atualizarResumo();
            }
            @Override public void erro(String msg) { mensagem.setValue(msg); }
        });
    }

    // ---------- Pedido (regras de negócio da tela) ----------
    public void adicionar(Produto p) {
        int estoque = p.getQuantidade() == null ? 0 : p.getQuantidade();
        Map<Long, Integer> c = new LinkedHashMap<>(carrinho.getValue());
        int noPedido = c.getOrDefault(p.getId(), 0);

        if (estoque == 0) { mensagem.setValue(p.getNome() + " está sem estoque."); return; }
        if (noPedido >= estoque) {
            mensagem.setValue("Só há " + estoque + " de " + p.getNome() + " em estoque.");
            return;
        }

        c.put(p.getId(), noPedido + 1);
        carrinho.setValue(c);
        atualizarResumo();
    }

    public void limpar() {
        carrinho.setValue(new LinkedHashMap<>());
        atualizarResumo();
    }

    private void atualizarResumo() {
        Map<Long, Integer> c = carrinho.getValue();
        if (c == null || c.isEmpty()) {
            itensPedido.setValue("Nenhum item adicionado");
            total.setValue("Total: " + moeda.format(0));
            return;
        }
        StringBuilder sb = new StringBuilder();
        double soma = 0;
        for (Map.Entry<Long, Integer> e : c.entrySet()) {
            Produto p = buscarProduto(e.getKey());
            if (p == null) continue;
            double subtotal = p.getPreco() * e.getValue();
            soma += subtotal;
            sb.append(e.getValue()).append("x ").append(p.getNome())
                    .append(" — ").append(moeda.format(subtotal)).append("\n");
        }
        itensPedido.setValue(sb.toString().trim());
        total.setValue("Total: " + moeda.format(soma));
    }

    private Produto buscarProduto(Long id) {
        List<Produto> lista = produtos.getValue();
        if (lista != null) for (Produto p : lista) if (p.getId().equals(id)) return p;
        return null;
    }

    // ---------- Venda ----------
    public void finalizar(String formaPagamento) {
        Map<Long, Integer> c = carrinho.getValue();
        if (c == null || c.isEmpty()) { mensagem.setValue("Adicione pelo menos um produto."); return; }

        List<VendaRequest.Item> itens = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : c.entrySet()) {
            itens.add(new VendaRequest.Item(e.getKey(), e.getValue()));
        }

        enviando.setValue(true);
        repo.criarVenda(new VendaRequest(formaPagamento, itens), new VendaRepository.Retorno<Venda>() {
            @Override public void sucesso(Venda v) {
                enviando.setValue(false);
                vendaCriada.setValue(v);   // a tela abre a janela de "Confirmar pagamento"
            }
            @Override public void erro(String msg) {
                enviando.setValue(false);
                mensagem.setValue(msg);    // o carrinho NÃO é apagado: dá para tentar de novo
            }
        });
    }

    public void confirmarPagamento(Long id) {
        repo.confirmarPagamento(id, new VendaRepository.Retorno<Venda>() {
            @Override public void sucesso(Venda v) {
                mensagem.setValue("✔ Pagamento efetuado! Estoque atualizado.");
                limpar();
                carregarProdutos(); // mostra o estoque já diminuído
            }
            @Override public void erro(String msg) { mensagem.setValue(msg); }
        });
    }

    // ---------- Histórico ----------
    public void carregarHistorico() {
        repo.listarVendas(new VendaRepository.Retorno<List<Venda>>() {
            @Override public void sucesso(List<Venda> vendas) {
                StringBuilder sb = new StringBuilder();
                if (vendas != null) {
                    for (Venda v : vendas) {
                        boolean pago = "PAGO".equals(v.getStatusPagamento());
                        sb.append("Venda nº ").append(v.getId()).append(" — ").append(data(v.getDataHora())).append("\n")
                                .append(pago ? "✔ Pagamento efetuado" : "⏳ Aguardando pagamento")
                                .append(" | ").append(v.getFormaPagamento()).append("\n");
                        if (v.getItens() != null) {
                            for (Venda.Item i : v.getItens()) {
                                sb.append("   ").append(i.getQuantidade()).append("x ").append(i.getNomeProduto())
                                        .append(" — ").append(moeda.format(i.getSubtotal())).append("\n");
                            }
                        }
                        sb.append("Total: ").append(moeda.format(v.getValorTotal())).append("\n\n");
                    }
                }
                historico.setValue(sb.length() == 0 ? "Nenhuma venda registrada ainda." : sb.toString().trim());
            }
            @Override public void erro(String msg) { mensagem.setValue(msg); }
        });
    }

    private String data(String iso) { // "2026-10-19T21:35:00" -> "19/10 21:35"
        if (iso == null || iso.length() < 16) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + " " + iso.substring(11, 16);
    }

    // ---- A tela avisa que já tratou o evento ----
    public void mensagemExibida() { mensagem.setValue(null); }
    public void vendaExibida() { vendaCriada.setValue(null); }
    public void historicoExibido() { historico.setValue(null); }
}