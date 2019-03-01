package ru.vpiska.profile


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.BuildConfig
import ru.vpiska.MainScreenActivity
import ru.vpiska.R
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.helper.SQLiteHandler
import java.util.*

class FeedbackActivity : AppCompatActivity() {


    private var txtFeedback: TextView? = null

    private var accessToken: String? = null

    private var db: SQLiteHandler? = null

    private var feedback: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)


        val btnAddReport = findViewById<Button>(R.id.btnSendFeedBack)

        db = SQLiteHandler(applicationContext)


        btnAddReport.setOnClickListener {
            txtFeedback = findViewById(R.id.feeadback)

            feedback = txtFeedback!!.text.toString()

            sendFeedback(feedback)
        }

        MobileAds.initialize(this, "ca-app-pub-6595506155906957/5953597740")

        val adView = findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun sendFeedback(feedback: String?) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_send_feedback"
        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]


        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_ADD_FEEDBACK, Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {
                    Toast.makeText(applicationContext, "Отзыв добавлен.", Toast.LENGTH_LONG).show()
                    // Launch main activity
                    val intent = Intent(this@FeedbackActivity, MainScreenActivity::class.java)
                    startActivity(intent)
                    finish()
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
                params["feedback"] = feedback!!
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
                        "req_send_feedback" -> sendFeedback(feedback)
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

        private val TAG = FeedbackActivity::class.java.simpleName
    }
}
