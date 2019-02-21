package ru.vpiska.party;


import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.os.Bundle;


import ru.vpiska.BuildConfig;
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.maps.DatePicker;
import ru.vpiska.helper.maps.TimePicker;
import ru.vpiska.map.MapsActivity;
import ru.vpiska.helper.SessionManager;

import java.util.HashMap;
import java.util.Map;

import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;

import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;


import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;


public class AddPartyActivity extends AppCompatActivity {
    private static final String TAG = AddPartyActivity.class.getSimpleName();


    private SessionManager session;

    private EditText inputTitleParty,inputDescriptionParty,inputCountPeople,inputAddressParty;

    private TextView txtDate,txtTime;

    private Switch inputAlcohol;

    private Spinner spinner;


    private ProgressDialog pDialog;


    private SQLiteHandler db;

    private String accessToken;


    private String titleParty,  descriptionParty,  addressParty, coordinatesParty,  countPeopleParty , alcoholParty, dateTimeParty,typeParty ;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_party);


        Button btnAddParty = findViewById(R.id.btnAddParty);
        Button btnInputDate = findViewById(R.id.inputDate);
        Button btnInputTime = findViewById(R.id.inputTime);

        inputTitleParty = findViewById(R.id.title_party);
        inputDescriptionParty = findViewById(R.id.description_party);
        inputCountPeople = findViewById(R.id.count_people);
        inputAddressParty = findViewById(R.id.address_party);

        txtDate = findViewById(R.id.txtDate);
        txtTime = findViewById(R.id.txtTime);

        spinner = findViewById(R.id.spinner);
        inputAlcohol = findViewById(R.id.alcohol);

        // Progress dialog
        pDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.types_parties, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        spinner.setAdapter(adapter);


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);



        // SqLite database handler
        db = new SQLiteHandler(getApplicationContext());

        // session manager
        session = new SessionManager(getApplicationContext());



        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("При создании квартирника, отмечайте конкретный адрес с указанием дома, этажа и квартиры. ")
                .setTitle("Информация")
                .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();



        btnAddParty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(CheckConnection.hasConnection(AddPartyActivity.this)) {
                    formatArrayDataToSend();
                }

            }
        });
        btnInputDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogFragment dateDialog = new DatePicker();
                dateDialog.show(getSupportFragmentManager(), "datePicker");


            }
        });
        btnInputTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogFragment dateDialog = new TimePicker();
                dateDialog.show(getSupportFragmentManager(), "timePicker");

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
        Intent intent = new Intent(AddPartyActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddPartyActivity.this, MapsActivity.class);
        startActivity(intent);
        finish();
    }

    private void formatArrayDataToSend(){
         titleParty = inputTitleParty.getText().toString().trim();
         descriptionParty = inputDescriptionParty.getText().toString().trim();
         addressParty = inputAddressParty.getText().toString().trim();
         coordinatesParty = getIntent().getExtras().getString("latitudeMarker") + " " + getIntent().getExtras().getString("longitudeMarker");
         countPeopleParty = inputCountPeople.getText().toString().trim();
         alcoholParty = ((inputAlcohol.isChecked()) ? "1" : "0" );
         dateTimeParty = txtDate.getText() + " " + txtTime.getText();
         typeParty = Integer.toString(spinner.getSelectedItemPosition());

        sendDataParty(titleParty,descriptionParty,addressParty,coordinatesParty,countPeopleParty,alcoholParty,dateTimeParty,typeParty);
    }


    private void sendDataParty(final String titleParty, final String descriptionParty,final  String addressParty,final String coordinates,final String countPeople, final String alcohol,final String dateTime,final String typeParty ) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_add_party";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        pDialog.setMessage("Идет отправка данных  ...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_ADD_PARTY, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ошибка авторизации: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {

                        // Launch main activity
                        Intent intent = new Intent(AddPartyActivity.this, MapsActivity.class);
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
                hideDialog();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("version",Integer.toString(BuildConfig.VERSION_CODE));
                params.put("access_token", accessToken);
                params.put("title_party", titleParty);
                params.put("description_party", descriptionParty);
                params.put("address_party", addressParty);
                params.put("coordinates", coordinates);
                params.put("count_people", countPeople);
                params.put("alcohol", alcohol);
                params.put("date_time", dateTime);
                params.put("type_party", typeParty);

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
                            case "req_add_party":
                                sendDataParty(titleParty,descriptionParty,addressParty,coordinatesParty,countPeopleParty,alcoholParty,dateTimeParty,typeParty);
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
