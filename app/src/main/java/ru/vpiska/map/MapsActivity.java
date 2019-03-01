package ru.vpiska.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONArray;
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
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.party.AddPartyActivity;
import ru.vpiska.party.PartyActivity;
import ru.vpiska.profile.GuestProfileActivity;
import ru.vpiska.shop.ShopActivity;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback  {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private GoogleMap map;
    private SessionManager session;
    private SQLiteHandler db;
    private String latitudeMarker, longitudeMarker;
    private SupportMapFragment mapFragment;

    private LatLng sbp;

    private String accessToken;

    private ProgressDialog pDialog;

    private Marker myPosition;

    private int countAddingParty,maxNumberAddingParty,balance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);


        final String SHOW_INFORMATION_MAPS_WINDOW = "show_information_maps_window";
        final String SHOW_INFORMATION_GPS_WINDOW = "show_information_gps_window";

        pDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);

        session = new SessionManager(getApplicationContext());
        db = new SQLiteHandler(getApplicationContext());



        Button btnAddVpiska = findViewById(R.id.btnAddVpiska);

        if(session.isGuest()){
            btnAddVpiska.setVisibility(View.GONE);
        }

        btnAddVpiska.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(countAddingParty < maxNumberAddingParty){
                    Intent intent = new Intent(MapsActivity.this, AddPartyActivity.class);
                    intent.putExtra("latitudeMarker", latitudeMarker);
                    intent.putExtra("longitudeMarker", longitudeMarker);
                    startActivity(intent);
                    finish();
                }else{
                    final AlertDialog.Builder errorAddingPartyDialog = new AlertDialog.Builder(MapsActivity.this);
                    errorAddingPartyDialog.setTitle("Информация");
                    errorAddingPartyDialog.setCancelable(false);
                    if(balance >= 100){
                        errorAddingPartyDialog.setMessage("Лимит добавления событий исчерпан. У вас есть необходимое количество монет для создания события.");
                        errorAddingPartyDialog.setPositiveButton("Потратить монеты",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(MapsActivity.this, AddPartyActivity.class);
                                        intent.putExtra("latitudeMarker", latitudeMarker);
                                        intent.putExtra("longitudeMarker", longitudeMarker);
                                        startActivity(intent);
                                        finish();

                                    }
                                })
                                .setNegativeButton("Не тратить",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {


                                            }
                                        });
                    }else{
                        errorAddingPartyDialog.setMessage("Лимит добавления событий исчерпан. Создание одного мероприятия стоит 100 монет.");
                        errorAddingPartyDialog.setPositiveButton("Отправиться в магазин",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(MapsActivity.this, ShopActivity.class);
                                        startActivity(intent);
                                        finish();

                                    }
                                })
                                .setNegativeButton("Не покупать",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {


                                            }
                                        });
                    }


                    errorAddingPartyDialog.create();
                    errorAddingPartyDialog.show();
                }

            }
        });


        SharedPreferences sPref;
        sPref = getPreferences(MODE_PRIVATE);
        String checkShowInformationWindow = sPref.getString(SHOW_INFORMATION_MAPS_WINDOW, "0");
        if(checkShowInformationWindow.equals("0")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_info_about_maps)
                    .setTitle(R.string.title_perf_for_maps)
                    .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            // Create the AlertDialog object and return it
            builder.create();
            builder.show();

            SharedPreferences.Editor editor = sPref.edit();
            editor.putString(SHOW_INFORMATION_MAPS_WINDOW,"1");
            editor.apply();
        }

        String checkShowInformationGPSWindow = sPref.getString(SHOW_INFORMATION_GPS_WINDOW, "0");
        if(checkShowInformationGPSWindow.equals("0")){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_info_about_gps)
                    .setTitle(R.string.title_perf_for_gps)
                    .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                        }
                    });

            // Create the AlertDialog object and return it
            builder.create();
            builder.show();

            SharedPreferences.Editor editor = sPref.edit();
            editor.putString(SHOW_INFORMATION_GPS_WINDOW,"1");
            editor.apply();
        }



        AdMobController.showBanner(this);
        showDialog();
        getDataOfMarkers("all");



    }


    private void checkPermission(final GoogleMap googleMap){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {
                        hasPermission(googleMap);
                    }
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(), "Доступ ограничен.", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                        hasPermission(googleMap);
                    }
                }).check();

    }

    private void hasPermission(final GoogleMap googleMap) {

        LocationListener   locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                myPosition.remove();
                latitudeMarker = Double.toString(location.getLatitude());
                longitudeMarker = Double.toString(location.getLongitude());
                addMyPositionToMap(googleMap);
                //getDataOfMarkers("all");
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

               // Toast.makeText(MapsActivity.this,"GPS включен, метка будет выставлена на ваше положение",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String s) {
                myPosition.remove();
                latitudeMarker = "59.94203134328905";
                longitudeMarker = "30.32421223819256";

                addMyPositionToMap(googleMap);
               // getDataOfMarkers("all");



            }
        };

        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (result != PackageManager.PERMISSION_GRANTED) {
            checkPermission(googleMap);
        }else {
            LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            assert locationManager != null;
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,10,locationListener);
        }



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(session.isGuest()){
            getMenuInflater().inflate(R.menu.menu_guest, menu);
        }else {
            getMenuInflater().inflate(R.menu.menu_maps, menu);

        }
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.refresh) {
            mapFragment.getMapAsync(MapsActivity.this);
        }
        if (id == R.id.action_about) {
            AppController.getInstance().showDialogAboutUs(this);
        }
        if (id == R.id.action_exit) {
            logoutUser();
        }

        if (id == R.id.sort) {
            openSortDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(MapsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void openSortDialog() {
        AlertDialog.Builder builder;

        final String[] mSortName ={"Прогулка", "Домашняя посиделка","Выставка","Концерт","Игры","Кино"};

        builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите тип"); // заголовок для диалога

        builder.setItems(mSortName, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                // TODO Auto-generated method stub

                switch (item){
                    case 0:
                        getDataOfMarkers("people");
                        break;
                    case 1:
                        getDataOfMarkers("tea");
                        break;
                    case 2:
                        getDataOfMarkers("picture");
                        break;
                    case 3:
                        getDataOfMarkers("film");
                        break;
                    case 4:
                        getDataOfMarkers("game");
                        break;
                    case 5:
                        getDataOfMarkers("sacs");
                        break;
                    default:
                        getDataOfMarkers("all");
                        break;

                }


            }
        });
        builder.setCancelable(true);
        builder.show();
    }


    @Override
    public void onBackPressed() {
        if(session.isGuest()){
            Intent intent = new Intent(MapsActivity.this, GuestProfileActivity.class);
            startActivity(intent);
            finish();
        }else{
            Intent intent = new Intent(MapsActivity.this, MainScreenActivity.class);
            startActivity(intent);
            finish();
        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        latitudeMarker = "59.94203134328905";
        longitudeMarker = "30.32421223819256";
        sbp = new LatLng(Double.parseDouble(latitudeMarker),Double.parseDouble(longitudeMarker));
        myPosition =  map.addMarker(new MarkerOptions().title("Перетащи меня").draggable(true).position(sbp).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        myPosition.showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLng(sbp));
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng newMarker =  marker.getPosition();
                latitudeMarker = Double.toString(newMarker.latitude);
                longitudeMarker = Double.toString(newMarker.longitude);
            }
        });
        map.getUiSettings().setZoomControlsEnabled(true);
       hasPermission(googleMap);
    }



    private void addMyPositionToMap(GoogleMap googleMap){

        map = googleMap;

        sbp = new LatLng(Double.parseDouble(latitudeMarker),Double.parseDouble(longitudeMarker));
        myPosition = map.addMarker(new MarkerOptions().title("Перетащи меня").draggable(true).position(sbp).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        myPosition.showInfoWindow();
        map.moveCamera(CameraUpdateFactory.newLatLng(sbp));
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                LatLng newMarker =  marker.getPosition();
                latitudeMarker = Double.toString(newMarker.latitude);
                longitudeMarker = Double.toString(newMarker.longitude);
            }
        });
       // map.getUiSettings().setZoomControlsEnabled(true);


        hideDialog();

    }

    private void getDataOfMarkers(final String filterType) {
        pDialog.setMessage("Получение данных карты ...");

        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_get_parties";

        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.GET, AppConfig.URL_GET_PARTIES, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                map.clear();
                Log.d(TAG, "Ответ сервера [Получение меток] " + response);


                try {

                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");

                    // Check for error node in json
                    if (!error) {

                        JSONArray arr = jObj.getJSONArray("parties");
                        JSONObject my_data = jObj.getJSONObject("data_user");
                        countAddingParty = my_data.getInt("count_adding_party");
                        maxNumberAddingParty = my_data.getInt("max_number_adding_party");
                        balance = my_data.getInt("balance");

                        for (int i = 0; i < arr.length(); i++) {
                            BitmapDescriptor icon;
                            switch (arr.getJSONObject(i).getString("type")){
                                case "people":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_walk_people);
                                    break;
                                case "tea":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_tea);
                                    break;
                                case "picture":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_picture);
                                    break;
                                case "film":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_film);
                                    break;
                                case "game":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_game);
                                    break;
                                case "sacs":
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_sacs);
                                    break;
                                default:
                                    icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_tea);
                                    break;
                            }
                            if(Integer.parseInt(arr.getJSONObject(i).getString("alcohol")) == 1){
                                icon  = BitmapDescriptorFactory.fromResource(R.drawable.ic_alcohol);
                            }

                            String[] coor = (arr.getJSONObject(i).getString("coordinates")).split(" ");
                            if((Integer.parseInt(arr.getJSONObject(i).getString("status")) == 1)  && (filterType.equals(arr.getJSONObject(i).getString("type") ) || filterType.equals("all"))) {

                                map.addMarker(new MarkerOptions()

                                            .title(arr.getJSONObject(i).getString("title_party"))
                                            .position(new LatLng(
                                                            Double.parseDouble(coor[0]),
                                                            Double.parseDouble(coor[1])
                                                    )
                                            )
                                            .icon(icon)
                                    ).setTag(arr.getJSONObject(i).getString("id"));
                                map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {

                                    @Override
                                    public void onInfoWindowClick(final Marker arg0) {
                                      //  final AlertDialog.Builder errorCountViewPartyDialog = new AlertDialog.Builder(MapsActivity.this,R.style.AppTheme);
                                     //   if(Integer.parseInt(countView) < Integer.parseInt(maxNumberView)){
                                            if (arg0.getTag() != null) {
                                                Intent intent = new Intent(MapsActivity.this, PartyActivity.class);
                                                intent.putExtra("id_party", arg0.getTag().toString());
                                                startActivity(intent);
                                                finish();
                                            }
//                                        }else{
//
//                                            errorCountViewPartyDialog.setTitle("Информация");
//                                            errorCountViewPartyDialog.setCancelable(false);
//                                            if(Integer.parseInt(balance) >= 100){
//                                                errorCountViewPartyDialog.setMessage("Лимит просмотра событий исчерпан. У вас есть необходимое количество монет для просмотра события. \nПримечание: если вы уже были присоединены к мероприятию, то баланс не будет уменьшен.");
//                                                errorCountViewPartyDialog.setPositiveButton("Перейти к просмотру",
//                                                        new DialogInterface.OnClickListener() {
//                                                            public void onClick(DialogInterface dialog, int which) {
//                                                                if (arg0.getTag() != null) {
//                                                                    Intent intent = new Intent(MapsActivity.this, PartyActivity.class);
//                                                                    intent.putExtra("id_party", arg0.getTag().toString());
//                                                                    startActivity(intent);
//                                                                    finish();
//                                                                }
//
//                                                            }
//                                                        })
//                                                        .setNegativeButton("Вернуться на карту",
//                                                                new DialogInterface.OnClickListener() {
//                                                                    public void onClick(DialogInterface dialog, int id) {
//
//
//                                                                    }
//                                                                });
//                                                errorCountViewPartyDialog.create();
//                                                errorCountViewPartyDialog.show();
//                                            }else{
//
//                                                Intent intent = new Intent(MapsActivity.this, PartyActivity.class);
//                                                intent.putExtra("id_party", arg0.getTag().toString());
//                                                startActivity(intent);
//                                                finish();
//                                            }



                                      //  }



                                    }

                                });
                            }


                        }
                        addMyPositionToMap(map);
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
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Content-Type", "application/json; charset=UTF-8");
                params.put("Access-Token", accessToken);
                params.put("Version",Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }
        };

        if(CheckConnection.hasConnection(MapsActivity.this)) {
            AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
        }

    }



    private void updateDataTokens(final String tag){
        HttpsTrustManager.allowAllSSL();
        Log.d(TAG, "Обновляем токен");

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
                            case "req_get_parties":
                                getDataOfMarkers("all");
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
                Map<String, String> params = new HashMap<>();
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
