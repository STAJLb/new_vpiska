package ru.vpiska.party;

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

import ru.vpiska.R;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.map.MapsActivity;

public class AddReportActivity extends AppCompatActivity {

    private static final String TAG = AddReportActivity.class.getSimpleName();

    private Spinner spinner;

    private TextView txtDescriptionReport;

    private String accessToken;

    private SQLiteHandler db;

    private  String descriptionReport,  reason,  partyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_report);

        spinner = findViewById(R.id.spinner);
        Button btnAddReport = findViewById(R.id.btnAddReport);

        db = new SQLiteHandler(getApplicationContext());


        btnAddReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                partyId = getIntent().getExtras().getString("party_id");

                txtDescriptionReport = findViewById(R.id.description_report);

                descriptionReport = txtDescriptionReport.getText().toString();
                reason = spinner.getSelectedItem().toString();
                addReport(descriptionReport,reason,partyId);
            }
        });
        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.reasons_report, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinner.setAdapter(adapter);

        MobileAds.initialize(this, "ca-app-pub-6595506155906957/5953597740");

        com.google.android.gms.ads.AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
    }

    private void addReport(final String descriptionReport,final String reason,final String partyId) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        String tag_string_req = "req_add_report";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");


        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_ADD_REPORT, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ошибка авторизации: " + response);


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(),"Репорт добавлен.", Toast.LENGTH_LONG).show();
                        // Launch main activity
                        Intent intent = new Intent(AddReportActivity.this, MapsActivity.class);
                        startActivity(intent);
                        finish();
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
                params.put("access_token", accessToken);
                params.put("description_report", descriptionReport);
                params.put("reason", reason);
                params.put("party_id", partyId);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }
}
