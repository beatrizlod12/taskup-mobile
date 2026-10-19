package com.mousseomanos.taskup;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProdutoApi {

    @GET("produtos")
    Call<List<Produto>> listar(@Query("nome") String nome);

    @GET("produtos/estoque-baixo")
    Call<List<Produto>> estoqueBaixo(@Query("limite") int limite);

    @POST("produtos")
    Call<Void> criar(@Body Produto produto);

    @PUT("produtos/{id}")
    Call<Void> atualizar(@Path("id") Long id, @Body Produto produto);

    @PATCH("produtos/{id}/estoque")
    Call<Void> ajustarEstoque(@Path("id") Long id, @Body Map<String, Integer> corpo);

    @PATCH("produtos/{id}/inativar")
    Call<Void> inativar(@Path("id") Long id);

    @PATCH("produtos/{id}/ativar")
    Call<Void> ativar(@Path("id") Long id);

    @DELETE("produtos/{id}")
    Call<Void> deletar(@Path("id") Long id);
}