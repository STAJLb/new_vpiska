package ru.vpiska.party


import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.R
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.map.MapsActivity
import java.util.*

class AddReportActivity : AppCompatActivity() {

    private var spinner: Spinner? = null

    private var txtDescriptionReport: TextView? = null

    private var accessToken: String? = null

    private var db: SQLiteHandler? = null

    private var descriptionReport: String? = null
    private var reason: String? = null
    private var partyId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_report)

        spinner = findViewById(R.id.spinner)
        val btnAddReport = findViewById<Button>(R.id.btnAddReport)

        db = SQLiteHandler(applicationContext)


        btnAddReport.setOnClickListener {
            partyId = intent.extras!!.getString("party_id")

            txtDescriptionReport = findViewById(R.id.description_report)

            descriptionReport = txtDescriptionReport!!.text.toString()
            reason = spinner!!.selectedItem.toString()
            addReport(descriptionReport, reason, partyId)
        }
        val adapter = ArrayAdapter.createFromResource(this, R.array.reasons_report, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner!!.adapter = adapter

        MobileAds.initialize(this, "ca-app-pub-6595506155906957/5953597740")

        val adView = findViewById<com.google.android.gms.ads.AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    private fun addReport(descriptionReport: String?, reason: String?, partyId: String?) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_add_report"
        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]


        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_ADD_REPORT, Response.Listener { response ->
            Log.d(TAG, "Ошибка авторизации: $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {
                    Toast.makeText(applicationContext, "Репорт добавлен.", Toast.LENGTH_LONG).show()
                    // Launch main activity
                    val intent = Intent(this@AddReportActivity, MapsActivity::class.java)
                    startActivity(intent)
                    finish()
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
                params["access_token"] = accessToken!!
                params["description_report"] = descriptionReport!!
                params["reason"] = reason!!
                params["party_id"] = partyId!!
                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    companion object {

        private val TAG = AddReportActivity::class.java.simpleName
    }
}
