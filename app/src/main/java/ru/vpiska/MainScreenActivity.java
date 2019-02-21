package ru.vpiska;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.bumptech.glide.Glide;
import com.flurry.android.FlurryAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.InAppBillingResources;
import ru.vpiska.helper.PreferencesManager;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.map.MapsActivity;
import ru.vpiska.profile.FeedbackActivity;
import ru.vpiska.profile.MyProfileActivity;
import ru.vpiska.rating.RatingActivity;
import ru.vpiska.shop.ShopActivity;

import static ru.vpiska.helper.InAppBillingResources.getSKU_Disable_Ads;

public class MainScreenActivity extends AppCompatActivity implements BillingProcessor.IBillingHandler {

    private TextView txtFirstName, txtNikName;
    private ImageView imgAvatar;
    private static long back_pressed;

    private static final String TAG = MainScreenActivity.class.getSimpleName();


    private BillingProcessor bp;
    private SQLiteHandler db;
    private SessionManager session;

    private String accessToken;
    private PreferencesManager prefManager; // класс, который работает с SharedPreferences. Я для себя решил вынести всю логику отдельно
    private TransactionDetails detailsInfo;

    private String partyId, memberId, idNote;

    private EditText rating;

    private ProgressDialog pDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FlurryAgent.onStartSession(this);

        final String SHOW_INFORMATION_PLAY_MARKET = "show_information_play_market";

        txtFirstName = findViewById(R.id.first_name);
        txtNikName = findViewById(R.id.nik_name);

        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnMap = findViewById(R.id.btnMap);
        Button btnRating = findViewById(R.id.btnRating);
        imgAvatar = findViewById(R.id.imgAvatar);

        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        prefManager = new PreferencesManager(getApplicationContext()); // класс, который работает с `SharedPreferences`
        // session manager
        session = new SessionManager(getApplicationContext());

        if (!session.isLoggedIn()) {
            logoutUser();
            FlurryAgent.onEndSession(this);
        }
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        getMyProfile();
        checkUpdateRating();
        getDataOfParties();

        SharedPreferences sPref;
        sPref = getPreferences(MODE_PRIVATE);
        String checkShowInformationWindow = sPref.getString(SHOW_INFORMATION_PLAY_MARKET, "0");
        if(checkShowInformationWindow.equals("0")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_info_play_market)
                    .setTitle(R.string.title_perf_for_play_market)
                    .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            // Create the AlertDialog object and return it
            builder.create();
            builder.show();

