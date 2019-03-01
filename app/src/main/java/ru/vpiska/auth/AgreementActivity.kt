package ru.vpiska.auth

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView

import ru.vpiska.R


/**
 * Created by Кирилл on 10.02.2018.
 */

class AgreementActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agreement)

        val mWebView = findViewById<WebView>(R.id.webView)
        // включаем поддержку JavaScript

        // указываем страницу загрузки
        mWebView.loadUrl("https://clickcoffee.ru/api/agreement")


        //AdMobController.showBanner(this);
    }

    override fun onBackPressed() {
        val intent = Intent(this@AgreementActivity, RegisterActivity::class.java)
        startActivity(intent)
        finish()
    }
}
