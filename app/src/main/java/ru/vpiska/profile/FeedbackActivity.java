package ru.vpiska.profile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.vpiska.BuildConfig;
import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.map.MapsActivity;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = FeedbackActivity.class.getSimpleName();


    private TextView txtFeedback;

    private String accessToken;

    private SQLiteHandler db;

    private  String feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);


        Button btnAddReport = findViewById(R.id.btnSendFeedBack);

        db = new SQLiteHandler(getApplicationContext());


        btnAddReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                txtFeedback = findViewById(R.id.feeadback);

                feedback = txtFeedback.getText().toString();

                sendFeedback(feedback);
            }
        });

        MobileAds.initialize(this, "ca-app-pub-6595506155906957/5953597740");

        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void sendFeedback(final String feedback) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final   String tag_string_req = "req_send_feedback";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");


        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_ADD_FEEDBACK, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {



                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(),"Отзыв добавлен.", Toast.LENGTH_LONG).show();
                        // Launch main activity
                        Intent intent = new Intent(FeedbackActivity.this, MainScreenActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        if(expAccessToken){
                            updateDataTokens(tag_string_req);
                        }else{
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),"Ошибка: " +
                        error.getMessage(), Toast.LENGTH_LONG).show();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("access_token", accessToken);
                params.put("version",Integer.toString(BuildConfig.VERSION_CODE));
                params.put("feedback", feedback);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }



    private void updateDataTokens(final String tag){
        HttpsTrustManager.allowAllSSL();
        Log.e(TAG, "Обновляем токен");
        String tag_string_req = "req_update_data_tokens";
        db = new SQLiteHandler(getApplicationContext());
        HashMap<String, String> dataTokens = db.getDataTokens();

        final String refreshToken = dataTokens.get("refresh_token");

        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, new Response.Listener<String>(){

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ошибка авторизации: " + response);


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        JSONObject dataTokens = jObj.getJSONObject("data_tokens");
                        String accessToken = dataTokens.getString("access_token");
                        String refreshToken = dataTokens.getString("refresh_token");
                        String expAccessToken = dataTokens.getString("exp_access_token");
                        String expRefreshToken = dataTokens.getString("exp_refresh_token");
                        Log.e(TAG, "Обновили таблицу");
                        db.updateDataTokens(accessToken,expAccessToken,refreshToken,expRefreshToken);

                        Log.e(TAG, "Получили новый токен:" + accessToken);

                        switch (tag){
                            case "req_send_feedback":
                                sendFeedback(feedback);
                                break;

                        }
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),"Ошибка: " +
                                errorMsg, Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),"Ошибка: " +
                        error.getMessage(), Toast.LENGTH_LONG).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("refresh_token", refreshToken);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq,tag_string_req);
    }
}
