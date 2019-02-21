package ru.vpiska.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Кирилл on 19.03.2018.
 */

public class PreferencesManager {

    private static SharedPreferences mSPref;

    private static final String APP_PREF    = "app_pref";      // имя файла настроек Вашего приложения

    private static final String APP_ADS_STATUS = "adsStatus";  // статус рекламы

    public PreferencesManager(Context context) {
        mSPref = context.getSharedPreferences(APP_PREF, Context.MODE_PRIVATE);
    }

    public void setAdsStatus(boolean adsStatus) {
        // true - enabled  | false - disabled
        SharedPreferences.Editor mSPEditor = mSPref.edit();
        mSPEditor.putBoolean(APP_ADS_STATUS, adsStatus);
        mSPEditor.apply();
    }

    public static boolean getAdsStatus() {
        try {
            return mSPref.getBoolean(APP_ADS_STATUS, true);
        }catch (NullPointerException e){
            return  false;
        }

    }

}