package com.mousseomanos.taskup.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mousseomanos.taskup.R;
import com.mousseomanos.taskup.model.Produto;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VendaProdutoAdapter extends RecyclerView.Adapter<VendaProdutoAdapter.VH> {

    public interface OnAdicionar { void adicionar(Produto p); }

    private final List<Produto> lista = new ArrayList<>();
    private final Map<Long, Integer> carrinho = new HashMap<>();
    private final OnAdicionar listener;
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public VendaProdutoAdapter(OnAdicionar listener) {
        this.listener = listener;
    }

    // Chamado pela tela quando o ViewModel manda dados novos
    public void atualizar(List<Produto> produtos, Map<Long, Integer> pedido) {
        lista.clear();
        if (produtos != null) lista.addAll(produtos);
        carrinho.clear();
        if (pedido != null) carrinho.putAll(pedido);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_venda_produto, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Produto p = lista.get(position);
        int estoque = p.getQuantidade() == null ? 0 : p.getQuantidade();
        int noPedido = carrinho.getOrDefault(p.getId(), 0);
        int disponivel = estoque - noPedido;

        h.tvNome.setText(p.getNome());
        h.tvPreco.setText(moeda.format(p.getPreco() == null ? 0 : p.getPreco()));

        if (estoque == 0) {
            h.tvEstoque.setText("Sem estoque");
            h.tvEstoque.setTextColor(Color.parseColor("#C62828"));
            h.btnAdicionar.setEnabled(false);
            h.btnAdicionar.setText("Indisponível");
        } else {
            String texto = "Disponível: " + disponivel;
            if (estoque <= 5) texto += "  ⚠ estoque baixo";
            h.tvEstoque.setText(texto);
            h.tvEstoque.setTextColor(Color.parseColor(estoque <= 5 ? "#E65100" : "#2E7D32"));
            h.btnAdicionar.setEnabled(disponivel > 0);
            h.btnAdicionar.setText("Adicionar");
        }

        h.btnAdicionar.setOnClickListener(v -> listener.adicionar(p));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNome, tvPreco, tvEstoque;
        Button btnAdicionar;

        VH(View v) {
            super(v);
            tvNome = v.findViewById(R.id.tvNomeVenda);
            tvPreco = v.findViewById(R.id.tvPrecoVenda);
            tvEstoque = v.findViewById(R.id.tvEstoqueVenda);
            btnAdicionar = v.findViewById(R.id.btnAdicionar);
        }
    }
}