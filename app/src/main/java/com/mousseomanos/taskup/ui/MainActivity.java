package com.mousseomanos.taskup.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import com.mousseomanos.taskup.model.Produto;
import com.mousseomanos.taskup.viewmodel.ProdutoViewModel;

public class MainActivity extends AppCompatActivity implements ProdutoAdapter.Acoes {

    private EditText edtNome, edtPreco, edtQuantidade, edtBusca;
    private Button btnSalvar, btnCancelar;
    private CheckBox chkBaixo;
    private TextView txtStatus, txtTituloForm;

    private ProdutoAdapter adapter;
    private ProdutoViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left + 32, bars.top + 32, bars.right + 32, bars.bottom);
            return insets;
        });

        // ---- Componentes da tela ----
        edtNome = findViewById(R.id.edtNome);
        edtPreco = findViewById(R.id.edtPreco);
        edtQuantidade = findViewById(R.id.edtQuantidade);
        edtBusca = findViewById(R.id.edtBusca);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        chkBaixo = findViewById(R.id.chkBaixo);
        txtStatus = findViewById(R.id.txtStatus);
        txtTituloForm = findViewById(R.id.txtTituloForm);

        adapter = new ProdutoAdapter(this);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        // ---- ViewModel ----
        vm = new ViewModelProvider(this).get(ProdutoViewModel.class);
        observar();

        // ---- Cliques: a tela só repassa para o ViewModel ----
        btnSalvar.setOnClickListener(v -> vm.salvar(
                edtNome.getText().toString(),
                edtPreco.getText().toString(),
                edtQuantidade.getText().toString()));
        btnCancelar.setOnClickListener(v -> vm.cancelarEdicao());
        findViewById(R.id.btnBuscar).setOnClickListener(v -> carregar());
        chkBaixo.setOnCheckedChangeListener((b, marcado) -> carregar());

        findViewById(R.id.btnVendas).setOnClickListener(v ->
                startActivity(new Intent(this, VendasActivity.class)));
        findViewById(R.id.btnWebView).setOnClickListener(v ->
                startActivity(new Intent(this, WebViewActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregar(); // atualiza ao abrir e ao voltar da tela de vendas
    }

    private void carregar() {
        vm.carregar(edtBusca.getText().toString(), chkBaixo.isChecked());
    }

    // A tela "observa" o ViewModel: quando um dado muda, a tela se atualiza sozinha
    private void observar() {
        vm.getProdutos().observe(this, lista -> adapter.setProdutos(lista));

        vm.getStatus().observe(this, texto -> {
            txtStatus.setVisibility(texto == null ? View.GONE : View.VISIBLE);
            if (texto != null) txtStatus.setText(texto);
        });

        vm.getMensagem().observe(this, msg -> {
            if (msg == null) return;
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            vm.mensagemExibida();
        });

        vm.getEditando().observe(this, p -> {
            boolean editando = p != null;
            txtTituloForm.setText(editando ? "Editando: " + p.getNome() : "Novo produto");
            btnSalvar.setText(editando ? "Salvar alterações" : "Cadastrar");
            btnCancelar.setVisibility(editando ? View.VISIBLE : View.GONE);
        });

        vm.getLimparForm().observe(this, limpar -> {
            if (!limpar) return;
            edtNome.setText("");
            edtPreco.setText("");
            edtQuantidade.setText("");
            vm.formularioLimpo();
        });
    }

    // ---- Botões de cada cartão: só repassam para o ViewModel ----
    @Override
    public void onEditar(Produto p) {
        vm.editar(p);
        edtNome.setText(p.getNome());
        edtPreco.setText(String.valueOf(p.getPreco()));
        edtQuantidade.setText(String.valueOf(p.getQuantidade()));
        edtNome.requestFocus();
    }

    @Override
    public void onAjustarEstoque(Produto p, int quantidade) { vm.ajustarEstoque(p, quantidade); }

    @Override
    public void onAlternarAtivo(Produto p) { vm.alternarAtivo(p); }

    @Override
    public void onExcluir(Produto p) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir produto")
                .setMessage("Excluir \"" + p.getNome() + "\"?")
                .setPositiveButton("Excluir", (d, w) -> vm.excluir(p))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}