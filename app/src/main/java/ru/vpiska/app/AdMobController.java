package ru.vpiska.app;

import android.app.Activity;
import android.view.View;


import com.google.android.gms.ads.AdListener;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import ru.vpiska.R;
import ru.vpiska.helper.PreferencesManager;

/**
 * Created by Кирилл on 18.03.2018.
 */

public class AdMobController {


    // создаем метод для создания баннера
    public static void showBanner(final Activity activity) {


        final AdView banner = (AdView) activity.findViewById(R.id.adView);

        if(PreferencesManager.getAdsStatus()) {
            // создаем баннер, находим его по id

            // строит и загружает баннер
            // импорт android.gms.ads
            AdRequest adRequest = new AdRequest.Builder().build();
            banner.loadAd(adRequest);

            // слушатель загрузки баннера
            banner.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                }
            });
        }else{
            banner.setVisibility(View.GONE);
        }
    }





}