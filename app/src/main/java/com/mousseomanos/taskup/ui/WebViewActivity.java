package com.mousseomanos.taskup.ui;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.mousseomanos.taskup.R;

public class WebViewActivity extends AppCompatActivity {

    // Link da WebView entregue pela outra squad (pesquisa do restaurante)
    private static final String URL_SQUAD =
            "https://pesquisa-restaurante-tp-e5hpfcctfmgfhmf6.brazilsouth-01.azurewebsites.net/";

    private WebView webView;
    private ProgressBar progresso;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_web_view);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets b = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(b.left, b.top, b.right, b.bottom);
            return insets;
        });

        webView = findViewById(R.id.webview);
        progresso = findViewById(R.id.progresso);

        // Necessário para páginas modernas (Angular, React...)
        WebSettings config = webView.getSettings();
        config.setJavaScriptEnabled(true);   // sem isso a página fica em branco
        config.setDomStorageEnabled(true);   // permite o localStorage da página

        // Abre tudo DENTRO do app (não no Chrome) e controla o carregamento
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progresso.setVisibility(View.GONE);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError erro) {
                if (req.isForMainFrame()) {
                    progresso.setVisibility(View.GONE);
                    Toast.makeText(WebViewActivity.this,
                            "Não foi possível carregar a pesquisa. Verifique a internet.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // Só carrega na primeira vez (se girar a tela, mantém a página)
        if (savedInstanceState == null) {
            webView.loadUrl(URL_SQUAD);
        } else {
            webView.restoreState(savedInstanceState);
        }

        findViewById(R.id.btnVoltarWeb).setOnClickListener(v -> finish());

        // Voltar do celular: volta as páginas da WebView; sem página, fecha a tela
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack();
                else finish();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState); // guarda o que o usuário já preencheu ao girar
    }
}