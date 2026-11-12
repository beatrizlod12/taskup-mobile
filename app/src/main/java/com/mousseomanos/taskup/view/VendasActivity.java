package com.mousseomanos.taskup.view;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mousseomanos.taskup.R;
import com.mousseomanos.taskup.viewmodel.VendaViewModel;

import java.text.NumberFormat;
import java.util.Locale;

public class VendasActivity extends AppCompatActivity {

    private VendaViewModel vm;
    private VendaProdutoAdapter adapter;
    private TextView tvItens, tvTotal;
    private RadioGroup rgPagamento;
    private Button btnFinalizar;
    private final NumberFormat moeda = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vendas);

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

        // ---- ViewModel ----
        vm = new ViewModelProvider(this).get(VendaViewModel.class);

        // ---- Lista de produtos ----
        RecyclerView rv = findViewById(R.id.rvProdutosVenda);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new VendaProdutoAdapter(p -> vm.adicionar(p));
        rv.setAdapter(adapter);

        observar();

        // ---- Cliques: só repassam para o ViewModel ----
        findViewById(R.id.btnLimpar).setOnClickListener(v -> vm.limpar());
        btnFinalizar.setOnClickListener(v -> vm.finalizar(formaPagamento()));
        findViewById(R.id.btnHistorico).setOnClickListener(v -> vm.carregarHistorico());
        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.carregarProdutos(); // produtos novos aparecem no pedido
    }

    private String formaPagamento() {
        int id = rgPagamento.getCheckedRadioButtonId();
        if (id == R.id.rbDinheiro) return "DINHEIRO";
        if (id == R.id.rbCartao) return "CARTAO";
        return "PIX";
    }

    // A tela observa o ViewModel e se atualiza sozinha
    private void observar() {
        vm.getProdutos().observe(this, lista -> adapter.atualizar(lista, vm.getCarrinho().getValue()));
        vm.getCarrinho().observe(this, c -> adapter.atualizar(vm.getProdutos().getValue(), c));

        vm.getItensPedido().observe(this, texto -> tvItens.setText(texto));
        vm.getTotal().observe(this, texto -> tvTotal.setText(texto));

        vm.getEnviando().observe(this, enviando -> {
            btnFinalizar.setEnabled(!enviando);
            btnFinalizar.setText(enviando ? "Enviando..." : "Finalizar venda");
        });

        vm.getMensagem().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            vm.mensagemExibida();
        });

        vm.getVendaCriada().observe(this, v -> {
            if (v == null) return;
            vm.vendaExibida();
            new AlertDialog.Builder(this)
                    .setTitle("Venda nº " + v.getId() + " criada")
                    .setMessage("Total: " + moeda.format(v.getValorTotal())
                            + "\nForma de pagamento: " + v.getFormaPagamento()
                            + "\nStatus: aguardando pagamento"
                            + "\n\nConfirme quando o cliente pagar.")
                    .setCancelable(false)
                    .setPositiveButton("Confirmar pagamento", (d, w) -> vm.confirmarPagamento(v.getId()))
                    .setNegativeButton("Depois", null)
                    .show();
        });

        vm.getHistorico().observe(this, texto -> {
            if (texto == null) return;
            vm.historicoExibido();
            new AlertDialog.Builder(this)
                    .setTitle("Histórico de vendas")
                    .setMessage(texto)
                    .setPositiveButton("Fechar", null)
                    .show();
        });
    }
}