package ru.vpiska.profile

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.BuildConfig
import ru.vpiska.R
import ru.vpiska.app.AdMobController
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.party.PartyActivity
import ru.vpiska.rating.RatingActivity
import java.util.*
import android.R.id.edit






class OtherProfileActivity : AppCompatActivity() {

    private var txtFirstName: TextView? = null
    private var txtNikName: TextView? = null
    private var txtRating: TextView? = null

    private var imgAvatar: ImageView? = null

    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null

    private var pDialog: ProgressDialog? = null

    private var accessToken: String? = null

    private var idUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profile)

        txtFirstName = findViewById(R.id.first_name)
        txtNikName = findViewById(R.id.nik_name)
        txtRating = findViewById(R.id.rating)
        imgAvatar = findViewById(R.id.imgAvatar)

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)

        db = SQLiteHandler(applicationContext)
        session = SessionManager(applicationContext)

        idUser = intent.extras!!.getString("id_user")

        getDataOfOtherParty(Integer.parseInt(idUser))


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

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
        if (intent.extras!!.getString("id_party") != null) {
            val intent = Intent(this@OtherProfileActivity, PartyActivity::class.java)
            intent.putExtra("id_party", getIntent().extras!!.getString("id_party"))
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this@OtherProfileActivity, RatingActivity::class.java)

            startActivity(intent)
            finish()
        }


    }

    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@OtherProfileActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getDataOfOtherParty(idUser: Int) {
        pDialog!!.setMessage("Получение данных профиля ...")
        showDialog()
        HttpsTrustManager.allowAllSSL()

        val tag_string_req = "req_get_created_party_user"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE, Response.Listener { response ->
            Log.d(TAG, "Ошибка получения createdUserParty: $response")
            try {
                hideDialog()

                val jObj = JSONObject(response)
                val user = jObj.getJSONObject("user")
                val firstName = user.getString("first_name")
                val nikName = user.getString("nik_name")
                val rating = user.getString("rating")
                val image = user.getString("image")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {
                    txtFirstName!!.text = firstName
                    txtNikName!!.text = nikName
                    txtRating!!.text = rating


                    Glide.with(this@OtherProfileActivity)
                            .load(image)
                            .fitCenter()
                            .placeholder(R.drawable.ic_profile)
                            .override(400, 400)
                            .dontAnimate()
                            .into(imgAvatar!!)


                } else {
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
                params["Uid"] = Integer.toString(idUser)
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

                    when ("req_get_created_party_user") {
                        "req_get_data_of_my_profile" -> getDataOfOtherParty(Integer.parseInt(idUser!!))
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

        private val TAG = OtherProfileActivity::class.java.simpleName
    }
}
