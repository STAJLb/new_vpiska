package ru.vpiska.app

import android.app.Activity
import android.view.View


import com.google.android.gms.ads.AdListener


import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

import ru.vpiska.R
import ru.vpiska.helper.PreferencesManager

/**
 * Created by Кирилл on 18.03.2018.
 */

object AdMobController {


    // создаем метод для создания баннера
    fun showBanner(activity: Activity) {


        val banner = activity.findViewById<AdView>(R.id.adView)

        if (PreferencesManager.adsStatus) {
            // создаем баннер, находим его по id

            // строит и загружает баннер
            // импорт android.gms.ads
            val adRequest = AdRequest.Builder().build()
            banner.loadAd(adRequest)

            // слушатель загрузки баннера
            banner.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                }
            }
        } else {
            banner.visibility = View.GONE
        }
    }


}