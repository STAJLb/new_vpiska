package ru.vpiska.helper

import android.content.Context
import android.content.SharedPreferences

/**
 * Created by Кирилл on 19.03.2018.
 */

class PreferencesManager(context: Context) {

    init {
        mSPref = context.getSharedPreferences(APP_PREF, Context.MODE_PRIVATE)
    }

    companion object {

        private lateinit var mSPref: SharedPreferences

        private val APP_PREF = "app_pref"      // имя файла настроек Вашего приложения

        private val APP_ADS_STATUS = "adsStatus"  // статус рекламы

        // true - enabled  | false - disabled
        var adsStatus: Boolean
            get() {
                try {
                    return mSPref.getBoolean(APP_ADS_STATUS, true)
                } catch (e: NullPointerException) {
                    return false
                }

            }
            set(adsStatus) {
                val mSPEditor = mSPref.edit()
                mSPEditor.putBoolean(APP_ADS_STATUS, adsStatus)
                mSPEditor.apply()
            }
    }

}