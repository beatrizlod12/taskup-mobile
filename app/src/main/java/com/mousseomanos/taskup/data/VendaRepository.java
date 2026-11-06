package com.mousseomanos.taskup.data;

import com.mousseomanos.taskup.model.Produto;
import com.mousseomanos.taskup.model.Venda;
import com.mousseomanos.taskup.model.VendaRequest;

import org.json.JSONObject;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VendaRepository {

    private final ProdutoApi api = RetrofitClient.getApi();

    public interface Retorno<T> {
        void sucesso(T dados);
        void erro(String mensagem);
    }

    public void listarProdutos(Retorno<List<Produto>> r) {
        api.listar(null).enqueue(callback(r));
    }

    public void criarVenda(VendaRequest req, Retorno<Venda> r) {
        api.criarVenda(req).enqueue(callback(r));
    }

    public void confirmarPagamento(Long id, Retorno<Venda> r) {
        api.confirmarPagamento(id).enqueue(callback(r));
    }

    public void listarVendas(Retorno<List<Venda>> r) {
        api.listarVendas().enqueue(callback(r));
    }

    private <T> Callback<T> callback(Retorno<T> r) {
        return new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful()) r.sucesso(response.body());
                else r.erro(lerErro(response));
            }
            @Override
            public void onFailure(Call<T> call, Throwable t) {
                r.erro("Sem conexão com o servidor. Verifique a internet.");
            }
        };
    }

    // Lê a mensagem que a API mandou (ex.: "Estoque insuficiente de Pudim")
    private String lerErro(Response<?> r) {
        try {
            return new JSONObject(r.errorBody().string()).optString("message", "Erro " + r.code());
        } catch (Exception e) {
            return "Erro " + r.code();
        }
    }
}