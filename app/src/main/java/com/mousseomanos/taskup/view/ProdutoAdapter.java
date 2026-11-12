package com.mousseomanos.taskup.view;

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
import java.util.List;
import java.util.Locale;

public class ProdutoAdapter extends RecyclerView.Adapter<ProdutoAdapter.ViewHolder> {

    public static final int LIMITE_ESTOQUE_BAIXO = 5;

    public interface Acoes {
        void onEditar(Produto p);
        void onAjustarEstoque(Produto p, int quantidade);
        void onAlternarAtivo(Produto p);
        void onExcluir(Produto p);
    }

    private final List<Produto> produtos = new ArrayList<>();
    private final Acoes acoes;
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    public ProdutoAdapter(Acoes acoes) {
        this.acoes = acoes;
    }

    public void setProdutos(List<Produto> novos) {
        produtos.clear();
        produtos.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_produto, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Produto p = produtos.get(position);
        int qtd = p.getQuantidade() == null ? 0 : p.getQuantidade();
        boolean ativo = p.getAtivo() == null || p.getAtivo();

        h.txtNome.setText(p.getNome());
        h.txtPreco.setText(moeda.format(p.getPreco() == null ? 0 : p.getPreco()));
        h.txtQuantidade.setText(String.valueOf(qtd));

        if (!ativo) {
            h.txtSituacao.setText("INATIVO");
            h.txtSituacao.setVisibility(View.VISIBLE);
        } else if (qtd <= LIMITE_ESTOQUE_BAIXO) {
            h.txtSituacao.setText("ESTOQUE BAIXO");
            h.txtSituacao.setVisibility(View.VISIBLE);
        } else {
            h.txtSituacao.setVisibility(View.GONE);
        }

        h.itemView.setAlpha(ativo ? 1f : 0.55f);
        h.btnAtivo.setText(ativo ? "Inativar" : "Ativar");

        h.btnMenos.setOnClickListener(v -> acoes.onAjustarEstoque(p, -1));
        h.btnMais.setOnClickListener(v -> acoes.onAjustarEstoque(p, 1));
        h.btnEditar.setOnClickListener(v -> acoes.onEditar(p));
        h.btnAtivo.setOnClickListener(v -> acoes.onAlternarAtivo(p));
        h.btnExcluir.setOnClickListener(v -> acoes.onExcluir(p));
    }

    @Override
    public int getItemCount() {
        return produtos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtNome, txtPreco, txtQuantidade, txtSituacao;
        Button btnMenos, btnMais, btnEditar, btnAtivo, btnExcluir;

        ViewHolder(View v) {
            super(v);
            txtNome = v.findViewById(R.id.txtNome);
            txtPreco = v.findViewById(R.id.txtPreco);
            txtQuantidade = v.findViewById(R.id.txtQuantidade);
            txtSituacao = v.findViewById(R.id.txtSituacao);
            btnMenos = v.findViewById(R.id.btnMenos);
            btnMais = v.findViewById(R.id.btnMais);
            btnEditar = v.findViewById(R.id.btnEditar);
            btnAtivo = v.findViewById(R.id.btnAtivo);
            btnExcluir = v.findViewById(R.id.btnExcluir);
        }
    }
}