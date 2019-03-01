package ru.vpiska.profile

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.support.media.ExifInterface
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.vpiska.MainScreenActivity
import ru.vpiska.R
import ru.vpiska.app.AdMobController
import ru.vpiska.app.ApiClient
import ru.vpiska.app.AppController
import ru.vpiska.app.ServerResponse
import ru.vpiska.auth.LoginActivity
import ru.vpiska.helper.CheckConnection
import ru.vpiska.helper.SQLiteHandler
import ru.vpiska.helper.SessionManager
import ru.vpiska.interfaces.ApiService
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class UploadImageActivity : AppCompatActivity() {

    private var mediaPath: String? = null
    private var imgView: ImageView? = null
    private var progressDialog: ProgressDialog? = null


    private var db: SQLiteHandler? = null
    private var session: SessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_image)

        progressDialog = ProgressDialog(this, R.style.AppCompatAlertDialogStyle)
        progressDialog!!.setMessage("Идет загрузка изображения...")


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        db = SQLiteHandler(applicationContext)
        session = SessionManager(applicationContext)

        val btnUpload = findViewById<Button>(R.id.upload)
        val btnPickImage = findViewById<Button>(R.id.pick_img)
        imgView = findViewById(R.id.preview)

        btnUpload.setOnClickListener {
            if (CheckConnection.hasConnection(this@UploadImageActivity)) {
                uploadFile()
            }
        }

        btnPickImage.setOnClickListener { checkPermission() }

        AdMobController.showBanner(this)

    }

    private fun checkPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(response: PermissionGrantedResponse) {

                        startGal()
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        Toast.makeText(applicationContext, "Доступ ограничен.", Toast.LENGTH_LONG).show()
                    }

                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest, token: PermissionToken) {
                        token.continuePermissionRequest()
                    }
                }).check()

    }

    private fun startGal() {
        val galleryIntent = Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, 0)
    }

    override fun onBackPressed() {
        val intent = Intent(this@UploadImageActivity, MainScreenActivity::class.java)
        startActivity(intent)
        finish()
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
        val intent = Intent(this@UploadImageActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            // When an Image is picked
            if (requestCode == 0 && resultCode == Activity.RESULT_OK && null != data) {

                // Get the Image from data
                val selectedImage = data.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

                assert(selectedImage != null)
                val cursor = contentResolver.query(selectedImage!!, filePathColumn, null, null, null)!!
                cursor.moveToFirst()

                val columnIndex = cursor.getColumnIndex(filePathColumn[0])
                mediaPath = cursor.getString(columnIndex)
                // Set the Image in ImageView for Previewing the Media
                imgView!!.setImageBitmap(BitmapFactory.decodeFile(mediaPath))
                var img = BitmapFactory.decodeFile(mediaPath)
                img = rotateBitmap(img, mediaPath)
                imgView!!.setImageBitmap(img)

                cursor.close()

            } else {
                Toast.makeText(this, "Изображение не было выбрано.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка.", Toast.LENGTH_LONG).show()
        }

    }

    private fun rotateBitmap(srcBitmap: Bitmap, path: String?): Bitmap {
        var exif: ExifInterface? = null
        try {
            exif = ExifInterface(path!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        assert(exif != null)
        val orientation = exif!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        exif.setAttribute(ExifInterface.TAG_ORIENTATION, 0.toString())
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width,
                srcBitmap.height, matrix, true)
    }

    // Uploading Image/Video
    private fun uploadFile() {
        if (mediaPath != null) {
            progressDialog!!.show()


            val file = File(mediaPath)

            @SuppressLint("SimpleDateFormat") val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss")
            val format = simpleDateFormat.format(Date())

            // Parsing any Media type file
            val requestBody = RequestBody.create(MediaType.parse("*/*"), file)
            val fileToUpload = MultipartBody.Part.createFormData("file", file.name, requestBody)
            val filename = RequestBody.create(MediaType.parse("text/plain"), file.name)


            db = SQLiteHandler(applicationContext)

            val token = db!!.dataTokens
            val accessToken = token["access_token"]

            val getResponse = ApiClient.client.create(ApiService::class.java)
            val call = getResponse.uploadFile(fileToUpload, filename, format, getFileExtension(file.name)!!, accessToken!!)
            call.enqueue(object : Callback<ServerResponse> {
                override fun onResponse(call: Call<ServerResponse>, response: Response<ServerResponse>) {
                    val serverResponse = response.body()
                    if (serverResponse != null) {
                        Toast.makeText(applicationContext, serverResponse.message, Toast.LENGTH_SHORT).show()
                    } else {
                        assert(false)
                        //                        Log.v("Response", serverResponse.toString());
                    }
                    progressDialog!!.dismiss()
                }

                override fun onFailure(call: Call<ServerResponse>, t: Throwable) {

                }
            })
        } else {
            Toast.makeText(applicationContext, "Изображение не выбрано.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileExtension(mystr: String): String? {
        val index = mystr.indexOf('.')
        return if (index == -1) null else mystr.substring(index)
    }

}