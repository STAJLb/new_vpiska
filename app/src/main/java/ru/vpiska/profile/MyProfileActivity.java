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



/**
 * Created by Кирилл on 10.11.2017.
 */

public class MyProfileActivity extends AppCompatActivity {

    private EditText txtFirstName, txtNikName;

    private ProgressDialog pDialog;
    private ImageView imgAvatar;
    private RadioGroup radioGroup;
    private TextView txtAge,txtBalance;


    private SessionManager session;
    private SQLiteHandler db;

    private String accessToken;

    private String firstName,nikName,sex,age;


    private static final String TAG = MyProfileActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_profile);

        txtFirstName = findViewById(R.id.first_name);
        txtNikName = findViewById(R.id.nik_name);


        imgAvatar = findViewById(R.id.imgAvatar);
        radioGroup = findViewById(R.id.radioGroup);
        txtAge = findViewById(R.id.txtAge);
        txtBalance = findViewById(R.id.txtBalance);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        Button btnUpdate = findViewById(R.id.btnUpdate);
        Button btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        Button btnInputAge = findViewById(R.id.inputAge);

        pDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);

        session = new SessionManager(getApplicationContext());

        getMyProfile();

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                firstName = txtFirstName.getText().toString().trim();
                nikName = txtNikName.getText().toString().trim();
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                sex = null;
                switch (checkedRadioButtonId){
                    case R.id.rbMan:
                        sex = "m";
                        break;
                    case R.id.rbWoman:
                        sex = "w";
                        break;
                }
                age = txtAge.getText().toString().trim();
                updateUser(firstName, nikName,sex,age);

            }
        });

        btnChangeAvatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MyProfileActivity.this, UploadImageActivity.class);
                startActivity(intent);
                finish();

            }
        });

        btnInputAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogFragment dateDialog = new DatePicker();
                dateDialog.show(getSupportFragmentManager(), "datePicker");

            }
        });

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

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(MyProfileActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(MyProfileActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();
    }

    private void getMyProfile() {
        pDialog.setMessage("Получение данных профиля ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_data_of_my_profile";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE , new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {
                        hideDialog();
                        JSONObject user = jObj.getJSONObject("user");
                        String firstName = user.getString("first_name");
                        String nikName = user.getString("nik_name");
                        String image = user.getString("image");
                        String age  = user.getString("age");
                        String sex  = user.getString("sex");
                        String balance  = user.getString("balance");

                        txtFirstName.setText(firstName);
                        txtNikName.setText(nikName);
                        txtAge.setText(age);
                        txtBalance.setText(balance);

                        switch (sex){
                            case "m":
                                radioGroup.check(R.id.rbMan);
                                break;
                            case "w":
                                radioGroup.check(R.id.rbWoman);
                                break;
                            default:
                                radioGroup.check(R.id.rbMan);
                                break;
                        }


                        Glide.with(getApplicationContext())
                                .load(image)
                                .placeholder(R.drawable.ic_profile)
                                .fitCenter()
                                .override(400, 400)
                                .dontAnimate()
                                .into(imgAvatar);


                    } else {
                        if(expAccessToken && Boolean.toString(expAccessToken) != null){
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
                Toast.makeText(getApplicationContext(), "Ошибка: " +
                        error.getMessage(), Toast.LENGTH_LONG).show();

            }

        })
        {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version", Integer.toString(BuildConfig.VERSION_CODE));
                return params;
            }};
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void updateUser(final String firstName, final String nikName,final String sex,final String age) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        pDialog.setMessage("Обновление ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_USER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response);
                hideDialog();

                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    if (!error) {

                        Toast.makeText(getApplicationContext(), "Данные обновлены.", Toast.LENGTH_LONG).show();

                        // Launch login activity
                        Intent intent = new Intent(MyProfileActivity.this, MainScreenActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        if(expAccessToken && Boolean.toString(expAccessToken) != null){
                            updateDataTokens(tag_string_req);
                        }else{
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
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
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("first_name", firstName);
                params.put("nik_name", nikName);
                params.put("sex", sex);
                params.put("age", age);
                params.put("access_token", accessToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));


                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }


    private void updateDataTokens(final String tag){
        HttpsTrustManager.allowAllSSL();

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

                        db.updateDataTokens(accessToken,expAccessToken,refreshToken,expRefreshToken);



                        switch (tag){
                            case "req_get_data_of_my_profile":
                                getMyProfile();
                                break;
                            case "req_update":
                                updateUser(firstName, nikName,sex,age);
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


    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }


}
