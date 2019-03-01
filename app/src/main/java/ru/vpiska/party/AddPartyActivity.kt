package ru.vpiska.party


import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
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
import ru.vpiska.helper.maps.DatePicker
import ru.vpiska.helper.maps.TimePicker
import ru.vpiska.map.MapsActivity
import java.util.*


class AddPartyActivity : AppCompatActivity() {


    private var session: SessionManager? = null

    private var inputTitleParty: EditText? = null
    private var inputDescriptionParty: EditText? = null
    private var inputCountPeople: EditText? = null
    private var inputAddressParty: EditText? = null

    private var txtDate: TextView? = null
    private var txtTime: TextView? = null

    private var inputAlcohol: Switch? = null

    private var spinner: Spinner? = null


    private var pDialog: ProgressDialog? = null


    private var db: SQLiteHandler? = null

    private var accessToken: String? = null


    private var titleParty: String? = null
    private var descriptionParty: String? = null
    private var addressParty: String? = null
    private var coordinatesParty: String? = null
    private var countPeopleParty: String? = null
    private var alcoholParty: String? = null
    private var dateTimeParty: String? = null
    private var typeParty: String? = null


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_party)


        val btnAddParty = findViewById<Button>(R.id.btnAddParty)
        val btnInputDate = findViewById<Button>(R.id.inputDate)
        val btnInputTime = findViewById<Button>(R.id.inputTime)

        inputTitleParty = findViewById(R.id.title_party)
        inputDescriptionParty = findViewById(R.id.description_party)
        inputCountPeople = findViewById(R.id.count_people)
        inputAddressParty = findViewById(R.id.address_party)

        txtDate = findViewById(R.id.txtDate)
        txtTime = findViewById(R.id.txtTime)

        spinner = findViewById(R.id.spinner)
        inputAlcohol = findViewById(R.id.alcohol)

        // Progress dialog
        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)
        val adapter = ArrayAdapter.createFromResource(this, R.array.types_parties, R.layout.spinner_item)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        spinner!!.adapter = adapter


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)


        // SqLite database handler
        db = SQLiteHandler(applicationContext)

        // session manager
        session = SessionManager(applicationContext)


        val builder = AlertDialog.Builder(this)
        builder.setMessage("При создании квартирника, отмечайте конкретный адрес с указанием дома, этажа и квартиры. ")
                .setTitle("Информация")
                .setPositiveButton(R.string.good) { dialog, id -> }

        // Create the AlertDialog object and return it
        builder.create()
        builder.show()



        btnAddParty.setOnClickListener {
            if (session!!.isGuest) {

                val builder = AlertDialog.Builder(this@AddPartyActivity)
                builder.setMessage("Для создания события необходимо пройти регистрацию.")
                        .setTitle("Информация")
                        .setPositiveButton("Пройти регистрацию"

                        ) { dialog, id ->
                            session!!.setKeyIsGuest(false)

                            db!!.deleteDataTokensTable()

                            // Launching the login activity
                            val intent = Intent(this@AddPartyActivity, LoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        }.setNegativeButton("Позже") { dialog, which -> }

                // Create the AlertDialog object and return it
                builder.create()
                builder.show()
            } else if (CheckConnection.hasConnection(this@AddPartyActivity)) {
                formatArrayDataToSend()
            }
        }
        btnInputDate.setOnClickListener {
            val dateDialog = DatePicker()
            dateDialog.show(supportFragmentManager, "datePicker")
        }
        btnInputTime.setOnClickListener {
            val dateDialog = TimePicker()
            dateDialog.show(supportFragmentManager, "timePicker")
        }

        AdMobController.showBanner(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (session!!.isGuest) {
            menuInflater.inflate(R.menu.menu_guest, menu)
        } else {
            menuInflater.inflate(R.menu.menu_main, menu)
        }

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
        if (session!!.isGuest) {
            session!!.setKeyIsGuest(false)
        } else {
            session!!.setLogin(false)
        }


        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@AddPartyActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val intent = Intent(this@AddPartyActivity, MapsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun formatArrayDataToSend() {
        titleParty = inputTitleParty!!.text.toString().trim { it <= ' ' }
        descriptionParty = inputDescriptionParty!!.text.toString().trim { it <= ' ' }
        addressParty = inputAddressParty!!.text.toString().trim { it <= ' ' }
        coordinatesParty = intent.extras!!.getString("latitudeMarker") + " " + intent.extras!!.getString("longitudeMarker")
        countPeopleParty = inputCountPeople!!.text.toString().trim { it <= ' ' }
        alcoholParty = if (inputAlcohol!!.isChecked) "1" else "0"
        dateTimeParty = txtDate!!.text.toString() + " " + txtTime!!.text
        typeParty = Integer.toString(spinner!!.selectedItemPosition)

        sendDataParty(titleParty, descriptionParty, addressParty, coordinatesParty, countPeopleParty, alcoholParty, dateTimeParty, typeParty)
    }


    private fun sendDataParty(titleParty: String?, descriptionParty: String?, addressParty: String?, coordinates: String?, countPeople: String?, alcohol: String?, dateTime: String?, typeParty: String?) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_add_party"

        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        accessToken = token["access_token"]

        pDialog!!.setMessage("Идет отправка данных  ...")
        showDialog()

        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_ADD_PARTY, Response.Listener { response ->
            Log.d(TAG, "Ошибка авторизации: $response")
            hideDialog()

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")

                // Check for error node in json
                if (!error) {

                    // Launch main activity
                    val intent = Intent(this@AddPartyActivity, MapsActivity::class.java)
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
            hideDialog()
        }) {

            override fun getParams(): Map<String, String> {
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)
                params["access_token"] = accessToken!!
                params["title_party"] = titleParty!!
                params["description_party"] = descriptionParty!!
                params["address_party"] = addressParty!!
                params["coordinates"] = coordinates!!
                params["count_people"] = countPeople!!
                params["alcohol"] = alcohol!!
                params["date_time"] = dateTime!!
                params["type_party"] = typeParty!!

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
                        "req_add_party" -> sendDataParty(titleParty, descriptionParty, addressParty, coordinatesParty, countPeopleParty, alcoholParty, dateTimeParty, typeParty)
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
        private val TAG = AddPartyActivity::class.java.simpleName
    }
}
