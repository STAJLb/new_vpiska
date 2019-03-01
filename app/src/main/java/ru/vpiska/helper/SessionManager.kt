package ru.vpiska.helper

/**
 * Created by Кирилл on 09.10.2017.
 */

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.util.Log

class SessionManager @SuppressLint("CommitPrefEdits")
constructor(_context: Context) {

    // Shared Preferences
    private var pref: SharedPreferences

    private var editor: Editor

    // Shared pref mode
    private var PRIVATE_MODE = 0

    val isLoggedIn: Boolean
        get() = pref.getBoolean(KEY_IS_LOGGEDIN, false)
    val isGuest: Boolean
        get() = pref.getBoolean(KEY_IS_GUEST, false)

    init {
        pref = _context.getSharedPreferences(PREF_NAME, PRIVATE_MODE)
        editor = pref.edit()
    }

    fun setLogin(isLoggedIn: Boolean) {

        editor.putBoolean(KEY_IS_LOGGEDIN, isLoggedIn)

        // commit changes
        editor.commit()

        Log.d(TAG, "User login session modified!")
    }

    fun setKeyIsGuest(isGuest: Boolean) {

        editor.putBoolean(KEY_IS_GUEST, isGuest)

        // commit changes
        editor.commit()

        Log.d(TAG, "User login session modified!")
    }

    companion object {
        // LogCat tag
        private val TAG = SessionManager::class.java.simpleName

        // Shared preferences file name
        private const val PREF_NAME = "AndroidHiveLogin"

        private const val KEY_IS_LOGGEDIN = "isLoggedIn"

        private const val KEY_IS_GUEST = "isGuest"
    }
}
