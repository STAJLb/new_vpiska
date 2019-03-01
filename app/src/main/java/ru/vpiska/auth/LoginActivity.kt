package ru.vpiska.auth

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject
import ru.vpiska.MainScreenActivity
import ru.vpiska.R
import ru.vpiska.app.AppConfig
import ru.vpiska.app.AppController
import ru.vpiska.app.HttpsTrustManager
import ru.vpiska.helper.CheckConnection
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.profile.GuestProfileActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*

class LoginActivity : AppCompatActivity() {


    private lateinit var inputNikName: EditText
    private lateinit var inputPassword: EditText

    private var pDialog: ProgressDialog? = null
    private lateinit var session: SessionManager
    private lateinit var db: SQLiteHandler
    public override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        inputNikName = findViewById(R.id.nik_name)
        inputPassword = findViewById(R.id.password)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnLinkToRegister = findViewById<Button>(R.id.btnLinkToRegisterScreen)
        val btnGuest = findViewById<Button>(R.id.btnGuest)

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)


        db = SQLiteHandler(applicationContext)


        session = SessionManager(applicationContext)



        if (session.isGuest) {
            val intent = Intent(this@LoginActivity, GuestProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
        if (session.isLoggedIn) {

            val intent = Intent(this@LoginActivity, MainScreenActivity::class.java)
            startActivity(intent)
            finish()
        }


        btnLogin.setOnClickListener {
            val nikName = inputNikName.text.toString().trim { it <= ' ' }
            val password = inputPassword.text.toString().trim { it <= ' ' }

            // Check for empty data in the form
            if (!nikName.isEmpty() && !password.isEmpty()) {
                if (CheckConnection.hasConnection(this@LoginActivity)) {
                    checkLogin(nikName, password)
                }
            } else {
                // Prompt user to enter credentials
                Toast.makeText(applicationContext,
                        "Проверьте правильность данных", Toast.LENGTH_LONG)
                        .show()
            }
        }

        // Link to Register Screen
        btnLinkToRegister.setOnClickListener {
            val i = Intent(applicationContext,
                    RegisterActivity::class.java)
            startActivity(i)
            finish()
        }

        btnGuest.setOnClickListener {
            // val deviceUniqueIdentifier = id(this@LoginActivity)
            startGuestSession(id(applicationContext))
        }

    }

    private var uniqueID: String? = null
    private val prefUniqueId = "PREF_UNIQUE_ID"

    @Synchronized
    fun id(context: Context): String {
        if (uniqueID == null) {
            val sharedPrefs = context.getSharedPreferences(
                    prefUniqueId, Context.MODE_PRIVATE)
            uniqueID = sharedPrefs.getString(prefUniqueId, null)
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString()
                val editor = sharedPrefs.edit()
                editor.putString(prefUniqueId, uniqueID)
                editor.apply()
            }
        }
        return uniqueID!!
    }


    override fun onBackPressed() {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed()
        else
            Toast.makeText(baseContext, "Нажми еще раз для выхода!",
                    Toast.LENGTH_SHORT).show()
        back_pressed = System.currentTimeMillis()
    }

    private fun startGuestSession(deviceUniqueIdentifier: String) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()

        pDialog!!.setMessage("Готовим гостевой аккаунт  ...")
        showDialog()

        val strReq = object : StringRequest(Method.POST, AppConfig.URL_REGISTER, Response.Listener { response ->
            Log.d(TAG, "Ошибка авторизации: $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {
                    // user successfully logged in
                    // Create login session
                    session.setKeyIsGuest(true)
                    val dataTokens = jObj.getJSONObject("data_tokens")
                    // Now store the user in SQLite
                    val accessToken = dataTokens.getString("access_token")
                    val refreshToken = dataTokens.getString("refresh_token")
                    val expAccessToken = dataTokens.getString("exp_access_token")
                    val expRefreshToken = dataTokens.getString("exp_refresh_token")
                    db.deleteDataTokensTable()
                    // Inserting row in users table

                    db.addDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)

                    // Launch main activity
                    val intent = Intent(this@LoginActivity, GuestProfileActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Error in login. Get the error message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                }
                hideDialog()
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()
                Toast.makeText(applicationContext, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Login Error: " + error.message)
            Toast.makeText(applicationContext, "Ошибка: " + error.message, Toast.LENGTH_LONG).show()
            hideDialog()
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["type"] = "guest"
                params["imei"] = deviceUniqueIdentifier


                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq)
    }

    private fun checkLogin(nikName: String, password: String) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_login"

        pDialog!!.setMessage("Идет авторизация  ...")
        showDialog()

        val strReq = object : StringRequest(Method.POST, AppConfig.URL_LOGIN, Response.Listener { response ->
            Log.d(TAG, "Ошибка авторизации: $response")


            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")

                // Check for error node in json
                if (!error) {
                    // user successfully logged in
                    // Create login session
                    session.setLogin(true)
                    val dataTokens = jObj.getJSONObject("data_tokens")
                    // Now store the user in SQLite
                    val accessToken = dataTokens.getString("access_token")
                    val refreshToken = dataTokens.getString("refresh_token")
                    val expAccessToken = dataTokens.getString("exp_access_token")
                    val expRefreshToken = dataTokens.getString("exp_refresh_token")
                    db.deleteDataTokensTable()
                    // Inserting row in users table

                    db.addDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)


                    // Launch main activity
                    val intent = Intent(this@LoginActivity, MainScreenActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Error in login. Get the error message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext, "Ошибка: $errorMsg", Toast.LENGTH_LONG).show()
                }
                hideDialog()
            } catch (e: JSONException) {
                // JSON error
                e.printStackTrace()
                Toast.makeText(applicationContext, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Login Error: " + error.message)
            Toast.makeText(applicationContext, "Ошибка: " + error.message, Toast.LENGTH_LONG).show()
            hideDialog()
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["type"] = "auth"
                params["nik_name"] = nikName
                params["password"] = password

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq)
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

        private val TAG = LoginActivity::class.java.simpleName
        private var back_pressed: Long = 0


        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }


        private lateinit var sID: String
        private const val INSTALLATION = "INSTALLATION"

        @Synchronized
        private fun id(context: Context): String {

            val installation = File(context.filesDir, INSTALLATION)
            try {
                if (!installation.exists())
                    writeInstallationFile(installation)
                readInstallationFile(installation)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }


            return sID
        }

        @Throws(IOException::class)
        private fun readInstallationFile(installation: File): String {
            val f = RandomAccessFile(installation, "r")
            val bytes = ByteArray(f.length().toInt())
            f.readFully(bytes)
            f.close()
            return String(bytes)
        }

        @Throws(IOException::class)
        private fun writeInstallationFile(installation: File) {
            val out = FileOutputStream(installation)
            val id = UUID.randomUUID().toString()
            out.write(id.toByteArray())
            out.close()
        }
    }
}
