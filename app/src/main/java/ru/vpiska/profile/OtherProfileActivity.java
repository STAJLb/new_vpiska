package ru.vpiska.profile;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
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
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.party.PartyActivity;
import ru.vpiska.rating.RatingActivity;


public class OtherProfileActivity extends AppCompatActivity {

    private TextView txtFirstName, txtNikName, txtRating;

    private ImageView imgAvatar;

    private static final String TAG = OtherProfileActivity.class.getSimpleName();

    private SessionManager session;
    private SQLiteHandler db;

    private ProgressDialog pDialog;

    private String accessToken;

    private String idUser,idParty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other_profile);

        txtFirstName = findViewById(R.id.first_name);
        txtNikName = findViewById(R.id.nik_name);
        txtRating = findViewById(R.id.rating);
        imgAvatar = findViewById(R.id.imgAvatar);

        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);

        db = new SQLiteHandler(getApplicationContext());
        session = new SessionManager(getApplicationContext());
        if (getIntent().getExtras().getString("id_party") != null) {
            idParty = getIntent().getExtras().getString("id_party");
        }
        idUser = getIntent().getExtras().getString("id_user");

        getDataOfOtherParty(Integer.parseInt(idUser));


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

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
        if (getIntent().getExtras().getString("id_party") != null) {
            Intent intent = new Intent(OtherProfileActivity.this, PartyActivity.class);
            intent.putExtra("id_party", getIntent().getExtras().getString("id_party"));
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(OtherProfileActivity.this, RatingActivity.class);

            startActivity(intent);
            finish();
        }


    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(OtherProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void getDataOfOtherParty(final int idUser) {
        pDialog.setMessage("Получение данных профиля ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();

        final String tag_string_req = "req_get_created_party_user";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ошибка получения createdUserParty: " + response);
                try {
                    hideDialog();

                    JSONObject jObj = new JSONObject(response);
                    JSONObject user = jObj.getJSONObject("user");
                    String firstName = user.getString("first_name");
                    String nikName = user.getString("nik_name");
                    String rating = user.getString("rating");
                    String image = user.getString("image");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        txtFirstName.setText(firstName);
                        txtNikName.setText(nikName);
                        txtRating.setText(rating);


                        Glide.with(OtherProfileActivity.this)
                                .load(image)
                                .fitCenter()
                                .placeholder(R.drawable.ic_profile)
                                .override(400, 400)
                                .dontAnimate()
                                .into(imgAvatar);


                    } else {
                        if (expAccessToken && Boolean.toString(expAccessToken) != null) {
                            updateDataTokens(tag_string_req);
                        } else {
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
                hideDialog();
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version", Integer.toString(BuildConfig.VERSION_CODE));
                params.put("Uid", Integer.toString(idUser));
                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void updateDataTokens(final String tag) {
        HttpsTrustManager.allowAllSSL();
        Log.e(TAG, "Обновляем токен");
        String tag_string_req = "req_update_data_tokens";
        db = new SQLiteHandler(getApplicationContext());
        HashMap<String, String> dataTokens = db.getDataTokens();

        final String refreshToken = dataTokens.get("refresh_token");

        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, new Response.Listener<String>() {

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
                        db.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken);

                        Log.e(TAG, "Получили новый токен:" + accessToken);

                        switch (tag) {
                            case "req_get_data_of_my_profile":
                                getDataOfOtherParty(Integer.parseInt(idUser));
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
                Log.e(TAG, "Login Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(), "Ошибка: " +
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
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
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