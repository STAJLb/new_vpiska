package ru.vpiska.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.PurchaseState;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.flurry.android.FlurryAgent;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import ru.vpiska.BuildConfig;
import ru.vpiska.R;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.InAppBillingResources;
import ru.vpiska.helper.PreferencesManager;
import ru.vpiska.helper.SQLiteHandler;

import static ru.vpiska.helper.InAppBillingResources.getSKU_Disable_Ads;

public class AppController extends Application  {

    private static final String TAG = AppController.class.getSimpleName();

    private RequestQueue mRequestQueue;

    @SuppressLint("StaticFieldLeak")
    private static AppController mInstance;

    private  SQLiteHandler db;


    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        new FlurryAgent.Builder()
                .withLogEnabled(true)
                .build(this, "T9T92QFXRVY5JKQF2NDD");

    }




    public static synchronized AppController getInstance() {
        return mInstance;
    }


    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {

       if(CheckConnection.hasConnection(mInstance)){
           req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
           getRequestQueue().add(req);
       }

    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }


    public void showDialogAboutUs(Context context){
        final AlertDialog.Builder aboutUsDialog = new AlertDialog.Builder(context);
        aboutUsDialog.setIcon(R.mipmap.ic_launcher_new_2);
        aboutUsDialog.setTitle(R.string.action_about);
        aboutUsDialog.setCancelable(true);


        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE );
        @SuppressLint("InflateParams") View linearlayout = inflater.inflate(R.layout.dialog_about_us, null);
        aboutUsDialog.setView(linearlayout);

        TextView content = (TextView) linearlayout.findViewById(R.id.content);
        content.setText(Html.fromHtml("<a href='https://vk.com/kvartirnikapp'>Мы вконтакте</a>"));
        content.setLinksClickable(true);
        content.setMovementMethod(LinkMovementMethod.getInstance());


        aboutUsDialog.create();
        aboutUsDialog.show();
    }







}