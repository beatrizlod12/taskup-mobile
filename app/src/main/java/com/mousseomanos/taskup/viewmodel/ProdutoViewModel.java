package com.mousseomanos.taskup.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mousseomanos.taskup.data.ProdutoRepository;
import com.mousseomanos.taskup.model.Produto;

import java.util.ArrayList;
import java.util.List;

public class ProdutoViewModel extends ViewModel {

    private final ProdutoRepository repo = new ProdutoRepository();

    // Dados que a tela observa
    private final MutableLiveData<List<Produto>> produtos = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> status = new MutableLiveData<>("Carregando...");
    private final MutableLiveData<String> mensagem = new MutableLiveData<>();
    private final MutableLiveData<Boolean> limparForm = new MutableLiveData<>(false);
    private final MutableLiveData<Produto> editando = new MutableLiveData<>(null);

    // Última busca, para recarregar depois de cada alteração
    private String ultimaBusca = null;
    private boolean ultimoSoBaixo = false;

    // ---- O que a tela pode observar (só leitura) ----
    public LiveData<List<Produto>> getProdutos() { return produtos; }
    public LiveData<String> getStatus() { return status; }
    public LiveData<String> getMensagem() { return mensagem; }
    public LiveData<Boolean> getLimparForm() { return limparForm; }
    public LiveData<Produto> getEditando() { return editando; }

    // ---- Ações que a tela chama ----
    public void carregar(String busca, boolean soBaixo) {
        ultimaBusca = (busca == null || busca.trim().isEmpty()) ? null : busca.trim();
        ultimoSoBaixo = soBaixo;
        status.setValue("Carregando...");

        repo.listar(ultimaBusca, soBaixo, new ProdutoRepository.Retorno<List<Produto>>() {
            @Override public void sucesso(List<Produto> lista) {
                List<Produto> l = lista == null ? new ArrayList<>() : lista;
                produtos.setValue(l);
                status.setValue(l.isEmpty() ? "Nenhum produto encontrado." : null); // null = esconder
            }
            @Override public void erro(String msg) { status.setValue(msg); }
        });
    }

    public void salvar(String nome, String precoTxt, String qtdTxt) {
        nome = nome.trim();
        precoTxt = precoTxt.trim().replace(",", ".");
        qtdTxt = qtdTxt.trim();

        if (nome.isEmpty() || precoTxt.isEmpty() || qtdTxt.isEmpty()) {
            mensagem.setValue("Preencha nome, preço e quantidade.");
            return;
        }

        Produto p;
        try {
            p = new Produto(nome, Double.parseDouble(precoTxt), Integer.parseInt(qtdTxt));
        } catch (NumberFormatException e) {
            mensagem.setValue("Preço ou quantidade inválidos.");
            return;
        }

        Produto emEdicao = editando.getValue();
        Long id = emEdicao == null ? null : emEdicao.getId();
        String ok = id == null ? "Produto cadastrado!" : "Produto atualizado!";
        repo.salvar(id, p, retorno(ok, true));
    }

    public void editar(Produto p) { editando.setValue(p); }

    public void cancelarEdicao() {
        editando.setValue(null);
        limparForm.setValue(true);
    }

    public void ajustarEstoque(Produto p, int quantidade) {
        if (quantidade < 0 && (p.getQuantidade() == null || p.getQuantidade() <= 0)) {
            mensagem.setValue("O estoque não pode ficar negativo.");
            return;
        }
        repo.ajustarEstoque(p.getId(), quantidade,
                retorno(quantidade > 0 ? "Entrada registrada" : "Saída registrada", false));
    }

    public void alternarAtivo(Produto p) {
        boolean ativo = p.getAtivo() == null || p.getAtivo();
        repo.alternarAtivo(p, retorno(ativo ? "Produto inativado" : "Produto reativado", false));
    }

    public void excluir(Produto p) {
        repo.excluir(p.getId(), retorno("Produto excluído", false));
    }

    // A tela avisa que já mostrou a mensagem / já limpou o formulário
    public void mensagemExibida() { mensagem.setValue(null); }
    public void formularioLimpo() { limparForm.setValue(false); }

    // Resposta padrão das operações que alteram dados
    private ProdutoRepository.Retorno<Void> retorno(String ok, boolean limpar) {
        return new ProdutoRepository.Retorno<Void>() {
            @Override public void sucesso(Void v) {
                mensagem.setValue(ok);
                if (limpar) { editando.setValue(null); limparForm.setValue(true); }
                carregar(ultimaBusca, ultimoSoBaixo);
            }
            @Override public void erro(String msg) { mensagem.setValue(msg); }
        };
    }
}