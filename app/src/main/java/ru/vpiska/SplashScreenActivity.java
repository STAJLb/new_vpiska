package ru.vpiska;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import ru.vpiska.auth.LoginActivity;

public class SplashScreenActivity extends AppCompatActivity {

    private TextView txtVersionCode;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        String versionName = BuildConfig.VERSION_NAME;

        txtVersionCode = (TextView) findViewById(R.id.txtVersionName);

        txtVersionCode.setText(versionName);


        //Создаем новый поток:
        Thread splash_time = new Thread()
        {
            public void run()
            {
                try
                {
                    //Целое значение время отображения картинки:
                    int SplashTimer = 0;

                    while(SplashTimer < 800) {
                        sleep(100);
                        SplashTimer = SplashTimer +50;
                    };
                }
                catch (InterruptedException e) {
                    e.printStackTrace(); }
                finally {
                    startActivity(new Intent(SplashScreenActivity.this,LoginActivity.class));
                    //Закрываем activity:
                    finish();
                }
            }
        };
        splash_time.start();
    }
}
