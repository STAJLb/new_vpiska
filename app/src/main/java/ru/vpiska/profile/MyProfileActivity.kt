package ru.vpiska.profile

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.bumptech.glide.Glide
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
import ru.vpiska.helper.DatePicker
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import java.util.*


/**
 * Created by Кирилл on 10.11.2017.
 */

class MyProfileActivity : AppCompatActivity() {

    private var txtFirstName: EditText? = null
    private var txtNikName: EditText? = null

    private var pDialog: ProgressDialog? = null
    private var imgAvatar: ImageView? = null
    private var radioGroup: RadioGroup? = null
    private var txtAge: TextView? = null
    private var txtBalance: TextView? = null


    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null

    private var accessToken: String? = null

    private var firstName: String? = null
    private var nikName: String? = null
    private var sex: String? = null
    private var age: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_profile)

        txtFirstName = findViewById(R.id.first_name)
        txtNikName = findViewById(R.id.nik_name)


        imgAvatar = findViewById(R.id.imgAvatar)
        radioGroup = findViewById(R.id.radioGroup)
        txtAge = findViewById(R.id.txtAge)
        txtBalance = findViewById(R.id.txtBalance)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        val btnUpdate = findViewById<Button>(R.id.btnUpdate)
        val btnChangeAvatar = findViewById<Button>(R.id.btnChangeAvatar)
        val btnInputAge = findViewById<Button>(R.id.inputAge)

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)

        session = SessionManager(applicationContext)

        getMyProfile()

        btnUpdate.setOnClickListener {
            firstName = txtFirstName!!.text.toString().trim { it <= ' ' }
            nikName = txtNikName!!.text.toString().trim { it <= ' ' }
            val checkedRadioButtonId = radioGroup!!.checkedRadioButtonId
            sex = null
            when (checkedRadioButtonId) {
                R.id.rbMan -> sex = "m"
                R.id.rbWoman -> sex = "w"
            }
            age = txtAge!!.text.toString().trim { it <= ' ' }
            updateUser(firstName, nikName, sex, age)
        }

        btnChangeAvatar.setOnClickListener {
            val intent = Intent(this@MyProfileActivity, UploadImageActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnInputAge.setOnClickListener {
            val dateDialog = DatePicker()
            dateDialog.show(supportFragmentManager, "datePicker")
        }

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

    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@MyProfileActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val intent = Intent(this@MyProfileActivity, MainScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun getMyProfile() {
        pDialog!!.setMessage("Получение данных профиля ...")
        showDialog()
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_get_data_of_my_profile"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.GET, AppConfig.URL_GET_PROFILE, Response.Listener { response ->
            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {
                    hideDialog()
                    val user = jObj.getJSONObject("user")
                    val firstName = user.getString("first_name")
                    val nikName = user.getString("nik_name")
                    val image = user.getString("image")
                    val age = user.getString("age")
                    val sex = user.getString("sex")
                    val balance = user.getString("balance")

                    txtFirstName!!.setText(firstName)
                    txtNikName!!.setText(nikName)
                    txtAge!!.text = age
                    txtBalance!!.text = balance

                    when (sex) {
                        "m" -> radioGroup!!.check(R.id.rbMan)
                        "w" -> radioGroup!!.check(R.id.rbWoman)
                        else -> radioGroup!!.check(R.id.rbMan)
                    }


                    Glide.with(applicationContext)
                            .load(image)
                            .placeholder(R.drawable.ic_profile)
                            .fitCenter()
                            .override(400, 400)
                            .dontAnimate()
                            .into(imgAvatar!!)


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
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Content-Type"] = "application/json; charset=UTF-8"
                params["Access-Token"] = accessToken!!
                params["Version"] = Integer.toString(BuildConfig.VERSION_CODE)
                return params
            }
        }
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)

    }

    private fun updateUser(firstName: String?, nikName: String?, sex: String?, age: String?) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_update"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]

        pDialog!!.setMessage("Обновление ...")
        showDialog()

        val strReq = object : StringRequest(Request.Method.POST,
                AppConfig.URL_UPDATE_USER, Response.Listener { response ->
            Log.d(TAG, "Register Response: $response")
            hideDialog()

            try {

                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")
                if (!error) {

                    Toast.makeText(applicationContext, "Данные обновлены.", Toast.LENGTH_LONG).show()

                    // Launch login activity
                    val intent = Intent(this@MyProfileActivity, MainScreenActivity::class.java)
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

            override fun getParams(): Map<String, String> {
                // Posting params to register url
                val params = HashMap<String, String>()
                params["first_name"] = firstName!!
                params["nik_name"] = nikName!!
                params["sex"] = sex!!
                params["age"] = age!!
                params["access_token"] = accessToken!!
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)


                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)

    }


    private fun updateDataTokens(tag: String) {
        HttpsTrustManager.allowAllSSL()

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

                    db!!.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)



                    when (tag) {
                        "req_get_data_of_my_profile" -> getMyProfile()
                        "req_update" -> updateUser(firstName, nikName, sex, age)
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


        private val TAG = MyProfileActivity::class.java.simpleName
    }


}
