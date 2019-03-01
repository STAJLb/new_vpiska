package ru.vpiska.helper

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.util.*

class SQLiteHandler(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {


    /**
     * Getting user data from database
     */
    // Move to first row
    // return user
    val dataTokens: HashMap<String, String>
        get() {
            val token = HashMap<String, String>()
            val selectQuery = "SELECT  * FROM $TABLE_DATA_TOKENS"

            val db = this.readableDatabase
            val cursor = db.rawQuery(selectQuery, null)
            cursor.moveToFirst()
            if (cursor.count > 0) {
                token["access_token"] = cursor.getString(1)
                token["exp_access_token"] = cursor.getString(2)
                token["refresh_token"] = cursor.getString(3)
                token["exp_refresh_token"] = cursor.getString(4)

            }
            cursor.close()
            db.close()
            Log.d(TAG, "Fetching user from Sqlite: $token")

            return token
        }

    // Creating Tables
    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_DATA_TOKENS_TABLE = ("CREATE TABLE " + TABLE_DATA_TOKENS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_ACCESS_TOKEN + " TEXT ," + KEY_EXP_ACCESS_TOKEN + " TEXT ," + KEY_REFRESH_TOKEN + " TEXT," + KEY_EXP_REFRESH_TOKEN + " TEXT" + ")")
        db.execSQL(CREATE_DATA_TOKENS_TABLE)

        Log.d(TAG, "Database tables created")
    }

    // Upgrading database
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DATA_TOKENS")

        // Create tables again
        onCreate(db)
    }

    /**
     * Storing user details in database
     */
    fun addDataTokens(accessToken: String, expAccessToken: String, refreshToken: String, expRefreshToken: String) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_ACCESS_TOKEN, accessToken)
        values.put(KEY_EXP_ACCESS_TOKEN, expAccessToken)
        values.put(KEY_REFRESH_TOKEN, refreshToken)
        values.put(KEY_EXP_REFRESH_TOKEN, expRefreshToken)

        // Inserting Row
        val id = db.insert(TABLE_DATA_TOKENS, null, values)
        db.close() // Closing database connection

        Log.d(TAG, "New token inserted into sqlite: $id")
    }

    fun updateDataTokens(accessToken: String, expAccessToken: String, refreshToken: String, expRefreshToken: String) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(KEY_ACCESS_TOKEN, accessToken)
        values.put(KEY_EXP_ACCESS_TOKEN, expAccessToken)
        values.put(KEY_REFRESH_TOKEN, refreshToken)
        values.put(KEY_EXP_REFRESH_TOKEN, expRefreshToken)





        db.update(TABLE_DATA_TOKENS, values, "id = ?", arrayOf("1"))
        db.close()

    }


    /**
     * Re crate database Delete all tables and create them again
     */
    fun deleteDataTokensTable() {
        val db = this.writableDatabase

        // Delete All Rows
        db.delete(TABLE_DATA_TOKENS, null, null)
        db.close()

        Log.d(TAG, "Deleted all user info from sqlite")
    }

    companion object {

        private val TAG = SQLiteHandler::class.java.simpleName

        // All Static variables
        // Database Version
        private const val DATABASE_VERSION = 10

        // Database Name
        private const val DATABASE_NAME = "kvartirnik"

        // Login table name
        private const val TABLE_DATA_TOKENS = "data_tokens"

        // Login Table Columns names
        private const val KEY_ID = "id"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_EXP_ACCESS_TOKEN = "exp_access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_EXP_REFRESH_TOKEN = "exp_refresh_token"
    }

}