package ru.vpiska.auth;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import com.google.android.gms.ads.AdView;

import ru.vpiska.R;
import ru.vpiska.app.AdMobController;


/**
 * Created by Кирилл on 10.02.2018.
 */

public class AgreementActivity extends AppCompatActivity {
    private WebView mWebView;

    private AdView AdView;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        mWebView = (WebView) findViewById(R.id.webView);
        // включаем поддержку JavaScript

        // указываем страницу загрузки
        mWebView.loadUrl("https://clickcoffee.ru/api/agreement");


        //AdMobController.showBanner(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AgreementActivity.this, RegisterActivity.class);
        startActivity(intent);
        finish();
    }
}
