package ru.vpiska

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.bumptech.glide.Glide
import com.flurry.android.FlurryAgent
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.app.AdMobController
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.*
import ru.vpiska.helper.InAppBillingResources.skU_Disable_Ads
import ru.vpiska.map.MapsActivity
import ru.vpiska.profile.FeedbackActivity
import ru.vpiska.profile.MyProfileActivity
import ru.vpiska.rating.RatingActivity
import ru.vpiska.shop.ShopActivity
import java.util.*

class MainScreenActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {

    private lateinit var txtFirstName: TextView
    private lateinit var txtNikName: TextView
    private lateinit var imgAvatar: ImageView


    private lateinit var bp: BillingProcessor
    private lateinit var db: SQLiteHandler
    private lateinit var session: SessionManager

    private lateinit var accessToken: String
    private lateinit var prefManager: PreferencesManager
    private lateinit var detailsInfo: TransactionDetails

    private lateinit var partyId: String
    private lateinit var memberId: String
    private lateinit var idNote: String

    private lateinit var rating: EditText

    private lateinit var progressBar: ProgressBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FlurryAgent.onStartSession(this)


        val showInformationPlayMarket = "showInformationPlayMarket"

        txtFirstName = findViewById(R.id.first_name)
        txtNikName = findViewById(R.id.nik_name)

        val btnProfile = findViewById<Button>(R.id.btnProfile)
        val btnMap = findViewById<Button>(R.id.btnMap)
        val btnRating = findViewById<Button>(R.id.btnRating)

        progressBar = findViewById(R.id.progressBar)



        imgAvatar = findViewById(R.id.imgAvatar)



        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        prefManager = PreferencesManager(applicationContext) // класс, который работает с `SharedPreferences`
        // session manager
        session = SessionManager(applicationContext)

        if (!session.isLoggedIn) {
            logoutUser()
            FlurryAgent.onEndSession(this)
        }
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!
        getMyProfile()
        checkUpdateRating()
        getDataOfParties()

        val sPref: SharedPreferences = getPreferences(Context.MODE_PRIVATE)
        val checkShowInformationWindow = sPref.getString(showInformationPlayMarket, "0")
        if (checkShowInformationWindow == "0") {
            val builder = AlertDialog.Builder(this)
            builder.setMessage(R.string.message_info_play_market)
                    .setTitle(R.string.title_perf_for_play_market)
                    .setPositiveButton(R.string.good) { dialog, id -> }

            // Create the AlertDialog object and return it
            builder.create()
            builder.show()

            val editor = sPref.edit()
            editor.putString(showInformationPlayMarket, "1")
            editor.apply()
        }


