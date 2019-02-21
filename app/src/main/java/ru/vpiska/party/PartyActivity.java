package ru.vpiska.party;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;






import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.vpiska.BuildConfig;
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.map.MapsActivity;
import ru.vpiska.profile.OtherProfileActivity;
import ru.vpiska.shop.ShopActivity;


public class PartyActivity extends AppCompatActivity {

    private static final String TAG = PartyActivity.class.getSimpleName();

    private TextView txtTitleParty,txtDescriptionParty, txtNikNameCreatedParty,txtAddressParty,txtDateTimeParty,txtRatingParty,txtSource;
    private Button cbAcceptParty;

    private ListView listView;


    private SessionManager session;

    private SQLiteHandler db;

    private int idParty;

    private String idCreatedParty;

    private String addressParty;

    private String message;

    private InterstitialAd mInterstitialAd;

    private static final String NAME_USER = "name_user"; // Верхний текст
    private static final String TIME = "TIME"; // ниже главного
    private static final String MESSAGE = "message";  // будущая картинка



    private ProgressDialog pDialog;

    private String accessToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activty_party);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        TabHost tabHost = findViewById(android.R.id.tabhost);
        // инициализация
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setIndicator("Главная");
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setIndicator("Участники");
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag3");
        tabSpec.setIndicator("Отзывы");
        tabSpec.setContent(R.id.tab3);
        tabHost.addTab(tabSpec);


        final TabWidget tw = tabHost.findViewById(android.R.id.tabs);
        for (int i = 0; i < tw.getChildCount(); ++i)
        {
            final View tabView = tw.getChildTabViewAt(i);
            final TextView tv = tabView.findViewById(android.R.id.title);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
        }

        tabHost.setCurrentTabByTag("tag1");

        txtTitleParty = findViewById(R.id.title_party);
        txtDescriptionParty = findViewById(R.id.description_party);
        txtNikNameCreatedParty = findViewById(R.id.created_party_nik_name);
        txtAddressParty = findViewById(R.id.address_party);
        txtDateTimeParty = findViewById(R.id.date_time);
        txtRatingParty = findViewById(R.id.rating);
        txtSource = findViewById(R.id.source);

        cbAcceptParty = findViewById(R.id.accept_party);
        listView = findViewById(R.id.listView);

        txtNikNameCreatedParty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PartyActivity.this, OtherProfileActivity.class);
                intent.putExtra("id_user", idCreatedParty);
                intent.putExtra("id_party", String.valueOf(idParty));
                startActivity(intent);
                finish();
            }
        });

        db = new SQLiteHandler(getApplicationContext());
        session = new SessionManager(getApplicationContext());

        idParty = Integer.parseInt(getIntent().getExtras().getString("id_party"));

        getDataOfParty(idParty);
        getReviewsOfParty(idParty);



        cbAcceptParty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                     txtAddressParty.setText("");
                    if (mInterstitialAd.isLoaded()) {
                        mInterstitialAd.show();
                    } else {
                        Log.d("TAG", "The interstitial wasn't loaded yet.");
                    }
            }
        });

        mInterstitialAd = new InterstitialAd(PartyActivity.this);
        mInterstitialAd.setAdUnitId(getString(R.string.banner_ad_unit_id_1));
        AdRequest adRequest = new AdRequest.Builder().build();

        mInterstitialAd.loadAd(adRequest);

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.

                mInterstitialAd.loadAd(new AdRequest.Builder().build());

                if(CheckConnection.hasConnection(PartyActivity.this)) {
                        addUserToParty(Integer.toString(idParty));

                }

            }
            @Override
            public void onAdLoaded(){

            }

        });

        FloatingActionButton fab =
                findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText input = findViewById(R.id.input);
                message = input.getText().toString();
                addReviewToParty(Integer.toString(idParty),message);

                // Clear the input
                input.setText("");
            }
        });


        AdMobController.showBanner(this);


    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_party, menu);
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
        if (id == R.id.action_create_report) {
            Intent intent = new Intent(PartyActivity.this, AddReportActivity.class);
            intent.putExtra("party_id",Integer.toString(idParty));
            startActivity(intent);
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(PartyActivity.this, MapsActivity.class);
        startActivity(intent);
        finish();
    }



    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(PartyActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }



    private void addReviewToParty(final String partyId,final String message ) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_add_review_to_party";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_ADD_REVIEW_TO_PARTY , new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Добавление отзыва " + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {

                        getReviewsOfParty(idParty);
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
                params.put("id_party", partyId);
                params.put("message", message);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void addUserToParty(final String partyId ) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_add_member_to_party";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_ADD_USER_TO_PARTY , new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {
                        String Msg = jObj.getString("msg");
                        getDataOfParty(idParty);
                        Toast.makeText(getApplicationContext(), "Выполнено: " +
                                Msg, Toast.LENGTH_LONG).show();
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
                Toast.makeText(getApplicationContext(),"Ошибка: " +
                        error.getMessage(), Toast.LENGTH_LONG).show();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("access_token", accessToken);
                params.put("id_party", partyId);
                params.put("version",Integer.toString(BuildConfig.VERSION_CODE));


                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void getDataOfParty(final int idParty) {

        cbAcceptParty.setText("Присоединиться");

        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_party";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_PARTY + idParty   , new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Получение данных квартирника: " + response);


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    boolean maxNumberView = jObj.getBoolean("max_number_view");

                    // Check for error node in json
                    if (!error) {

                        db = new SQLiteHandler(getApplicationContext());
                        List<String> members_list = new ArrayList<String>();
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>
                                (getApplicationContext(), R.layout.custom_textview_for_listview, members_list);
                        listView.setAdapter(arrayAdapter);

                        JSONObject party = jObj.getJSONObject("party");

                        String idUser = jObj.getString("id_user");

                        addressParty = party.getString("address");

                        idCreatedParty = party.getString("created_id");
                        txtTitleParty.setText(party.getString("title_party"));
                        txtDescriptionParty.setText(party.getString("description_party"));
                        txtNikNameCreatedParty.setText(party.getString("nik_name_created_party"));
                        txtDateTimeParty.setText(party.getString("date_time"));
                        txtRatingParty.setText(party.getString("rating"));
                        txtSource.setText(party.getString("source_url"));

                        if(Integer.parseInt(idUser) == Integer.parseInt(idCreatedParty)){

                            cbAcceptParty.setText("Это ваш квартирник");
                            cbAcceptParty.setEnabled(false);
                            txtAddressParty.setText("Адрес: \n" + addressParty + "\n");

                        }
                        JSONArray membersParty = party.getJSONArray("member");
                        for(int i=0;i<membersParty.length();i++){
                            String nameMember = membersParty.getJSONObject(i).getString("name_member");
                            String idMember =  membersParty.getJSONObject(i).getString("id_user");
                            String idPartyOfServer = party.getString("id");
                            if((Integer.parseInt(idMember) == Integer.parseInt(idUser)) && (idParty == Integer.parseInt(idPartyOfServer))  ){
                                cbAcceptParty.setText("Отказаться от участия");
                                txtAddressParty.setText("Адрес: \n\n" + addressParty);

                            }

                            members_list.add(nameMember);
                            arrayAdapter.notifyDataSetChanged();



                        }


                    } else {
                        if(expAccessToken && Boolean.toString(expAccessToken) != null){
                            updateDataTokens(tag_string_req);
                        }else{
                            if(maxNumberView && Boolean.toString(maxNumberView) != null ){
                                final AlertDialog.Builder errorCountViewPartyDialog = new AlertDialog.Builder(PartyActivity.this);
                                errorCountViewPartyDialog.setMessage("Лимит просмотра событий исчерпан.Дополнительные просмотры можно активировать в магазине.");
                                errorCountViewPartyDialog.setPositiveButton("Отправиться в магазин",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(PartyActivity.this, ShopActivity.class);
                                                    startActivity(intent);
                                                    finish();


                                            }
                                        })
                                        .setNegativeButton("Вернуться на карту",
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {

                                                        Intent intent = new Intent(PartyActivity.this, MapsActivity.class);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                });
                                errorCountViewPartyDialog.create();
                                errorCountViewPartyDialog.show();
                            }else {
                                String errorMsg = jObj.getString("error_msg");
                                Toast.makeText(getApplicationContext(), "Ошибка: " +
                                        errorMsg, Toast.LENGTH_LONG).show();
                            }

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
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version",Integer.toString(BuildConfig.VERSION_CODE));
                return params;
            }
        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void getReviewsOfParty(final int idParty) {
        cbAcceptParty.setText("Присоединиться");

        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_reviews_party";

        db = new SQLiteHandler(getApplicationContext());
        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_REVIEWS_OF_PARTY + idParty   , new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Получение всех отзывов: " + response);


                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {
                        JSONArray reviews = jObj.getJSONArray("reviews");
                        ListView listView = findViewById(R.id.list_of_messages);
                        ArrayList<HashMap<String, Object>> reviewsList = new ArrayList<>();
                        HashMap<String, Object> hashMap;

                        for(int i=0;i<reviews.length();i++){
                            hashMap = new HashMap<>();
                            hashMap.put(NAME_USER, reviews.getJSONObject(i).getString("name_user")); // Название
                            hashMap.put(TIME, reviews.getJSONObject(i).getString("created_at")); // Описание
                            hashMap.put(MESSAGE, reviews.getJSONObject(i).getString("message")); // Картинка
                            reviewsList.add(hashMap);
                        }

                        SimpleAdapter adapter = new SimpleAdapter(PartyActivity.this, reviewsList,
                                R.layout.message, new String[]{NAME_USER, TIME, MESSAGE}, new int[]{R.id.message_user, R.id.message_time, R.id.message_text});

                        // Устанавливаем адаптер для списка
                        listView.setAdapter(adapter);
                        listView.smoothScrollToPosition(adapter.getCount() - 1);
                        listView.setSelector(new ColorDrawable(Color.TRANSPARENT));

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
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version",Integer.toString(BuildConfig.VERSION_CODE));
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
                            case "req_add_review_to_party":
                                addReviewToParty(Integer.toString(idParty),message);
                            break;
                            case "req_add_member_to_party":
                                addUserToParty(Integer.toString(idParty));
                                break;
                            case "req_get_party":
                                getDataOfParty(idParty);
                                break;
                            case "req_get_reviews_party":
                                getReviewsOfParty(idParty);
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
