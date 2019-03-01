package ru.vpiska.map

import android.Manifest
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.ads.reward.RewardedVideoAd
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.BuildConfig
import ru.vpiska.MainScreenActivity
import ru.vpiska.R
import ru.vpiska.app.AdMobController
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.CheckConnection
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.party.AddPartyActivity
import ru.vpiska.party.PartyActivity
import ru.vpiska.profile.GuestProfileActivity
import ru.vpiska.shop.ShopActivity
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var map: GoogleMap? = null
    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null
    private var latitudeMarker: String? = null
    private var longitudeMarker: String? = null
    private var mapFragment: SupportMapFragment? = null

    private var sbp: LatLng? = null

    private var accessToken: String? = null

    private var pDialog: ProgressDialog? = null

    private var myPosition: Marker? = null

    private var countAddingParty: Int = 0
    private var maxNumberAddingParty: Int = 0
    private var balance: Int = 0

    private val mRewardedVideoAd: RewardedVideoAd? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this@MapsActivity)


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)


        val SHOW_INFORMATION_MAPS_WINDOW = "show_information_maps_window"
        val SHOW_INFORMATION_GPS_WINDOW = "show_information_gps_window"

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)

        session = SessionManager(applicationContext)
        db = SQLiteHandler(applicationContext)


        val btnAddVpiska = findViewById<Button>(R.id.btnAddVpiska)

        //        if(session.isGuest()){
        //            btnAddVpiska.setVisibility(View.GONE);
        //        }

        btnAddVpiska.setOnClickListener {
            mRewardedVideoAd!!.show()
            if (countAddingParty < maxNumberAddingParty) {
                val intent = Intent(this@MapsActivity, AddPartyActivity::class.java)
                intent.putExtra("latitudeMarker", latitudeMarker)
                intent.putExtra("longitudeMarker", longitudeMarker)
                startActivity(intent)
                finish()
            } else {
                val errorAddingPartyDialog = AlertDialog.Builder(this@MapsActivity)
                errorAddingPartyDialog.setTitle("Информация")
                errorAddingPartyDialog.setCancelable(false)
                if (balance >= 100) {
                    errorAddingPartyDialog.setMessage("Лимит добавления событий исчерпан. У вас есть необходимое количество монет для создания события.")
                    errorAddingPartyDialog.setPositiveButton("Потратить монеты"
                    ) { dialog, which ->
                        val intent = Intent(this@MapsActivity, AddPartyActivity::class.java)
                        intent.putExtra("latitudeMarker", latitudeMarker)
                        intent.putExtra("longitudeMarker", longitudeMarker)
                        startActivity(intent)
                        finish()
                    }
                            .setNegativeButton("Не тратить"
                            ) { dialog, id -> }
                } else {
                    errorAddingPartyDialog.setMessage("Лимит добавления событий исчерпан. Создание одного мероприятия стоит 100 монет.")
                    errorAddingPartyDialog.setPositiveButton("Отправиться в магазин"
                    ) { dialog, which ->
                        val intent = Intent(this@MapsActivity, ShopActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                            .setNegativeButton("Не покупать"
                            ) { dialog, id -> }
                }


                errorAddingPartyDialog.create()
                errorAddingPartyDialog.show()
            }
        }


        val sPref: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val checkShowInformationWindow = sPref.getString(SHOW_INFORMATION_MAPS_WINDOW, "0")
        if (checkShowInformationWindow == "0") {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.message_info_about_maps)
                    .setTitle(R.string.title_perf_for_maps)
                    .setPositiveButton(R.string.good) { dialog, id -> }

            // Create the AlertDialog object and return it
            builder.create()
            builder.show()

            val editor = sPref.edit()
            editor.putString(SHOW_INFORMATION_MAPS_WINDOW, "1")
            editor.apply()
        }

        val checkShowInformationGPSWindow = sPref.getString(SHOW_INFORMATION_GPS_WINDOW, "0")
        if (checkShowInformationGPSWindow == "0") {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.message_info_about_gps)
                    .setTitle(R.string.title_perf_for_gps)
                    .setPositiveButton(R.string.good) { dialog, id -> }

            // Create the AlertDialog object and return it
            builder.create()
            builder.show()

            val editor = sPref.edit()
            editor.putString(SHOW_INFORMATION_GPS_WINDOW, "1")
            editor.apply()
        }



        AdMobController.showBanner(this)


        showDialog()
        getDataOfMarkers("all")


    }


    private fun checkPermission(googleMap: GoogleMap) {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        hasPermission(googleMap)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        Toast.makeText(applicationContext, "Доступ ограничен.", Toast.LENGTH_SHORT).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                        hasPermission(googleMap)
                    }
                }).check()

    }

    private fun hasPermission(googleMap: GoogleMap) {

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                myPosition!!.remove()
                latitudeMarker = java.lang.Double.toString(location.latitude)
                longitudeMarker = java.lang.Double.toString(location.longitude)
                addMyPositionToMap(googleMap)
                //getDataOfMarkers("all");
            }

            override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {

            }

            override fun onProviderEnabled(s: String) {

                // Toast.makeText(MapsActivity.this,"GPS включен, метка будет выставлена на ваше положение",Toast.LENGTH_SHORT).show();
            }

            override fun onProviderDisabled(s: String) {
                myPosition!!.remove()
                latitudeMarker = "59.94203134328905"
                longitudeMarker = "30.32421223819256"

                addMyPositionToMap(googleMap)
                // getDataOfMarkers("all");


            }
        }

        val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (result != PackageManager.PERMISSION_GRANTED) {
            checkPermission(googleMap)
        } else {
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 10f, locationListener)
        }


    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (session!!.isGuest) {
            menuInflater.inflate(R.menu.menu_guest, menu)
        } else {
            menuInflater.inflate(R.menu.menu_maps, menu)

        }
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId
        if (id == R.id.refresh) {
            mapFragment!!.getMapAsync(this@MapsActivity)
        }
        if (id == R.id.action_about) {
            AppController.instance?.showDialogAboutUs(this)
        }
        if (id == R.id.action_exit) {
            logoutUser()
        }

        if (id == R.id.sort) {
            openSortDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@MapsActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openSortDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

        val mSortName = arrayOf("Прогулка", "Домашняя посиделка", "Выставка", "Концерт", "Игры", "Кино")

        builder.setTitle("Выберите тип") // заголовок для диалога

        builder.setItems(mSortName) { dialog, item ->
            // TODO Auto-generated method stub

            when (item) {
                0 -> getDataOfMarkers("people")
                1 -> getDataOfMarkers("tea")
                2 -> getDataOfMarkers("picture")
                3 -> getDataOfMarkers("film")
                4 -> getDataOfMarkers("game")
                5 -> getDataOfMarkers("sacs")
                else -> getDataOfMarkers("all")
            }
        }
        builder.setCancelable(true)
        builder.show()
    }


    override fun onBackPressed() {
        if (session!!.isGuest) {
            val intent = Intent(this@MapsActivity, GuestProfileActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this@MapsActivity, MainScreenActivity::class.java)
            startActivity(intent)
            finish()
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        latitudeMarker = "59.94203134328905"
        longitudeMarker = "30.32421223819256"
        sbp = LatLng(java.lang.Double.parseDouble(latitudeMarker!!), java.lang.Double.parseDouble(longitudeMarker!!))
        myPosition = map!!.addMarker(MarkerOptions().title("Перетащи меня").draggable(true).position(sbp!!).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        myPosition!!.showInfoWindow()
        map!!.moveCamera(CameraUpdateFactory.newLatLng(sbp))
        map!!.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {

            }

            override fun onMarkerDrag(marker: Marker) {

            }

            override fun onMarkerDragEnd(marker: Marker) {
                val newMarker = marker.position
                latitudeMarker = java.lang.Double.toString(newMarker.latitude)
                longitudeMarker = java.lang.Double.toString(newMarker.longitude)
            }
        })
        map!!.uiSettings.isZoomControlsEnabled = true
        hasPermission(googleMap)
    }


    private fun addMyPositionToMap(googleMap: GoogleMap?) {

        map = googleMap

        sbp = LatLng(java.lang.Double.parseDouble(latitudeMarker!!), java.lang.Double.parseDouble(longitudeMarker!!))
        myPosition = map!!.addMarker(MarkerOptions().title("Перетащи меня").draggable(true).position(sbp!!).icon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
        myPosition!!.showInfoWindow()
        map!!.moveCamera(CameraUpdateFactory.newLatLng(sbp))
        map!!.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {

            }

            override fun onMarkerDrag(marker: Marker) {

            }

            override fun onMarkerDragEnd(marker: Marker) {
                val newMarker = marker.position
                latitudeMarker = java.lang.Double.toString(newMarker.latitude)
                longitudeMarker = java.lang.Double.toString(newMarker.longitude)
            }
        })
        // map.getUiSettings().setZoomControlsEnabled(true);


        hideDialog()

    }

    private fun getDataOfMarkers(filterType: String) {
        pDialog!!.setMessage("Получение данных карты ...")

        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_get_parties"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_PARTIES, Response.Listener { response ->
            map!!.clear()
            Log.d(TAG, "Ответ сервера [Получение меток] $response")


            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {

                    val arr = jObj.getJSONArray("parties")
                    val my_data = jObj.getJSONObject("data_user")
                    countAddingParty = my_data.getInt("count_adding_party")
                    maxNumberAddingParty = my_data.getInt("max_number_adding_party")
                    balance = my_data.getInt("balance")

                    for (i in 0 until arr.length()) {
                        var icon: BitmapDescriptor
                        icon = when (arr.getJSONObject(i).getString("type")) {
                            "people" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_walk_people)
                            "tea" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_tea)
                            "picture" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_picture)
                            "film" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_film)
                            "game" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_game)
                            "sacs" -> BitmapDescriptorFactory.fromResource(R.drawable.ic_sacs)
                            else -> BitmapDescriptorFactory.fromResource(R.drawable.ic_tea)
                        }
                        if (Integer.parseInt(arr.getJSONObject(i).getString("alcohol")) == 1) {
                            icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_alcohol)
                        }

                        val coor = arr.getJSONObject(i).getString("coordinates").split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (Integer.parseInt(arr.getJSONObject(i).getString("status")) == 1 && (filterType == arr.getJSONObject(i).getString("type") || filterType == "all")) {

                            map!!.addMarker(MarkerOptions()

                                    .title(arr.getJSONObject(i).getString("title_party"))
                                    .position(LatLng(
                                            java.lang.Double.parseDouble(coor[0]),
                                            java.lang.Double.parseDouble(coor[1])
                                    )
                                    )
                                    .icon(icon)
                            ).tag = arr.getJSONObject(i).getString("id")
                            map!!.setOnInfoWindowClickListener { arg0 ->
                                //  final AlertDialog.Builder errorCountViewPartyDialog = new AlertDialog.Builder(MapsActivity.this,R.style.AppTheme);
                                //   if(Integer.parseInt(countView) < Integer.parseInt(maxNumberView)){
                                if (arg0.tag != null) {
                                    val intent = Intent(this@MapsActivity, PartyActivity::class.java)
                                    intent.putExtra("id_party", arg0.tag!!.toString())
                                    startActivity(intent)
                                    finish()
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
                        }


                    }
                    addMyPositionToMap(map)
                } else {

                    if (expAccessToken) {
                        updateDataTokens(tag_string_req)
                    } else {
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }


                }

            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()

            }
        }, Response.ErrorListener { error ->
            hideDialog()
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken!!
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }
        }

        if (CheckConnection.hasConnection(this@MapsActivity)) {
            AppController.instance?.addToRequestQueue(strReq, tag_string_req)
        }

    }


    private fun updateDataTokens(tag: String) {
        HttpsTrustManager.allowAllSSL()
        Log.d(TAG, "Обновляем токен")

        val tag_string_req = "req_update_data_tokens"
        db = SQLiteHandler(applicationContext)
        val dataTokens = db!!.dataTokens

        val refreshToken = dataTokens["refresh_token"]

        val strReq = object : StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, Response.Listener { response ->
            Log.d(TAG, "Ошибка авторизации: $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {

                    val dataTokens = jObj.getJSONObject("data_tokens")
                    val accessToken = dataTokens.getString("access_token")
                    val refreshToken = dataTokens.getString("refresh_token")
                    val expAccessToken = dataTokens.getString("exp_access_token")
                    val expRefreshToken = dataTokens.getString("exp_refresh_token")
                    Log.e(TAG, "Обновили таблицу")
                    db!!.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)

                    Log.e(TAG, "Получили новый токен:$accessToken")

                    when (tag) {
                        "req_get_parties" -> getDataOfMarkers("all")
                    }
                } else {
                    // Error in login. Get the error message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                }

            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()
                Toast.makeText(applicationContext, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Login Error: " + error.message)
            Toast.makeText(applicationContext, "Ошибка: " + error.message, Toast.LENGTH_LONG).show()
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["refresh_token"] = refreshToken!!

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun showDialog() {
        if (!pDialog!!.isShowing)
            pDialog!!.show()
    }

    private fun hideDialog() {
        if (pDialog!!.isShowing)
            pDialog!!.dismiss()
    }

    companion object {

        private val TAG = MapsActivity::class.java.simpleName
    }

}
