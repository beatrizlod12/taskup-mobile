package com.mousseomanos.taskup.data;

import com.mousseomanos.taskup.ui.ProdutoAdapter;
import com.mousseomanos.taskup.model.Produto;

import java.util.Collections;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProdutoRepository {

    private final ProdutoApi api = RetrofitClient.getApi();

    // "Resposta" que o Repository devolve para o ViewModel
    public interface Retorno<T> {
        void sucesso(T dados);
        void erro(String mensagem);
    }

    public void listar(String nome, boolean soEstoqueBaixo, Retorno<List<Produto>> r) {
        Call<List<Produto>> chamada = soEstoqueBaixo
                ? api.estoqueBaixo(ProdutoAdapter.LIMITE_ESTOQUE_BAIXO)
                : api.listar(nome);
        chamada.enqueue(callback(r));
    }

    public void salvar(Long id, Produto p, Retorno<Void> r) {
        if (id == null) api.criar(p).enqueue(callback(r));
        else api.atualizar(id, p).enqueue(callback(r));
    }

    public void ajustarEstoque(Long id, int quantidade, Retorno<Void> r) {
        api.ajustarEstoque(id, Collections.singletonMap("quantidade", quantidade)).enqueue(callback(r));
    }

    public void alternarAtivo(Produto p, Retorno<Void> r) {
        boolean ativo = p.getAtivo() == null || p.getAtivo();
        if (ativo) api.inativar(p.getId()).enqueue(callback(r));
        else api.ativar(p.getId()).enqueue(callback(r));
    }

    public void excluir(Long id, Retorno<Void> r) {
        api.deletar(id).enqueue(callback(r));
    }

    // Transforma a resposta do Retrofit em sucesso/erro
    private <T> Callback<T> callback(Retorno<T> r) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) r.sucesso(response.body());
                else r.erro("A API recusou a operação (código " + response.code() + ")");
            }
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                r.erro("Sem conexão com a API. Se ela estava hibernando, tente de novo em 30 s.");
            }
        };
    }
}