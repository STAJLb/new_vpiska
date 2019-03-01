package ru.vpiska.auth


import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.*
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
import ru.vpiska.helper.SessionManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*


class RegisterActivity : AppCompatActivity() {


    private var inputFirstName: EditText? = null
    private var inputNikName: EditText? = null
    private var inputPassword: EditText? = null


    private var txtAge: TextView? = null

    private var radioGroup: RadioGroup? = null

    private var pDialog: ProgressDialog? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)


        val txtOkContract = findViewById<TextView>(R.id.txtOkContract)
        txtAge = findViewById(R.id.txtAge)

        inputFirstName = findViewById(R.id.first_name)
        inputNikName = findViewById(R.id.nik_name)
        inputPassword = findViewById(R.id.password)

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnLinkToLogin = findViewById<Button>(R.id.btnLinkToLoginScreen)
        val btnInputAge = findViewById<Button>(R.id.inputAge)


        val checkContrakt = findViewById<CheckBox>(R.id.checkContrakt)
        radioGroup = findViewById(R.id.radioGroup)

        // Progress dialog
        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)

        // Session manager
        val session = SessionManager(applicationContext)

        // Check if user is already logged in or not
        if (session.isLoggedIn) {
            // User is already logged in. Take him to main activity
            val intent = Intent(this@RegisterActivity,
                    MainScreenActivity::class.java)
            startActivity(intent)
            finish()
        }

        txtOkContract.setOnClickListener {
            val intent = Intent(this@RegisterActivity, AgreementActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Register Button Click event
        btnRegister.setOnClickListener {
            if (CheckConnection.hasConnection(this@RegisterActivity)) {
                sendRegisterRequset()
            }
        }

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener {
            val i = Intent(applicationContext,
                    LoginActivity::class.java)
            startActivity(i)
            finish()
        }

        btnInputAge.setOnClickListener {
            val dateDialog = ru.vpiska.helper.DatePicker()
            dateDialog.show(supportFragmentManager, "datePicker")
        }

    }


    private fun sendRegisterRequset() {
        val deviceUniqueIdentifier = id(this)
        val firstName = inputFirstName!!.text.toString().trim { it <= ' ' }
        val nikName = inputNikName!!.text.toString().trim { it <= ' ' }
        val password = inputPassword!!.text.toString().trim { it <= ' ' }
        val age = txtAge!!.text.toString().trim { it <= ' ' }
        var sex: String? = null

        val checkedRadioButtonId = radioGroup!!.checkedRadioButtonId
        when (checkedRadioButtonId) {
            R.id.rbMan -> sex = "m"
            R.id.rbWoman -> sex = "w"
        }


        if (!firstName.isEmpty() && !nikName.isEmpty() && !password.isEmpty()) {
            registerUser(firstName, nikName, password, sex, deviceUniqueIdentifier, age)
        } else {
            Toast.makeText(applicationContext,
                    "Пожалуйста, заполните все поля!", Toast.LENGTH_LONG)
                    .show()
        }
    }


    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     */
    private fun registerUser(firstName: String, nikName: String,
                             password: String, sex: String?, imei: String, age: String) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_register"

        pDialog!!.setMessage("Идет регистрация ...")
        showDialog()

        val strReq = object : StringRequest(Method.POST,
                AppConfig.URL_REGISTER, Response.Listener { response ->
            Log.d(TAG, "Register Response: $response")
            hideDialog()

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                if (!error) {

                    Toast.makeText(applicationContext, "Регистрация прошла успешно. Пожалуйста, войдите.", Toast.LENGTH_LONG).show()

                    // Launch login activity
                    val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {

                    // Error occurred in registration. Get the error
                    // message
                    val errorMsg = jObj.getString("error_msg")
                    Toast.makeText(applicationContext,
                            errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, "Ошибка: " + e.message, Toast.LENGTH_LONG).show()
            }
        }, Response.ErrorListener { error ->
            Log.e(TAG, "Registration Error: " + error.message)
            Toast.makeText(applicationContext,
                    error.message, Toast.LENGTH_LONG).show()
        }) {

            override fun getParams(): Map<String, String> {
                // Posting params to register url
                val params = HashMap<String, String>()
                params["type"] = "auth"
                params["first_name"] = firstName
                params["nik_name"] = nikName
                params["password"] = password
                params["sex"] = sex!!
                params["age"] = age
                params["imei"] = imei


                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)

    }

    override fun onBackPressed() {
        val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
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
        private val TAG = RegisterActivity::class.java.simpleName

        private lateinit var sID: String
        private const val INSTALLATION = "INSTALLATION"

        @Synchronized
        private fun id(context: Context): String {
                val installation = File(context.filesDir, INSTALLATION)
                try {
                    if (!installation.exists())
                        writeInstallationFile(installation)
                    sID = readInstallationFile(installation)
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