        btnProfile.setOnClickListener {
            val intent = Intent(this@MainScreenActivity, MyProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnRating.setOnClickListener {
            val intent = Intent(this@MainScreenActivity, RatingActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnMap.setOnClickListener {
            val intent = Intent(this@MainScreenActivity, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }
        bp = BillingProcessor(applicationContext,
                InAppBillingResources.rsaKey, InAppBillingResources.merchantId, this)
        onPurchaseHistoryRestored()
        AdMobController.showBanner(this)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId
        if (id == R.id.action_shop) {
            val intent = Intent(this@MainScreenActivity, ShopActivity::class.java)
            startActivity(intent)
            finish()
        }
        if (id == R.id.action_about) {
            AppController.instance?.showDialogAboutUs(this@MainScreenActivity)
        }
        if (id == R.id.feeadback) {
            val intent = Intent(this@MainScreenActivity, FeedbackActivity::class.java)
            startActivity(intent)
            finish()
        }
        if (id == R.id.action_disabled_ads) {
            buyAds(this)
            createPurchase(detailsInfo)
        }
        if (id == R.id.action_exit) {
            logoutUser()
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            logoutUser()
            finish()
        } else {
            Toast.makeText(baseContext, "Нажми еще раз для выхода!",
                    Toast.LENGTH_SHORT).show()
        }
        back_pressed = System.currentTimeMillis()
    }


    private fun logoutUser() {
        if (session.isGuest) {
            session.setKeyIsGuest(false)
        } else {
            session.setLogin(false)
        }

        db = SQLiteHandler(applicationContext)
        db.deleteDataTokensTable()
        // Launching the login activity
        val intent = Intent(this@MainScreenActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun checkUpdateRating() {
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_check_update_rating_user"
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!
        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_CHECK_UPDATE_RATING, Response.Listener { response ->
            Log.d(TAG, "Данные обновления рейтинга: $response")
            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val checkUpdateRating = jObj.getBoolean("update_rating")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                val message = jObj.getString("message")

                // Check for error node in json
                if (!error) {
                    if (checkUpdateRating) {
                        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
                    }
                } else {

                    if (expAccessToken) {
                        updateDataTokens(tagRequest)
                    } else {
                        // Error in login. Get the error message
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()

            }
        }, Response.ErrorListener { error ->
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }


        }
        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagRequest)
    }


    private fun getDataOfParties() {
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_get_parties"
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!

        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_CHECK_SET_ANSWER, Response.Listener { response ->
            Log.d(TAG, "Данные получения информации об обновлении рейтинга: $response")
            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")


                if (!error) {
                    val member = jObj.getJSONObject("member")
                    val party = member.getJSONObject("party")
                    partyId = member.getString("id_party")
                    memberId = member.getString("id_user")
                    val nameParty = party.getString("title_party")
                    val nameCreatedUser = party.getString("created_name")
                    showRatingDialog(nameParty, nameCreatedUser, partyId, memberId)


                } else {
                    if (expAccessToken) {
                        updateDataTokens(tagRequest)
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
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }
        }

        if (CheckConnection.hasConnection(this@MainScreenActivity)) {
            AppController.instance?.addToRequestQueue(strReq, tagRequest)
        }
    }


    private fun showRatingDialog(nameParty: String, nameCreatedUser: String, partyId: String?, memberId: String?) {

        val ratingDialog = AlertDialog.Builder(this)
        ratingDialog.setIcon(android.R.drawable.btn_star_big_on)
        ratingDialog.setTitle("Информация")
        ratingDialog.setCancelable(false)
        ratingDialog.setMessage("Вы присутствовали на мероприятии '$nameParty' , теперь вы можете поставить оценку её создателю $nameCreatedUser. От -10 до 10.")

        val linearlayout = layoutInflater.inflate(R.layout.dialog_rating, null)
        ratingDialog.setView(linearlayout)
        rating = linearlayout.findViewById(R.id.rating)


        ratingDialog.setPositiveButton("Оценить"
        ) { dialog, which ->
            if (!rating.text.toString().isEmpty()) {
                // Toast.makeText(getApplicationContext(), "Ошибка: неверное значение рейтинга. " + (Integer.parseInt(rating.getText().toString()) > 10), Toast.LENGTH_LONG).show();
                updateRatingUser(partyId, memberId, rating.text.toString())
            } else {
                getDataOfParties()
                Toast.makeText(applicationContext, "Ошибка: неверное значение рейтинга. ", Toast.LENGTH_LONG).show()


            }
        }

                .setNegativeButton("Не оценивать"
                ) { dialog, id -> updateRatingUser(partyId, memberId, "0") }

        ratingDialog.create()
        ratingDialog.show()
    }

    private fun updateRatingUser(partyId: String?, memberId: String?, rating: String) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_update_rating_user"
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!

        val strReq = object : StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_RATING_USER + memberId!!, Response.Listener { response ->
            Log.d(TAG, "Данные получения информации об обновлении рейтинга 2: $response")
            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {
                    Toast.makeText(applicationContext, "Успешно: Рейтинг обновлен. Добавлено " + rating +
                            " единиц.", Toast.LENGTH_LONG).show()

                } else {
                    if (expAccessToken) {
                        updateDataTokens(tagRequest)
                    } else {
                        // Error in login. Get the error message
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()

            }
        }, Response.ErrorListener { error ->
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["party_id"] = partyId!!
                params["rating"] = rating
                params["access_token"] = accessToken
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }
        }
        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagRequest)
    }


    private fun getMyProfile() {
        progressBar.visibility = View.VISIBLE
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_get_data_of_my_profile"
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!
        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE, Response.Listener { response ->
            try {
                Log.d(TAG, "Токен действителен. ")
                Log.d(TAG, "Ответ сервера [Получение профиля] $response")
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")


                // Check for error node in json
                if (!error) {
                    progressBar.visibility = View.GONE
                    val user = jObj.getJSONObject("user")
                    val firstName = user.getString("first_name")
                    val nikName = user.getString("nik_name")
                    val image = user.getString("image")


                    Glide.with(applicationContext)
                            .load(image)
                            .placeholder(R.drawable.ic_profile)
                            .fitCenter()
                            .override(400, 400)
                            .dontAnimate()
                            .into(imgAvatar)

                    txtFirstName.text = firstName
                    txtNikName.text = nikName

                    val notes = jObj.getJSONObject("notes")

                    idNote = notes.getString("id")

                    val builder = AlertDialog.Builder(this@MainScreenActivity)
                    builder.setMessage(notes.getString("note"))
                            .setTitle("Информация")
                            .setPositiveButton(R.string.good) { dialog, id ->
                                Toast.makeText(applicationContext, idNote, Toast.LENGTH_LONG).show()
                                changeStatusNoteForUser(idNote)
                            }
                    // Create the AlertDialog object and return it
                    builder.create()
                    builder.show()

                } else {
                    if (expAccessToken) {
                        Log.d(TAG, "Токен истек. Обновляем токен доступа: $response")
                        updateDataTokens(tagRequest)
                    } else {
                        progressBar.visibility = View.GONE
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()

            }
        }, Response.ErrorListener { error ->
            progressBar.visibility = View.GONE
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }
        }
        AppController.instance?.addToRequestQueue(strReq, tagRequest)

    }

    private fun changeStatusNoteForUser(idNote: String?) {
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_update_status_note"
        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        accessToken = token["access_token"]!!
        val strReq = object : StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_NOTE + idNote!!, Response.Listener { response ->
            Log.d(TAG, "Ответ сервера [Получение профиля] $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                val message = jObj.getString("message")


                // Check for error node in json
                if (!error) {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                } else {
                    if (expAccessToken) {
                        updateDataTokens(tagRequest)
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
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["access_token"] = accessToken
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagRequest)
    }


    private fun updateDataTokens(tagRequest: String) {
        HttpsTrustManager.allowAllSSL()
        Log.d(TAG, "Обновляем токен")
        val tagRequestTokensRefresh = "req_update_data_tokens"
        db = SQLiteHandler(applicationContext)

        val dataTokens = db.dataTokens

        val refreshToken = dataTokens["refresh_token"]

        val strReq = object : StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, Response.Listener { response ->
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
                    Log.d(TAG, "Обновили таблицу")
                    db.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)

                    Log.d(TAG, "Получили новый токен:$accessToken")

                    when (tagRequest) {
                        "req_get_data_of_my_profile" -> getMyProfile()
                        "req_get_parties" -> getDataOfParties()
                        "req_update_rating_user" -> updateRatingUser(partyId, memberId, rating.text.toString())
                        "req_update_status_note" -> changeStatusNoteForUser(idNote)

                        "req_check_update_rating_user" -> checkUpdateRating()
                        "req_create_purchase" -> createPurchase(detailsInfo)
                    }

                } else {
                    // Error in login. Get the error message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                }

            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()

            }
        }, Response.ErrorListener { error ->
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["refresh_token"] = refreshToken!!

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagRequestTokensRefresh)
    }

    //Работа с покупаками

    private fun buyAds(activity: Activity) {
        bp.loadOwnedPurchasesFromGoogle()
        if (!bp.isPurchased(skU_Disable_Ads)) {
            bp.purchase(activity, skU_Disable_Ads)
        } else {
            Toast.makeText(applicationContext,
                    "Покупка уже осуществлена.",
                    Toast.LENGTH_LONG).show()
        }
    }

    // перезагружаем приложение
    private fun restartApp() {
        val rIntent = applicationContext.packageManager.getLaunchIntentForPackage(application.packageName)
        if (rIntent != null) {
            rIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            applicationContext.startActivity(rIntent)
        }
    }

    // ... другие методы класса
    // [START billing part of class]
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data)

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        // Called when requested PRODUCT ID was successfully purchased
        // Вызывается, когда запрашиваемый PRODUCT ID был успешно куплен
        Log.d(TAG, "Ответ сервера Google [Покупка товара]" + details!!)

        if (bp.isPurchased(productId)) {
            detailsInfo = details
            createPurchase(detailsInfo)
            PreferencesManager.adsStatus = false // 1. записываем в `SharedPreferences` состояние рекламы (ВЫКЛ / false)
            restartApp() // 3. перезагружаем приложение
        }


    }

    private fun createPurchase(details: TransactionDetails?) {
        HttpsTrustManager.allowAllSSL()
        val tagRequest = "req_create_purchase"


        db = SQLiteHandler(applicationContext)

        val token = db.dataTokens
        val accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_CREATE_PURCHASE, Response.Listener { response ->
            Log.d(TAG, "Ответ сервара [Покупка товара]$response")

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                val message = jObj.getString("message")


                // Check for error node in json
                if (!error) {
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                } else {
                    if (expAccessToken) {
                        updateDataTokens(tagRequest)
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
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["access_token"] = accessToken!!
                params["token"] = details!!.purchaseInfo.purchaseData.purchaseToken
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tagRequest)
    }


    override fun onPurchaseHistoryRestored() {
        //Вызывается, когда история покупки была восстановлена,
        // и список всех принадлежащих идентификаторы продуктов был загружен из Google Play
        // так Вы сможете НУЖНУЮ покупку проверить
        bp.loadOwnedPurchasesFromGoogle()
        if (bp.isPurchased(skU_Disable_Ads)) {  // true - куплено
            // пишем в `SharedPreferences`, что отключили рекламу
            PreferencesManager.adsStatus = false
        } else if (!PreferencesManager.adsStatus) {
            restartApp() // 3. перезагружаем приложение
            PreferencesManager.adsStatus = true
        }

    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        // Вызывается, когда появляется ошибка. См. константы класса
        // для получения более подробной информации
    }

    override fun onBillingInitialized() {
        bp.loadOwnedPurchasesFromGoogle()
    }


    companion object {
        private var back_pressed: Long = 0

        private val TAG = MainScreenActivity::class.java.simpleName
    }

}