package ru.vpiska.auth;

import android.app.ProgressDialog;
import android.content.Intent;

import android.os.Bundle;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();


    private EditText inputNikName,inputPassword;

    private ProgressDialog pDialog;
    private SessionManager session;
    private SQLiteHandler db;
    private static long back_pressed;


    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_login);



        inputNikName = findViewById(R.id.nik_name);
        inputPassword = findViewById(R.id.password);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnLinkToRegister = findViewById(R.id.btnLinkToRegisterScreen);

        // Progress dialog
        pDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);

        // SQLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // Session manager
        session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(LoginActivity.this, MainScreenActivity.class);
            startActivity(intent);
            finish();
        }

        // Login button Click Event
        btnLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                String nikName = inputNikName.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                // Check for empty data in the form
                if (!nikName.isEmpty() && !password.isEmpty()) {
                    if(CheckConnection.hasConnection(LoginActivity.this)) {
                        checkLogin(nikName, password);
                    }

                } else {
                    // Prompt user to enter credentials
                    Toast.makeText(getApplicationContext(),
                            "Проверьте правильность данных", Toast.LENGTH_LONG)
                            .show();
                }

            }

        });

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),
                        RegisterActivity.class);
                startActivity(i);
                finish();

            }
        });

    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), "Нажми еще раз для выхода!",
                    Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    private void checkLogin(final String nikName, final String password) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        String tag_string_req = "req_login";

        pDialog.setMessage("Идет авторизация  ...");
        showDialog();

        StringRequest strReq = new StringRequest(Method.POST, AppConfig.URL_LOGIN, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ошибка авторизации: " + response);


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        // user successfully logged in
                        // Create login session
                        session.setLogin(true);
                        JSONObject dataTokens = jObj.getJSONObject("data_tokens");
                        // Now store the user in SQLite
                        String accessToken = dataTokens.getString("access_token");
                        String refreshToken = dataTokens.getString("refresh_token");
                        String expAccessToken = dataTokens.getString("exp_access_token");
                        String expRefreshToken = dataTokens.getString("exp_refresh_token");
                        db.deleteDataTokensTable();
                        // Inserting row in users table

                        db.addDataTokens(accessToken,expAccessToken,refreshToken,expRefreshToken);



                        // Launch main activity
                        Intent intent = new Intent(LoginActivity.this, MainScreenActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),"Ошибка: " +
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                    hideDialog();
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
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("nik_name", nikName);
                params.put("password", password);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq);
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
