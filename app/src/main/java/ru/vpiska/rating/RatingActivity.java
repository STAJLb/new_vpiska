package ru.vpiska.rating;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
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
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.profile.GuestProfileActivity;


public class RatingActivity extends AppCompatActivity {

    private static final String TAG = RatingActivity.class.getSimpleName();

    private SessionManager session;
    private SQLiteHandler db;

    private String accessToken;
    @SuppressWarnings("unchecked")
    private final ArrayList<Rating> ratings = new ArrayList();

    private ListView ratingList;
    private ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        db = new SQLiteHandler(getApplicationContext());
        HashMap<String, String> dataTokens = db.getDataTokens();

        session = new SessionManager(getApplicationContext());

        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(true);
        getDataOfRating();


        AdMobController.showBanner(this);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {
            AppController.getInstance().showDialogAboutUs(this);
        }
        if (id == R.id.action_exit) {
            logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(session.isGuest()){
            Intent intent = new Intent(RatingActivity.this, GuestProfileActivity.class);
            startActivity(intent);
            finish();
        }else{
            Intent intent = new Intent(RatingActivity.this, MainScreenActivity.class);
            startActivity(intent);
            finish();
        }

    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(RatingActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void getDataOfRating() {
        pDialog.setMessage("Получение данных рейтинга ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_data_of_rating";


        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_DATA_OF_RATING, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.d(TAG, "Данные таблицы рейтинга " + response);


                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    ratingList = findViewById(R.id.lvRating);
                    RatingAdapter adapter = new RatingAdapter(getApplicationContext(), R.layout.list_item_rating, ratings);
                    if (!error) {
                        hideDialog();
                        JSONArray arr = jObj.getJSONArray("rating");
                        for(int i = 0;i<arr.length();i++){
                            ratings.add(new Rating( arr.getJSONObject(i).getString("first_name"),arr.getJSONObject(i).getString("rating"),
                                    arr.getJSONObject(i).getString("id")));
                        }
                        ratingList.setAdapter(adapter);


                    } else {
                        hideDialog();
                        if(expAccessToken){
                            updateDataTokens();
                        }else{
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }

                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                    hideDialog();
                    Toast.makeText(getApplicationContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }

            }
        }){
            @Override
        public Map<String, String> getHeaders() {
            Map<String, String> params = new HashMap<>();
            params.put("Content-Type", "application/json; charset=UTF-8");
            params.put("Access-Token", accessToken);
            params.put("Version", Integer.toString(BuildConfig.VERSION_CODE));

            return params;
        }};

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void updateDataTokens(){
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

                        switch ("req_get_data_of_rating"){
                            case "req_get_data_of_rating":
                                getDataOfRating();
                            break;
                        }



                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), "Ошибка: " +
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
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                assert refreshToken != null;
                params.put("refresh_token", refreshToken);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq,tag_string_req);
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
