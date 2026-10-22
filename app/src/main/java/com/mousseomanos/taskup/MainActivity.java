package com.mousseomanos.taskup;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements ProdutoAdapter.Acoes {

    private EditText edtNome, edtPreco, edtQuantidade, edtBusca;
    private Button btnSalvar, btnCancelar;
    private CheckBox chkBaixo;
    private TextView txtStatus, txtTituloForm;

    private ProdutoAdapter adapter;
    private ProdutoApi api;
    private Long editandoId = null; // null = cadastrando; com valor = editando

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

        edtNome = findViewById(R.id.edtNome);
        edtPreco = findViewById(R.id.edtPreco);
        edtQuantidade = findViewById(R.id.edtQuantidade);
        edtBusca = findViewById(R.id.edtBusca);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnCancelar = findViewById(R.id.btnCancelar);
        chkBaixo = findViewById(R.id.chkBaixo);
        txtStatus = findViewById(R.id.txtStatus);
        txtTituloForm = findViewById(R.id.txtTituloForm);
        Button btnBuscar = findViewById(R.id.btnBuscar);
        Button btnVendas = findViewById(R.id.btnVendas);

        api = RetrofitClient.getApi();
        adapter = new ProdutoAdapter(this);
        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        btnSalvar.setOnClickListener(v -> salvar());
        btnCancelar.setOnClickListener(v -> limparFormulario());
        btnBuscar.setOnClickListener(v -> carregar());
        chkBaixo.setOnCheckedChangeListener((b, marcado) -> carregar());

        // Abre a tela de vendas
        btnVendas.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, VendasActivity.class)));
    }

    // Roda sempre que a tela aparece: ao abrir o app e ao voltar da tela de vendas
    @Override
    protected void onResume() {
        super.onResume();
        carregar();
    }

    // GET /produtos, /produtos?nome=... ou /produtos/estoque-baixo
    private void carregar() {
        txtStatus.setText("Carregando...");
        txtStatus.setVisibility(View.VISIBLE);

        Call<List<Produto>> chamada;
        if (chkBaixo.isChecked()) {
            chamada = api.estoqueBaixo(ProdutoAdapter.LIMITE_ESTOQUE_BAIXO);
        } else {
            String termo = edtBusca.getText().toString().trim();
            chamada = api.listar(termo.isEmpty() ? null : termo);
        }

        chamada.enqueue(new Callback<List<Produto>>() {
            @Override
            public void onResponse(Call<List<Produto>> call, Response<List<Produto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    adapter.setProdutos(response.body());
                    if (response.body().isEmpty()) {
                        txtStatus.setText("Nenhum produto encontrado.");
                    } else {
                        txtStatus.setVisibility(View.GONE);
                    }
                } else {
                    txtStatus.setText("Erro da API: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Produto>> call, Throwable t) {
                txtStatus.setText("Sem conexão com a API. Se ela estava hibernando, tente de novo em 30 s.");
            }
        });
    }

    // POST (cadastrar) ou PUT (editar)
    private void salvar() {
        String nome = edtNome.getText().toString().trim();
        String precoTxt = edtPreco.getText().toString().trim().replace(",", ".");
        String qtdTxt = edtQuantidade.getText().toString().trim();

        if (nome.isEmpty() || precoTxt.isEmpty() || qtdTxt.isEmpty()) {
            toast("Preencha nome, preço e quantidade.");
            return;
        }

        Produto p;
        try {
            p = new Produto(nome, Double.parseDouble(precoTxt), Integer.parseInt(qtdTxt));
        } catch (NumberFormatException e) {
            toast("Preço ou quantidade inválidos.");
            return;
        }

        if (editandoId == null) {
            api.criar(p).enqueue(retorno("Produto cadastrado!", true));
        } else {
            api.atualizar(editandoId, p).enqueue(retorno("Produto atualizado!", true));
        }
    }

    private void limparFormulario() {
        editandoId = null;
        edtNome.setText("");
        edtPreco.setText("");
        edtQuantidade.setText("");
        txtTituloForm.setText("Novo produto");
        btnSalvar.setText("Cadastrar");
        btnCancelar.setVisibility(View.GONE);
    }

    // ---- Ações vindas dos botões de cada cartão ----

    @Override
    public void onEditar(Produto p) {
        editandoId = p.getId();
        edtNome.setText(p.getNome());
        edtPreco.setText(String.valueOf(p.getPreco()));
        edtQuantidade.setText(String.valueOf(p.getQuantidade()));
        txtTituloForm.setText("Editando: " + p.getNome());
        btnSalvar.setText("Salvar alterações");
        btnCancelar.setVisibility(View.VISIBLE);
        edtNome.requestFocus();
    }

    // PATCH /produtos/{id}/estoque  { "quantidade": 1 ou -1 }
    @Override
    public void onAjustarEstoque(Produto p, int quantidade) {
        if (quantidade < 0 && (p.getQuantidade() == null || p.getQuantidade() <= 0)) {
            toast("O estoque não pode ficar negativo.");
            return;
        }
        api.ajustarEstoque(p.getId(), Collections.singletonMap("quantidade", quantidade))
                .enqueue(retorno(quantidade > 0 ? "Entrada registrada" : "Saída registrada", false));
    }

    // PATCH /produtos/{id}/inativar ou /ativar
    @Override
    public void onAlternarAtivo(Produto p) {
        boolean ativo = p.getAtivo() == null || p.getAtivo();
        if (ativo) {
            api.inativar(p.getId()).enqueue(retorno("Produto inativado", false));
        } else {
            api.ativar(p.getId()).enqueue(retorno("Produto reativado", false));
        }
    }

    // DELETE /produtos/{id}, com confirmação
    @Override
    public void onExcluir(Produto p) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir produto")
                .setMessage("Excluir \"" + p.getNome() + "\"?")
                .setPositiveButton("Excluir", (d, w) ->
                        api.deletar(p.getId()).enqueue(retorno("Produto excluído", false)))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Callback usado por todas as operações que alteram dados
    private Callback<Void> retorno(String mensagem, boolean limparForm) {
        return new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    toast(mensagem);
                    if (limparForm) limparFormulario();
                    carregar();
                } else {
                    toast("A API recusou a operação (código " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                toast("Sem conexão com a API.");
            }
        };
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}