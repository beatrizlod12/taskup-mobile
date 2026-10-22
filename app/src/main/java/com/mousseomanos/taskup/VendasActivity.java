package com.mousseomanos.taskup;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendasActivity extends AppCompatActivity {

    private final ProdutoApi api = RetrofitClient.getApi();
    private final List<Produto> produtos = new ArrayList<>();
    private final Map<Long, Integer> carrinho = new LinkedHashMap<>(); // produtoId -> quantidade
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private VendaProdutoAdapter adapter;
    private TextView tvItens, tvTotal;
    private RadioGroup rgPagamento;
    private Button btnFinalizar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vendas);

        // Mantém a tela longe da barra de status e da barra de navegação (+16dp de margem)
        int margem = (int) (16 * getResources().getDisplayMetrics().density);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left + margem, b.top + margem, b.right + margem, b.bottom + margem);
            return insets;
        });

        tvItens = findViewById(R.id.tvItensPedido);
        tvTotal = findViewById(R.id.tvTotal);
        rgPagamento = findViewById(R.id.rgPagamento);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        RecyclerView rv = findViewById(R.id.rvProdutosVenda);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VendaProdutoAdapter(produtos, carrinho, this::adicionar);
        rv.setAdapter(adapter);

        findViewById(R.id.btnLimpar).setOnClickListener(v -> {
            carrinho.clear();
            atualizarPedido();
        });
        btnFinalizar.setOnClickListener(v -> finalizar());
        findViewById(R.id.btnHistorico).setOnClickListener(v -> mostrarHistorico());

        // Botão Voltar: fecha esta tela e volta para a tela inicial
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarProdutos(); // sempre busca de novo: produtos novos aparecem no pedido
    }

    // ---------- Produtos ----------
    private void carregarProdutos() {
        api.listar(null).enqueue(new Callback<List<Produto>>() {
            @Override
            public void onResponse(Call<List<Produto>> call, Response<List<Produto>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    produtos.clear();
                    for (Produto p : r.body()) {
                        if (p.getAtivo() == null || p.getAtivo()) produtos.add(p); // só ativos
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    toast(erro(r));
                }
            }
            @Override
            public void onFailure(Call<List<Produto>> call, Throwable t) {
                toast("Sem conexão com o servidor. Verifique a internet.");
            }
        });
    }

    // ---------- Pedido ----------
    private void adicionar(Produto p) {
        int estoque = p.getQuantidade() == null ? 0 : p.getQuantidade();
        int noPedido = carrinho.getOrDefault(p.getId(), 0);

        if (estoque == 0) { toast(p.getNome() + " está sem estoque."); return; }
        if (noPedido >= estoque) { toast("Só há " + estoque + " de " + p.getNome() + " em estoque."); return; }

        carrinho.put(p.getId(), noPedido + 1);
        atualizarPedido();
    }

    private void atualizarPedido() {
        if (carrinho.isEmpty()) {
            tvItens.setText("Nenhum item adicionado");
            tvTotal.setText("Total: " + moeda.format(0));
        } else {
            StringBuilder sb = new StringBuilder();
            double total = 0;
            for (Map.Entry<Long, Integer> e : carrinho.entrySet()) {
                Produto p = buscarProduto(e.getKey());
                if (p == null) continue;
                double subtotal = p.getPreco() * e.getValue();
                total += subtotal;
                sb.append(e.getValue()).append("x ").append(p.getNome())
                        .append(" — ").append(moeda.format(subtotal)).append("\n");
            }
            tvItens.setText(sb.toString().trim());
            tvTotal.setText("Total: " + moeda.format(total));
        }
        adapter.notifyDataSetChanged(); // atualiza o "Disponível" de cada cartão
    }

    private Produto buscarProduto(Long id) {
        for (Produto p : produtos) if (p.getId().equals(id)) return p;
        return null;
    }

    private String formaPagamento() {
        int id = rgPagamento.getCheckedRadioButtonId();
        if (id == R.id.rbDinheiro) return "DINHEIRO";
        if (id == R.id.rbCartao) return "CARTAO";
        return "PIX";
    }

    // ---------- Venda ----------
    private void finalizar() {
        if (carrinho.isEmpty()) { toast("Adicione pelo menos um produto."); return; }

        List<VendaRequest.Item> itens = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : carrinho.entrySet()) {
            itens.add(new VendaRequest.Item(e.getKey(), e.getValue()));
        }

        btnFinalizar.setEnabled(false);
        btnFinalizar.setText("Enviando...");

        api.criarVenda(new VendaRequest(formaPagamento(), itens)).enqueue(new Callback<Venda>() {
            @Override
            public void onResponse(Call<Venda> call, Response<Venda> r) {
                liberarBotao();
                if (r.isSuccessful() && r.body() != null) perguntarPagamento(r.body());
                else toast(erro(r));
            }
            @Override
            public void onFailure(Call<Venda> call, Throwable t) {
                liberarBotao();
                toast("Sem conexão. O pedido não foi perdido, tente de novo.");
            }
        });
    }

    private void perguntarPagamento(Venda v) {
        new AlertDialog.Builder(this)
                .setTitle("Venda nº " + v.getId() + " criada")
                .setMessage("Total: " + moeda.format(v.getValorTotal())
                        + "\nForma de pagamento: " + v.getFormaPagamento()
                        + "\nStatus: aguardando pagamento"
                        + "\n\nConfirme quando o cliente pagar.")
                .setCancelable(false)
                .setPositiveButton("Confirmar pagamento", (d, w) -> confirmarPagamento(v.getId()))
                .setNegativeButton("Depois", null)
                .show();
    }

    private void confirmarPagamento(Long id) {
        api.confirmarPagamento(id).enqueue(new Callback<Venda>() {
            @Override
            public void onResponse(Call<Venda> call, Response<Venda> r) {
                if (r.isSuccessful()) {
                    toast("✔ Pagamento efetuado! Estoque atualizado.");
                    carrinho.clear();
                    atualizarPedido();
                    carregarProdutos(); // mostra o estoque já diminuído
                } else {
                    toast(erro(r));
                }
            }
            @Override
            public void onFailure(Call<Venda> call, Throwable t) {
                toast("Sem conexão. O pagamento não foi confirmado.");
            }
        });
    }

    // ---------- Histórico ----------
    private void mostrarHistorico() {
        api.listarVendas().enqueue(new Callback<List<Venda>>() {
            @Override
            public void onResponse(Call<List<Venda>> call, Response<List<Venda>> r) {
                if (!r.isSuccessful() || r.body() == null) { toast(erro(r)); return; }

                StringBuilder sb = new StringBuilder();
                for (Venda v : r.body()) {
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
                if (sb.length() == 0) sb.append("Nenhuma venda registrada ainda.");

                new AlertDialog.Builder(VendasActivity.this)
                        .setTitle("Histórico de vendas")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Fechar", null)
                        .show();
            }
            @Override
            public void onFailure(Call<List<Venda>> call, Throwable t) {
                toast("Sem conexão com o servidor.");
            }
        });
    }

    // ---------- Utilitários ----------
    private String data(String iso) { // "2026-10-19T21:35:00" -> "19/10 21:35"
        if (iso == null || iso.length() < 16) return "";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + " " + iso.substring(11, 16);
    }

    private String erro(Response<?> r) { // lê a mensagem que a API mandou
        try {
            return new JSONObject(r.errorBody().string()).optString("message", "Erro " + r.code());
        } catch (Exception e) {
            return "Erro " + r.code();
        }
    }

    private void liberarBotao() {
        btnFinalizar.setEnabled(true);
        btnFinalizar.setText("Finalizar venda");
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}