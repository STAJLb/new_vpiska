package ru.vpiska.profile;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.vpiska.BuildConfig;
import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.DatePicker;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.map.MapsActivity;
import ru.vpiska.rating.RatingActivity;
import ru.vpiska.shop.ShopActivity;


/**
 * Created by Кирилл on 10.11.2017.
 */

public class GuestProfileActivity extends AppCompatActivity {



    private ProgressDialog pDialog;



    private SessionManager session;
    private SQLiteHandler db;





    private static final String TAG = GuestProfileActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_guest);


        session = new SessionManager(getApplicationContext());
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> dataTokens = db.getDataTokens();

        final String refreshToken = dataTokens.get("refresh_token");
        ImageView imgAvatar = findViewById(R.id.imgAvatar);
        Button btnMap = findViewById(R.id.btnMap);
        Button btnRating = findViewById(R.id.btnRating);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        Glide.with(getApplicationContext())
                .load("http://lumpics.ru/wp-content/uploads/2017/11/Programmyi-dlya-sozdaniya-avatarok.png")
                .placeholder(R.drawable.ic_profile)
                .fitCenter()
                .override(400, 400)
                .dontAnimate()
                .into(imgAvatar);

        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(GuestProfileActivity.this, RatingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(GuestProfileActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });


        AdMobController.showBanner(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_guest, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_exit) {
            logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        session.setKeyIsGuest(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(GuestProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GuestProfileActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }


}
