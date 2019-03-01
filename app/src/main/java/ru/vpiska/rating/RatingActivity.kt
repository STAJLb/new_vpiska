package ru.vpiska.rating

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
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
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.profile.GuestProfileActivity
import java.util.*





class RatingActivity : AppCompatActivity() {

    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null

    private var accessToken: String? = null

    private var ratings = ArrayList<Rating>()


    private var ratingList: ListView? = null
    private var pDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ru.vpiska.R.layout.activity_rating)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)
        db = SQLiteHandler(applicationContext)


        session = SessionManager(applicationContext)

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog?.setCancelable(true)
        getDataOfRating()


        AdMobController.showBanner(this)

    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
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

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (session!!.isGuest) {
            val intent = Intent(this@RatingActivity, GuestProfileActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this@RatingActivity, MainScreenActivity::class.java)
            startActivity(intent)
            finish()
        }

    }

    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@RatingActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getDataOfRating() {
        pDialog!!.setMessage("Получение данных рейтинга ...")
        showDialog()
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_get_data_of_rating"


        val token = db!!.dataTokens
        accessToken = token["access_token"]

        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_DATA_OF_RATING, Response.Listener { response ->
            Log.d(TAG, "Данные таблицы рейтинга $response")


            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                ratingList = findViewById(R.id.lvRating)

                if (!error) {
                    hideDialog()
                    val arr = jObj.getJSONArray("rating")
                    for (i in 0 until arr.length()) {
                        ratings.add(Rating(arr.getJSONObject(i).getString("first_name"), arr.getJSONObject(i).getString("rating"),
                                arr.getJSONObject(i).getString("id")))
                    }
                    val adapter = RatingAdapter(applicationContext, R.layout.list_item_rating, ratings)
                    ratingList!!.adapter = adapter


                } else {
                    hideDialog()
                    if (expAccessToken) {
                        updateDataTokens()
                    } else {
                        val errorMsg = jObj.getString("error_msg")
                        Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                    }

                }
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()
                hideDialog()
                Toast.makeText(applicationContext, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            hideDialog()
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {
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


    private fun updateDataTokens() {
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

                    when ("req_get_data_of_rating") {
                        "req_get_data_of_rating" -> getDataOfRating()
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
            if (error is TimeoutError) {
                Toast.makeText(applicationContext,
                        "Серверная ошибка.",
                        Toast.LENGTH_LONG).show()
            }
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                assert(refreshToken != null)
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

        private val TAG = RatingActivity::class.java.simpleName
    }
}
