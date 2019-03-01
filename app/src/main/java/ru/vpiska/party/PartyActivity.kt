package ru.vpiska.party


import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.BuildConfig
import ru.vpiska.R
import ru.vpiska.app.AdMobController
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.CheckConnection
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.map.MapsActivity
import ru.vpiska.profile.OtherProfileActivity
import ru.vpiska.shop.ShopActivity
import java.util.*


class PartyActivity : AppCompatActivity() {

    private var txtTitleParty: TextView? = null
    private var txtDescriptionParty: TextView? = null
    private var txtNikNameCreatedParty: TextView? = null
    private var txtAddressParty: TextView? = null
    private var txtDateTimeParty: TextView? = null
    private var txtRatingParty: TextView? = null
    private var txtSource: TextView? = null
    private var cbAcceptParty: Button? = null

    private var listView: ListView? = null


    private var session: SessionManager? = null

    private var db: SQLiteHandler? = null

    private var idParty: Int = 0

    private var idCreatedParty: String? = null

    private var addressParty: String? = null

    private var message: String? = null

    private var mInterstitialAd: InterstitialAd? = null


    private val pDialog: ProgressDialog? = null

    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activty_party)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        val tabHost = findViewById<TabHost>(android.R.id.tabhost)
        // инициализация
        tabHost.setup()

        var tabSpec: TabHost.TabSpec = tabHost.newTabSpec("tag1")
        tabSpec.setIndicator("Главная")
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("tag2")
        tabSpec.setIndicator("Участники")
        tabSpec.setContent(R.id.tab2)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("tag3")
        tabSpec.setIndicator("Отзывы")
        tabSpec.setContent(R.id.tab3)
        tabHost.addTab(tabSpec)


        val tw = tabHost.findViewById<TabWidget>(android.R.id.tabs)
        for (i in 0 until tw.childCount) {
            val tabView = tw.getChildTabViewAt(i)
            val tv = tabView.findViewById<TextView>(android.R.id.title)
            tv.setTextColor(Color.WHITE)
            tv.textSize = 12f
        }

        tabHost.setCurrentTabByTag("tag1")

        txtTitleParty = findViewById(R.id.title_party)
        txtDescriptionParty = findViewById(R.id.description_party)
        txtNikNameCreatedParty = findViewById(R.id.created_party_nik_name)
        txtAddressParty = findViewById(R.id.address_party)
        txtDateTimeParty = findViewById(R.id.date_time)
        txtRatingParty = findViewById(R.id.rating)
        txtSource = findViewById(R.id.source)

        cbAcceptParty = findViewById(R.id.accept_party)
        listView = findViewById(R.id.listView)

        txtNikNameCreatedParty!!.setOnClickListener {
            val intent = Intent(this@PartyActivity, OtherProfileActivity::class.java)
            intent.putExtra("id_user", idCreatedParty)
            intent.putExtra("id_party", idParty.toString())
            startActivity(intent)
            finish()
        }
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        db = SQLiteHandler(applicationContext)
        session = SessionManager(applicationContext)

        if (session!!.isGuest) {
            //  cbAcceptParty.setEnabled(false);
            fab.isEnabled = false
        }

        idParty = Integer.parseInt(intent.extras!!.getString("id_party")!!)

        getDataOfParty(idParty)
        getReviewsOfParty(idParty)



        cbAcceptParty!!.setOnClickListener {
            if (session!!.isGuest) {
                val builder = AlertDialog.Builder(this@PartyActivity)
                builder.setMessage("Для того, чтобы присоединиться к событию, необходимо пройти регистрацию.")
                        .setTitle("Информация")
                        .setPositiveButton("Пройти регистрацию"

                        ) { dialog, id ->
                            session!!.setKeyIsGuest(false)

                            db!!.deleteDataTokensTable()

                            // Launching the login activity
                            val intent = Intent(this@PartyActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }.setNegativeButton("Позже") { dialog, which -> }

                // Create the AlertDialog object and return it
                builder.create()
                builder.show()
            } else {
                txtAddressParty!!.text = ""
                if (mInterstitialAd!!.isLoaded) {
                    mInterstitialAd!!.show()
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.")
                }
            }
        }

        mInterstitialAd = InterstitialAd(this@PartyActivity)
        mInterstitialAd!!.adUnitId = getString(R.string.banner_ad_unit_id_1)
        val adRequest = AdRequest.Builder().build()

        mInterstitialAd!!.loadAd(adRequest)

        mInterstitialAd!!.adListener = object : AdListener() {
            override fun onAdClosed() {
                // Load the next interstitial.

                mInterstitialAd!!.loadAd(AdRequest.Builder().build())

                if (CheckConnection.hasConnection(this@PartyActivity)) {
                    addUserToParty(Integer.toString(idParty))

                }

            }

            override fun onAdLoaded() {

            }

        }



        fab.setOnClickListener {
            val input = findViewById<EditText>(R.id.input)
            message = input.text.toString()
            addReviewToParty(Integer.toString(idParty), message)

            // Clear the input
            input.setText("")
        }


        AdMobController.showBanner(this)


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_party, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId

        if (id == R.id.action_about) {
            AppController.instance?.showDialogAboutUs(this)
        }
        if (id == R.id.action_exit) {
            logoutUser()
        }
        if (id == R.id.action_create_report) {
            val intent = Intent(this@PartyActivity, AddReportActivity::class.java)
            intent.putExtra("party_id", Integer.toString(idParty))
            startActivity(intent)
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val intent = Intent(this@PartyActivity, MapsActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@PartyActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun addReviewToParty(partyId: String, message: String?) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_add_review_to_party"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]

        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_ADD_REVIEW_TO_PARTY, Response.Listener { response ->
            Log.d(TAG, "Добавление отзыва $response")

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {

                    getReviewsOfParty(idParty)
                } else {
                    if (expAccessToken && java.lang.Boolean.toString(expAccessToken) != null) {
                        updateDataTokens(tag_string_req)
                    } else {
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
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
                params["access_token"] = accessToken!!
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)
                params["id_party"] = partyId
                params["message"] = message!!
                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun addUserToParty(partyId: String) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_add_member_to_party"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]

        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_ADD_USER_TO_PARTY, Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {
                    val Msg = jObj.getString("msg")
                    getDataOfParty(idParty)
                    Toast.makeText(applicationContext, "Выполнено: $Msg", Toast.LENGTH_LONG).show()
                } else {
                    if (expAccessToken && java.lang.Boolean.toString(expAccessToken) != null) {
                        updateDataTokens(tag_string_req)
                    } else {
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
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
                params["access_token"] = accessToken!!
                params["id_party"] = partyId
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)


                return params
            }

        }

        // Adding request to request queue
         AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun getDataOfParty(idParty: Int) {

        cbAcceptParty!!.text = "Присоединиться"

        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_get_party"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]

        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_PARTY + idParty, Response.Listener { response ->
            Log.d(TAG, "Получение данных квартирника: $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                val maxNumberView = jObj.getBoolean("max_number_view")

                // Check for error node in json
                if (!error) {

                    db = SQLiteHandler(applicationContext)
                    val members_list = ArrayList<String>()
                    val arrayAdapter = ArrayAdapter(applicationContext, R.layout.custom_textview_for_listview, members_list)
                    listView!!.adapter = arrayAdapter

                    val party = jObj.getJSONObject("party")

                    val idUser = jObj.getString("id_user")

                    addressParty = party.getString("address")

                    idCreatedParty = party.getString("created_id")
                    txtTitleParty!!.text = party.getString("title_party")
                    txtDescriptionParty!!.text = party.getString("description_party")
                    txtNikNameCreatedParty!!.text = party.getString("nik_name_created_party")
                    txtDateTimeParty!!.text = party.getString("date_time")
                    txtRatingParty!!.text = party.getString("rating")
                    txtSource!!.text = party.getString("source_url")

                    if (Integer.parseInt(idUser) == Integer.parseInt(idCreatedParty!!)) {

                        cbAcceptParty!!.text = "Это ваш квартирник"
                        cbAcceptParty!!.isEnabled = false
                        txtAddressParty!!.text = "Адрес: \n$addressParty\n"

                    }
                    val membersParty = party.getJSONArray("member")
                    for (i in 0 until membersParty.length()) {
                        val nameMember = membersParty.getJSONObject(i).getString("name_member")
                        val idMember = membersParty.getJSONObject(i).getString("id_user")
                        val idPartyOfServer = party.getString("id")
                        if (Integer.parseInt(idMember) == Integer.parseInt(idUser) && idParty == Integer.parseInt(idPartyOfServer)) {
                            cbAcceptParty!!.text = "Отказаться от участия"
                            txtAddressParty!!.text = "Адрес: \n\n" + addressParty!!

                        }

                        members_list.add(nameMember)
                        arrayAdapter.notifyDataSetChanged()


                    }


                } else {
                    if (expAccessToken && java.lang.Boolean.toString(expAccessToken) != null) {
                        updateDataTokens(tag_string_req)
                    } else {
                        if (maxNumberView && java.lang.Boolean.toString(maxNumberView) != null) {
                            val errorCountViewPartyDialog = AlertDialog.Builder(this@PartyActivity)
                            errorCountViewPartyDialog.setMessage("Лимит просмотра событий исчерпан.Дополнительные просмотры можно активировать в магазине.")
                            errorCountViewPartyDialog.setPositiveButton("Отправиться в магазин"
                            ) { dialog, which ->
                                val intent = Intent(this@PartyActivity, ShopActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                                    .setNegativeButton("Вернуться на карту"
                                    ) { dialog, id ->
                                        val intent = Intent(this@PartyActivity, MapsActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                            errorCountViewPartyDialog.create()
                            errorCountViewPartyDialog.show()
                        } else {
                            val errorMsg = jObj.getString("error_msg")
                            Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                        }

                    }
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
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken!!
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }
        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun getReviewsOfParty(idParty: Int) {
        cbAcceptParty!!.text = "Присоединиться"

        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_get_reviews_party"

        db = SQLiteHandler(applicationContext)
        val token = db!!.dataTokens
        accessToken = token["access_token"]

        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_REVIEWS_OF_PARTY + idParty, Response.Listener { response ->
            Log.d(TAG, "Получение всех отзывов: $response")


            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {
                    val reviews = jObj.getJSONArray("reviews")
                    val listView = findViewById<ListView>(R.id.list_of_messages)
                    val reviewsList = ArrayList<HashMap<String, Any>>()
                    var hashMap: HashMap<String, Any>

                    for (i in 0 until reviews.length()) {
                        hashMap = HashMap()
                        hashMap[NAME_USER] = reviews.getJSONObject(i).getString("name_user") // Название
                        hashMap[TIME] = reviews.getJSONObject(i).getString("created_at") // Описание
                        hashMap[MESSAGE] = reviews.getJSONObject(i).getString("message") // Картинка
                        reviewsList.add(hashMap)
                    }

                    val adapter = SimpleAdapter(this@PartyActivity, reviewsList,
                            R.layout.message, arrayOf(NAME_USER, TIME, MESSAGE), intArrayOf(R.id.message_user, R.id.message_time, R.id.message_text))

                    // Устанавливаем адаптер для списка
                    listView.adapter = adapter
                    listView.smoothScrollToPosition(adapter.count - 1)
                    listView.selector = ColorDrawable(Color.TRANSPARENT)

                } else {
                    if (expAccessToken && java.lang.Boolean.toString(expAccessToken) != null) {
                        updateDataTokens(tag_string_req)
                    } else {
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
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
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken!!
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }
        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun updateDataTokens(tag: String) {
        HttpsTrustManager.allowAllSSL()
        Log.e(TAG, "Обновляем токен")

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
                        "req_add_review_to_party" -> addReviewToParty(Integer.toString(idParty), message)
                        "req_add_member_to_party" -> addUserToParty(Integer.toString(idParty))
                        "req_get_party" -> getDataOfParty(idParty)
                        "req_get_reviews_party" -> getReviewsOfParty(idParty)
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

    companion object {

        private val TAG = PartyActivity::class.java.simpleName

        private val NAME_USER = "name_user" // Верхний текст
        private val TIME = "TIME" // ниже главного
        private val MESSAGE = "message"  // будущая картинка
    }

}
