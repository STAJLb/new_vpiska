package ru.vpiska

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import ru.vpiska.auth.LoginActivity

class SplashScreenActivity : AppCompatActivity() {

    private lateinit var txtVersionCode: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        val versionName = BuildConfig.VERSION_NAME

        txtVersionCode = findViewById(R.id.txtVersionName)

        txtVersionCode.text = versionName


        //Создаем новый поток:
        val splash_time = object : Thread() {
            override fun run() {
                try {
                    //Целое значение время отображения картинки:
                    var splashTimer = 0

                    while (splashTimer < 800) {
                        Thread.sleep(100)
                        splashTimer = splashTimer + 50
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                } finally {
                    startActivity(Intent(this@SplashScreenActivity, LoginActivity::class.java))
                    //Закрываем activity:
                    finish()
                }
            }
        }
        splash_time.start()
    }
}