            SharedPreferences.Editor editor = sPref.edit();
            editor.putString(SHOW_INFORMATION_PLAY_MARKET,"1");
            editor.apply();
        }


        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainScreenActivity.this, MyProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnRating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainScreenActivity.this, RatingActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainScreenActivity.this, MapsActivity.class);
                startActivity(intent);
                finish();
            }
        });
        bp = new BillingProcessor(getApplicationContext(),
                InAppBillingResources.getRsaKey(), InAppBillingResources.getMerchantId(), this); // инициализируем `BillingProcessor`. В документации на `GitHub` сказано, что для защиты от липовых покупок через приложения типа `freedom` необходимо в конструктор `BillingProcessor`'а передать еще и свой `MERCHANT_ID`. Где его взять - внизу текущего ответа опишу шаги
        bp.initialize();
        onPurchaseHistoryRestored();
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
        if (id == R.id.action_shop) {
            Intent intent = new Intent(MainScreenActivity.this, ShopActivity.class);
            startActivity(intent);
            finish();
        }
        if (id == R.id.action_about) {
            AppController.getInstance().showDialogAboutUs(this);
        }
        if (id == R.id.feeadback) {
            Intent intent = new Intent(MainScreenActivity.this, FeedbackActivity.class);
            startActivity(intent);
            finish();
        }
        if (id == R.id.action_disabled_ads) {
            buyAds(this);
            createPurchase(detailsInfo);
        }
        if (id == R.id.action_exit) {
            logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed();
            logoutUser();
            finish();
        } else {
            Toast.makeText(getBaseContext(), "Нажми еще раз для выхода!",
                    Toast.LENGTH_SHORT).show();
        }
        back_pressed = System.currentTimeMillis();
    }


    private void logoutUser() {
        session.setLogin(false);
        db = new SQLiteHandler(getApplicationContext());
        db.deleteDataTokensTable();
        // Launching the login activity
        Intent intent = new Intent(MainScreenActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void checkUpdateRating() {
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_check_update_rating_user";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_CHECK_UPDATE_RATING, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Данные обновления рейтинга: " + response);
                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean checkUpdateRating = jObj.getBoolean("update_rating");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    String message = jObj.getString("message");

                    // Check for error node in json
                    if (!error) {
                        if (checkUpdateRating) {
                            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                        }
                    } else {

                        if (expAccessToken) {
                            updateDataTokens(tag_string_req);
                        } else {
                            // Error in login. Get the error message
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version", Integer.toString(BuildConfig.VERSION_CODE));
                return params;
            }


        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void getDataOfParties() {
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_parties";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_CHECK_SET_ANSWER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Данные получения информации об обновлении рейтинга: " + response);
                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");


                    if (!error) {
                        JSONObject member = jObj.getJSONObject("member");
                        JSONObject party = member.getJSONObject("party");
                        partyId = member.getString("id_party");
                        memberId = member.getString("id_user");
                        final String nameParty = party.getString("title_party");
                        final String nameCreatedUser = party.getString("created_name");
                        showRatingDialog(nameParty, nameCreatedUser, partyId, memberId);


                    } else {
                        if (expAccessToken) {
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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version", Integer.toString(BuildConfig.VERSION_CODE));
                return params;
            }
        };

        if (CheckConnection.hasConnection(MainScreenActivity.this)) {
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }
    }


    private void showRatingDialog(String nameParty, String nameCreatedUser, final String partyId, final String memberId) {

        final AlertDialog.Builder ratingDialog = new AlertDialog.Builder(this);
        ratingDialog.setIcon(android.R.drawable.btn_star_big_on);
        ratingDialog.setTitle("Информация");
        ratingDialog.setCancelable(false);
        ratingDialog.setMessage("Вы присутствовали на мероприятии '" + nameParty + "' , теперь вы можете поставить оценку её создателю " + nameCreatedUser + ". От -10 до 10.");

        @SuppressLint("InflateParams") View linearlayout = getLayoutInflater().inflate(R.layout.dialog_rating, null);
        ratingDialog.setView(linearlayout);
        rating = linearlayout.findViewById(R.id.rating);


        ratingDialog.setPositiveButton("Оценить",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!rating.getText().toString().isEmpty()) {
                           // Toast.makeText(getApplicationContext(), "Ошибка: неверное значение рейтинга. " + (Integer.parseInt(rating.getText().toString()) > 10), Toast.LENGTH_LONG).show();
                            updateRatingUser(partyId, memberId, rating.getText().toString());
                        } else {
                            getDataOfParties();
                            Toast.makeText(getApplicationContext(), "Ошибка: неверное значение рейтинга. ", Toast.LENGTH_LONG).show();



                        }


                    }
                })

                .setNegativeButton("Не оценивать",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                updateRatingUser(partyId, memberId, "0");

                            }
                        });

        ratingDialog.create();
        ratingDialog.show();
    }

    private void updateRatingUser(final String partyId, final String memberId, final String rating) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update_rating_user";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");

        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_RATING_USER + memberId, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Данные получения информации об обновлении рейтинга 2: " + response);
                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), "Успешно: Рейтинг обновлен. Добавлено " + rating +
                                " единиц.", Toast.LENGTH_LONG).show();

                    } else {
                        if (expAccessToken) {
                            updateDataTokens(tag_string_req);
                        } else {
                            // Error in login. Get the error message
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

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
                params.put("party_id", partyId);
                params.put("rating", rating);
                params.put("access_token", accessToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }
        };
        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void getMyProfile() {
        pDialog.setMessage("Получение данных профиля ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_data_of_my_profile";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    Log.d(TAG, "Токен действителен. " );
                    Log.d(TAG, "Ответ сервера [Получение профиля] " + response);
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


                        Glide.with(getApplicationContext())
                                .load(image)
                                .placeholder(R.drawable.ic_profile)
                                .fitCenter()
                                .override(400, 400)
                                .dontAnimate()
                                .into(imgAvatar);

                        txtFirstName.setText(firstName);
                        txtNikName.setText(nikName);

                        final JSONObject notes = jObj.getJSONObject("notes");

                        idNote = notes.getString("id");

                        AlertDialog.Builder builder = new AlertDialog.Builder(MainScreenActivity.this);
                        builder.setMessage(notes.getString("note"))
                                .setTitle("Информация")
                                .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Toast.makeText(getApplicationContext(), idNote, Toast.LENGTH_LONG).show();
                                        changeStatusNoteForUser(idNote);
                                    }
                                });
                        // Create the AlertDialog object and return it
                        builder.create();
                        builder.show();

                    } else {
                        if (expAccessToken) {
                            Log.d(TAG, "Токен истек. Обновляем токен доступа: " + response);
                            updateDataTokens(tag_string_req);
                        } else {
                            hideDialog();
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

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
                return params;
            }
        };
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    private void changeStatusNoteForUser(final String idNote) {
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update_status_note";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_NOTE + idNote, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Ответ сервера [Получение профиля] " + response);


                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    String message = jObj.getString("message");


                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    } else {
                        if (expAccessToken) {
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
                params.put("access_token", accessToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    private void updateDataTokens(final String tag) {
        HttpsTrustManager.allowAllSSL();
        Log.d(TAG, "Обновляем токен");
        String tag_string_req = "req_update_data_tokens";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> dataTokens = db.getDataTokens();

        final String refreshToken = dataTokens.get("refresh_token");

        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
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
                        Log.d(TAG, "Обновили таблицу");
                        db.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken);

                        Log.d(TAG, "Получили новый токен:" + accessToken);

                        switch (tag) {
                            case "req_get_data_of_my_profile":
                                getMyProfile();
                                break;
                            case "req_get_parties":
                                getDataOfParties();
                                break;
                            case "req_update_rating_user":
                                updateRatingUser(partyId, memberId, rating.getText().toString());
                                break;
                            case "req_update_status_note":
                                changeStatusNoteForUser(idNote);
                                break;

                            case "req_check_update_rating_user":
                                checkUpdateRating();
                                break;
                            case "req_create_purchase":
                                createPurchase(detailsInfo);
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
                params.put("refresh_token", refreshToken);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    //Работа с покупаками

    private void buyAds(Activity activity) {
        bp.loadOwnedPurchasesFromGoogle();
        if (!bp.isPurchased(getSKU_Disable_Ads())) {
            bp.purchase(activity, getSKU_Disable_Ads());
        } else {
            Toast.makeText(getApplicationContext(),
                    "Покупка уже осуществлена.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // перезагружаем приложение
    private void restartApp() {
        Intent rIntent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getApplication().getPackageName());
        if (rIntent != null) {
            rIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            getApplicationContext().startActivity(rIntent);
        }
    }

    // ... другие методы класса
    // [START billing part of class]
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public void onProductPurchased(@NonNull String productId, TransactionDetails details) {
        // Called when requested PRODUCT ID was successfully purchased
        // Вызывается, когда запрашиваемый PRODUCT ID был успешно куплен
        Log.d(TAG, "Ответ сервера Google [Покупка товара]" + details);

        if (bp.isPurchased(productId)) {
            detailsInfo = details;
            createPurchase(detailsInfo);
            prefManager.setAdsStatus(false); // 1. записываем в `SharedPreferences` состояние рекламы (ВЫКЛ / false)
            restartApp(); // 3. перезагружаем приложение
        }


    }

    private void createPurchase(final TransactionDetails details) {
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_create_purchase";


        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        final String accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_CREATE_PURCHASE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.d(TAG, "Ответ сервара [Покупка товара]" + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    String message = jObj.getString("message");


                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    } else {
                        if (expAccessToken) {
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
                params.put("access_token", accessToken);
                params.put("token", details.purchaseInfo.purchaseData.purchaseToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }


    @Override
    public void onPurchaseHistoryRestored() {
        //Вызывается, когда история покупки была восстановлена,
        // и список всех принадлежащих идентификаторы продуктов был загружен из Google Play
        // так Вы сможете НУЖНУЮ покупку проверить
        bp.loadOwnedPurchasesFromGoogle();
        if (bp.isPurchased(getSKU_Disable_Ads())) {  // true - куплено
            // пишем в `SharedPreferences`, что отключили рекламу
            prefManager.setAdsStatus(false);
        } else if (!PreferencesManager.getAdsStatus()) {
            restartApp(); // 3. перезагружаем приложение
            prefManager.setAdsStatus(true);
        }

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        // Вызывается, когда появляется ошибка. См. константы класса
        // для получения более подробной информации
    }

    @Override
    public void onBillingInitialized() {
        bp.loadOwnedPurchasesFromGoogle();
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