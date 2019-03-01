package ru.vpiska.shop


import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.*
import android.widget.*
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.TimeoutError
import com.android.volley.toolbox.StringRequest
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
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
import ru.vpiska.helper.InAppBillingResources
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import java.util.*


class ShopActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {

    private var session: SessionManager? = null
    private var db: SQLiteHandler? = null

    private var addingBalance: String? = null
    private var bp: BillingProcessor? = null
    private var pDialog: ProgressDialog? = null

    private var transactionDetails: TransactionDetails? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shops)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        db = SQLiteHandler(applicationContext)

        pDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        pDialog!!.setCancelable(false)
        val tabHost = findViewById<TabHost>(android.R.id.tabhost)
        // инициализация
        tabHost.setup()

        var tabSpec: TabHost.TabSpec = tabHost.newTabSpec("tag1")
        tabSpec.setIndicator("Монеты")
        tabSpec.setContent(R.id.tab1)
        tabHost.addTab(tabSpec)

        tabSpec = tabHost.newTabSpec("tag2")
        tabSpec.setIndicator("Контент")
        tabSpec.setContent(R.id.tab2)
        tabHost.addTab(tabSpec)

        val tw = tabHost.findViewById<TabWidget>(android.R.id.tabs)
        for (i in 0 until tw.childCount) {
            val tabView = tw.getChildTabViewAt(i)
            val tv = tabView.findViewById<TextView>(android.R.id.title)
            tv.setTextColor(Color.WHITE)
            tv.textSize = 12f
        }

        tabHost.setCurrentTabByTag("tag1")


        val shopList = findViewById<ListView>(R.id.lvShopApp)
        val shops = ArrayList<ShopApp>()
        shops.add(ShopApp("1", "Купить 10 просмотров событий", 100.0))
        shops.add(ShopApp("2", "Купить 1 добавление события", 100.0))

        Collections.sort(shops, ShopApp.ShopComparator)
        val adapter = ShopAppAdapter(applicationContext, shops)
        shopList.adapter = adapter


        bp = BillingProcessor(applicationContext,
                InAppBillingResources.rsaKey, InAppBillingResources.merchantId, this) // инициализируем `BillingProcessor`. В документации на `GitHub` сказано, что для защиты от липовых покупок через приложения типа `freedom` необходимо в конструктор `BillingProcessor`'а передать еще и свой `MERCHANT_ID`. Где его взять - внизу текущего ответа опишу шаги
        bp!!.initialize()
        session = SessionManager(applicationContext)

        AdMobController.showBanner(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (!bp!!.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data)

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
        val intent = Intent(this@ShopActivity, MainScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun logoutUser() {
        session!!.setLogin(false)

        db!!.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@ShopActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onPurchaseHistoryRestored() {
        //Вызывается, когда история покупки была восстановлена,
        // и список всех принадлежащих идентификаторы продуктов был загружен из Google Play
        // так Вы сможете НУЖНУЮ покупку проверить


    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        transactionDetails = details
        if (bp!!.isInitialized) {
            if (bp!!.isPurchased(productId)) {
                when (productId) {
                    "100_money" -> {
                        addingBalance = "100"
                        updateBalance(transactionDetails, "100")
                    }
                    "200_money" -> {
                        addingBalance = "200"
                        updateBalance(transactionDetails, "200")
                    }
                    "560_money" -> {
                        addingBalance = "560"
                        updateBalance(transactionDetails, "560")
                    }
                    "1100_money" -> {
                        addingBalance = "1100"
                        updateBalance(transactionDetails, "1100")
                    }
                    "2000_money" -> {
                        addingBalance = "2000"
                        updateBalance(transactionDetails, "2000")
                    }
                    "5000_money" -> {
                        addingBalance = "5000"
                        updateBalance(transactionDetails, "5000")
                    }
                }
            }
        }

    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        // Вызывается, когда появляется ошибка. См. константы класса
        // для получения более подробной информации
    }

    override fun onBillingInitialized() {
        val products = ArrayList<String>()
        products.add("100_money")
        products.add("200_money")
        products.add("560_money")
        products.add("1100_money")
        products.add("2000_money")
        products.add("5000_money")
        val shopList = findViewById<ListView>(R.id.lvShop)
        val shops = ArrayList<Shop>()


        val purchaseListingDetails = bp!!.getPurchaseListingDetails(products)
        for (i in purchaseListingDetails.indices) {
            shops.add(Shop(purchaseListingDetails[i].productId, purchaseListingDetails[i].title, purchaseListingDetails[i].priceValue))
        }
        Collections.sort(shops, Shop.ShopComparator)
        val adapter = ShopAdapter(applicationContext, shops)
        shopList.adapter = adapter

    }

    private fun updateBalance(details: TransactionDetails?, addingMoney: String?) {
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_update_balance"


        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        val accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_BALANCE, Response.Listener { response ->
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
                        updateDataTokens(tag_string_req)
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
                params["adding_money"] = addingMoney!!
                params["token"] = details!!.purchaseInfo.purchaseData.purchaseToken
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }


    private fun buy(productId: String) {
        if (bp!!.isInitialized) {
            bp!!.consumePurchase(productId)
            bp!!.purchase(this, productId)
        }

    }

    internal inner class ShopAdapter(context: Context, private val shopList: ArrayList<Shop>) : ArrayAdapter<Shop>(context, R.layout.list_item_shop, shopList) {
        private val inflater: LayoutInflater
        private val layout: Int = R.layout.list_item_shop


        init {
            this.inflater = LayoutInflater.from(context)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView

            val viewHolder: ViewHolder
            if (convertView == null) {
                convertView = inflater.inflate(this.layout, parent, false)
                viewHolder = ViewHolder(convertView!!)
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            val shop = shopList[position]

            viewHolder.titleView.text = shop.title
            viewHolder.priceView.text = "Цена: " + Math.round(shop.price) + " р."


            viewHolder.linkButton.setOnClickListener {
                //bp.consumePurchase(shop.getProductId());
                buy(shop.productId)
            }


            return convertView
        }

        private inner class ViewHolder internal constructor(view: View) {
            internal val linkButton: Button = view.findViewById(R.id.linkButton)
            internal val titleView: TextView
            internal val priceView: TextView

            init {
                titleView = view.findViewById(R.id.titleView)
                priceView = view.findViewById(R.id.priceView)

            }
        }
    }

    internal inner class ShopAppAdapter(context: Context, private val shopAppList: ArrayList<ShopApp>) : ArrayAdapter<ShopApp>(context, R.layout.list_item_shop, shopAppList) {
        private val inflater: LayoutInflater
        private val layout: Int = R.layout.list_item_shop


        init {
            this.inflater = LayoutInflater.from(context)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var convertView = convertView

            val viewHolder: ViewHolder
            if (convertView == null) {
                convertView = inflater.inflate(this.layout, parent, false)
                viewHolder = ViewHolder(convertView!!)
                convertView.tag = viewHolder
            } else {
                viewHolder = convertView.tag as ViewHolder
            }
            val shopApp = shopAppList[position]

            viewHolder.titleView.text = shopApp.title
            viewHolder.priceView.text = "Цена: " + Math.round(shopApp.price) + " монет"


            viewHolder.linkButton.setOnClickListener {
                when (shopApp.productId) {
                    "1" -> updateNumberView()
                    "2" -> updateNumberAdding()
                }
            }


            return convertView
        }


        private inner class ViewHolder internal constructor(view: View) {
            internal val linkButton: Button = view.findViewById(R.id.linkButton)
            internal val titleView: TextView
            internal val priceView: TextView

            init {
                titleView = view.findViewById(R.id.titleView)
                priceView = view.findViewById(R.id.priceView)

            }
        }


    }

    private fun updateNumberView() {
        pDialog!!.setMessage("Покупка контента ...")
        showDialog()
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_update_number_view"


        val token = db!!.dataTokens
        val accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_NUMBER_VIEW, Response.Listener { response ->
            Log.d(TAG, "Ответ сервара [Покупка просмотров событий]$response")

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")


                // Check for error node in json
                if (!error) {
                    val message = jObj.getString("message")
                    hideDialog()
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                } else {
                    hideDialog()
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
                // Posting parameters to login url
                val params = HashMap<String, String>()
                params["access_token"] = accessToken!!
                params["version"] = Integer.toString(BuildConfig.VERSION_CODE)

                return params
            }

        }

        // Adding request to request queue
        AppController.instance?.addToRequestQueue(strReq, tag_string_req)
    }

    private fun updateNumberAdding() {
        pDialog!!.setMessage("Покупка контента ...")
        showDialog()
        HttpsTrustManager.allowAllSSL()
        val tag_string_req = "req_update_number_adding"


        db = SQLiteHandler(applicationContext)

        val token = db!!.dataTokens
        val accessToken = token["access_token"]
        val strReq = object : StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_NUMBER_ADDING, Response.Listener { response ->
            Log.d(TAG, "Ответ сервара [Покупка добавления события]$response")

            try {
                val jObj = JSONObject(response)
                val error = jObj.getBoolean("error")
                val expAccessToken = jObj.getBoolean("exp_access_token")


                // Check for error node in json
                if (!error) {
                    hideDialog()
                    val message = jObj.getString("message")
                    Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()

                } else {
                    hideDialog()
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
                // Posting parameters to login url
                val params = HashMap<String, String>()
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
        Log.d(TAG, "Обновляем токен")
        val tag_string_req = "req_update_data_tokens"
        db = SQLiteHandler(applicationContext)

        val dataTokens = db!!.dataTokens

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
                    db!!.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken)

                    Log.d(TAG, "Получили новый токен:$accessToken")

                    when (tag) {
                        "req_update_balance" -> updateBalance(transactionDetails, addingBalance)
                        "req_update_number_view" -> updateNumberView()
                        "req_update_number_adding" -> updateNumberAdding()
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

        private val TAG = ShopActivity::class.java.simpleName
    }

}
