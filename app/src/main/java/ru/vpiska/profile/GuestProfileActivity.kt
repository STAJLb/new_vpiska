package ru.vpiska.profile

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.vpiska.MainScreenActivity
import ru.vpiska.R
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.map.MapsActivity
import ru.vpiska.rating.RatingActivity


/**
 * Created by Кирилл on 10.11.2017.
 */

class GuestProfileActivity : AppCompatActivity() {


    private val pDialog: ProgressDialog? = null


    private lateinit var session: SessionManager
    private lateinit var db: SQLiteHandler

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_guest)


        session = SessionManager(applicationContext)
        db = SQLiteHandler(applicationContext)


        val imgAvatar = findViewById<ImageView>(R.id.imgAvatar)
        val btnMap = findViewById<Button>(R.id.btnMap)
        val btnRating = findViewById<Button>(R.id.btnRating)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        Glide.with(applicationContext)
                .load("http://lumpics.ru/wp-content/uploads/2017/11/Programmyi-dlya-sozdaniya-avatarok.png")
                .placeholder(R.drawable.ic_profile)
                .fitCenter()
                .override(400, 400)
                .dontAnimate()
                .into(imgAvatar)

        btnRating.setOnClickListener {
            val intent = Intent(this@GuestProfileActivity, RatingActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnMap.setOnClickListener {
            val intent = Intent(this@GuestProfileActivity, MapsActivity::class.java)
            startActivity(intent)
            finish()
        }


//        AdMobController.showBanner(this)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_guest, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId
        if (id == R.id.action_exit) {
            logoutUser()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun logoutUser() {
        session.setKeyIsGuest(false)

        db.deleteDataTokensTable()

        // Launching the login activity
        val intent = Intent(this@GuestProfileActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val intent = Intent(this@GuestProfileActivity, MainScreenActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {


        private val TAG = GuestProfileActivity::class.java.simpleName
    }


}
