package ru.vpiska.app

import android.app.Application
import android.content.Context
import android.support.v7.app.AlertDialog
import android.text.Html
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.flurry.android.FlurryAgent
import ru.vpiska.R
import ru.vpiska.helper.CheckConnection
import ru.vpiska.helper.SQLiteHandler

class AppController : Application() {



    private val db: SQLiteHandler? = null

    private val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(applicationContext)
    }



    override fun onCreate() {
        super.onCreate()
        instance = this
        FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(this, "T9T92QFXRVY5JKQF2NDD")

    }

    fun <T> addToRequestQueue(req: Request<T>, tag: String) {

        if (CheckConnection.hasConnection(instance!!)) {
            req.tag = if (TextUtils.isEmpty(tag)) TAG else tag
            requestQueue.add(req)
        }

    }

    fun <T> addToRequestQueue(req: Request<T>) {
        req.tag = TAG
        requestQueue.add(req)
    }



    fun showDialogAboutUs(context: Context) {
        val aboutUsDialog = AlertDialog.Builder(context)
        aboutUsDialog.setIcon(R.mipmap.ic_launcher_new_2)
        aboutUsDialog.setTitle(R.string.action_about)
        aboutUsDialog.setCancelable(true)


        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val linearlayout = inflater.inflate(R.layout.dialog_about_us, null)
        aboutUsDialog.setView(linearlayout)

        val content = linearlayout.findViewById<TextView>(R.id.content)
        content.text = Html.fromHtml("<a href='https://vk.com/kvartirnikapp'>Мы вконтакте</a>")
        content.linksClickable = true
        content.movementMethod = LinkMovementMethod.getInstance()


        aboutUsDialog.create()
        aboutUsDialog.show()
    }

    companion object {

        private val TAG = AppController::class.java.simpleName


        @get:Synchronized
        var instance: AppController? = null
            private set
    }


}