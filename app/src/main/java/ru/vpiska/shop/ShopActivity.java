package ru.vpiska.shop;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.vpiska.BuildConfig;
import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.InAppBillingResources;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;



public class ShopActivity extends AppCompatActivity  implements BillingProcessor.IBillingHandler {

    private static final String TAG = ShopActivity.class.getSimpleName();

    private SessionManager session;
    private SQLiteHandler db;

    private String accessToken,addingBalance;
    private BillingProcessor bp;
    private ProgressDialog pDialog;

    private TransactionDetails transactionDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shops);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        db = new SQLiteHandler(getApplicationContext());

        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);
        TabHost tabHost = findViewById(android.R.id.tabhost);
        // инициализация
        tabHost.setup();

        TabHost.TabSpec tabSpec = tabHost.newTabSpec("tag1");
        tabSpec.setIndicator("Монеты");
        tabSpec.setContent(R.id.tab1);
        tabHost.addTab(tabSpec);

        tabSpec = tabHost.newTabSpec("tag2");
        tabSpec.setIndicator("Контент");
        tabSpec.setContent(R.id.tab2);
        tabHost.addTab(tabSpec);

        final TabWidget tw = tabHost.findViewById(android.R.id.tabs);
        for (int i = 0; i < tw.getChildCount(); ++i)
        {
            final View tabView = tw.getChildTabViewAt(i);
            final TextView tv = tabView.findViewById(android.R.id.title);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(12);
        }

        tabHost.setCurrentTabByTag("tag1");


        ListView shopList = findViewById(R.id.lvShopApp);
        ArrayList<ShopApp> shops = new ArrayList<ShopApp>();
        shops.add(new ShopApp("1","Купить 10 просмотров событий",100));
        shops.add(new ShopApp("2","Купить 1 добавление события",100));

        Collections.sort(shops,ShopApp.ShopComparator);
        ShopAppAdapter adapter = new ShopAppAdapter(getApplicationContext(), R.layout.list_item_shop, shops);
        shopList.setAdapter(adapter);


        bp = new BillingProcessor(getApplicationContext(),
                InAppBillingResources.getRsaKey(), InAppBillingResources.getMerchantId(), this); // инициализируем `BillingProcessor`. В документации на `GitHub` сказано, что для защиты от липовых покупок через приложения типа `freedom` необходимо в конструктор `BillingProcessor`'а передать еще и свой `MERCHANT_ID`. Где его взять - внизу текущего ответа опишу шаги
        bp.initialize();
        session = new SessionManager(getApplicationContext());

        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);





        AdMobController.showBanner(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (!bp.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_about) {
            AppController.getInstance().showDialogAboutUs(this);
        }
        if (id == R.id.action_exit) {
            logoutUser();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ShopActivity.this, MainScreenActivity.class);
        startActivity(intent);
        finish();
    }

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(ShopActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        //Вызывается, когда история покупки была восстановлена,
        // и список всех принадлежащих идентификаторы продуктов был загружен из Google Play
        // так Вы сможете НУЖНУЮ покупку проверить


    }

    @Override
    public void onProductPurchased(@NonNull String productId, @Nullable TransactionDetails details) {
        transactionDetails = details;
        if(bp.isInitialized()){
        if (bp.isPurchased(productId)) {
            switch (productId){
                case "100_money":
                    addingBalance = "100";
                    updateBalance(transactionDetails,"100");
                    break;
                case "200_money":
                    addingBalance = "200";
                    updateBalance(transactionDetails,"200");
                    break;
                case "560_money":
                    addingBalance = "560";
                    updateBalance(transactionDetails,"560");
                    break;
                case "1100_money":
                    addingBalance = "1100";
                    updateBalance(transactionDetails,"1100");
                    break;
                case "2000_money":
                    addingBalance = "2000";
                    updateBalance(transactionDetails,"2000");
                    break;
                case "5000_money":
                    addingBalance = "5000";
                    updateBalance(transactionDetails,"5000");
                    break;
            }
        }
        }

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        // Вызывается, когда появляется ошибка. См. константы класса
        // для получения более подробной информации
    }

    @Override
    public void onBillingInitialized() {
        ArrayList<String> products = new ArrayList<>();
        products.add("100_money");
        products.add("200_money");
        products.add("560_money");
        products.add("1100_money");
        products.add("2000_money");
        products.add("5000_money");
        ListView shopList = findViewById(R.id.lvShop);
        ArrayList<Shop> shops = new ArrayList<Shop>();


        List<SkuDetails> purchaseListingDetails = bp.getPurchaseListingDetails(products);
        for(int i = 0;i<purchaseListingDetails.size();i++){
            shops.add(new Shop(purchaseListingDetails.get(i).productId,purchaseListingDetails.get(i).title,purchaseListingDetails.get(i).priceValue));
        }
        Collections.sort(shops,Shop.ShopComparator);
        ShopAdapter adapter = new ShopAdapter(getApplicationContext(), R.layout.list_item_shop, shops);
        shopList.setAdapter(adapter);

    }

    private void updateBalance(final TransactionDetails details,final String addingMoney) {
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update_balance";


        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        final String accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_BALANCE, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.d(TAG, "Ответ сервара [Покупка товара]" + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");
                    String message = jObj.getString("message");


                    // Check for error node in json
                    if (!error) {
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    } else {
                        if (expAccessToken) {
                            updateDataTokens(tag_string_req);
                        } else {
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                params.put("access_token", accessToken);
                params.put("adding_money", addingMoney);
                params.put("token", details.purchaseInfo.purchaseData.purchaseToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }




    private void buy(String productId){
        if(bp.isInitialized()){
            bp.consumePurchase(productId);
            bp.purchase(this,productId);
        }

    }

    public class ShopAdapter extends ArrayAdapter<Shop> {
        private LayoutInflater inflater;
        private int layout;
        private ArrayList<Shop> shopList;
        private Context mContext = getContext();


        ShopAdapter(Context context, int resource, ArrayList<Shop> shop) {
            super(context, resource, shop);
            this.shopList = shop;
            this.layout = resource;
            this.inflater = LayoutInflater.from(context);
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {

            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(this.layout, parent, false);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            final Shop shop = shopList.get(position);

            viewHolder.titleView.setText(shop.getTitle());
            viewHolder.priceView.setText("Цена: " + Math.round(shop.getPrice()) + " р.");


            viewHolder.linkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //bp.consumePurchase(shop.getProductId());
                    buy(shop.getProductId());
                }
            });


            return convertView;
        }
        private class ViewHolder {
            final Button linkButton;
            final TextView titleView,priceView;

            ViewHolder(View view) {
                linkButton = (Button) view.findViewById(R.id.linkButton);
                titleView = (TextView) view.findViewById(R.id.titleView);
                priceView = (TextView) view.findViewById(R.id.priceView);

            }
        }
    }
        public class ShopAppAdapter extends ArrayAdapter<ShopApp> {
            private LayoutInflater inflater;
            private int layout;
            private ArrayList<ShopApp> shopAppList;
            private Context mContext = getContext();


            ShopAppAdapter(Context context, int resource, ArrayList<ShopApp> shopApp) {
                super(context, resource, shopApp);
                this.shopAppList = shopApp;
                this.layout = resource;
                this.inflater = LayoutInflater.from(context);
            }

            @NonNull
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                final ViewHolder viewHolder;
                if (convertView == null) {
                    convertView = inflater.inflate(this.layout, parent, false);
                    viewHolder = new ViewHolder(convertView);
                    convertView.setTag(viewHolder);
                } else {
                    viewHolder = (ViewHolder) convertView.getTag();
                }
                final ShopApp shopApp = shopAppList.get(position);

                viewHolder.titleView.setText(shopApp.getTitle());
                viewHolder.priceView.setText("Цена: " + Math.round(shopApp.getPrice()) + " монет");


                viewHolder.linkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    switch (shopApp.getProductId()){
                        case "1":
                            updateNumberView();
                            break;
                        case "2":
                            updateNumberAdding();
                            break;

                    }
                    }
                });


                return convertView;
            }






        private class ViewHolder {
            final Button linkButton;
            final TextView titleView,priceView;

            ViewHolder(View view) {
                linkButton = (Button) view.findViewById(R.id.linkButton);
                titleView = (TextView) view.findViewById(R.id.titleView);
                priceView = (TextView) view.findViewById(R.id.priceView);

            }
        }


    }

    private void updateNumberView() {
        pDialog.setMessage("Покупка контента ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update_number_view";




        HashMap<String, String> token = db.getDataTokens();
        final String accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_NUMBER_VIEW, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.d(TAG, "Ответ сервара [Покупка просмотров событий]" + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");



                    // Check for error node in json
                    if (!error) {
                        String message = jObj.getString("message");
                        hideDialog();
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    } else {
                        hideDialog();
                        if (expAccessToken) {
                            updateDataTokens(tag_string_req);
                        } else {
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                params.put("access_token", accessToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void updateNumberAdding() {
        pDialog.setMessage("Покупка контента ...");
        showDialog();
        HttpsTrustManager.allowAllSSL();
        final String tag_string_req = "req_update_number_adding";


        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> token = db.getDataTokens();
        final String accessToken = token.get("access_token");
        StringRequest strReq = new StringRequest(Request.Method.POST, AppConfig.URL_UPDATE_NUMBER_ADDING, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                Log.d(TAG, "Ответ сервара [Покупка добавления события]" + response);

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    boolean expAccessToken = jObj.getBoolean("exp_access_token");


                    // Check for error node in json
                    if (!error) {
                        hideDialog();
                        String message = jObj.getString("message");
                        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

                    } else {
                        hideDialog();
                        if (expAccessToken) {
                            updateDataTokens(tag_string_req);
                        } else {
                            String errorMsg = jObj.getString("error_msg");
                            Toast.makeText(getApplicationContext(), "Ошибка: " +
                                    errorMsg, Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (JSONException e) {

                    // JSON error
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                params.put("access_token", accessToken);
                params.put("version", Integer.toString(BuildConfig.VERSION_CODE));

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void updateDataTokens(final String tag) {
        HttpsTrustManager.allowAllSSL();
        Log.d(TAG, "Обновляем токен");
        String tag_string_req = "req_update_data_tokens";
        db = new SQLiteHandler(getApplicationContext());

        HashMap<String, String> dataTokens = db.getDataTokens();

        final String refreshToken = dataTokens.get("refresh_token");

        StringRequest strReq = new StringRequest(Request.Method.PUT, AppConfig.URL_UPDATE_DATA_TOKENS, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");

                    // Check for error node in json
                    if (!error) {

                        JSONObject dataTokens = jObj.getJSONObject("data_tokens");
                        String accessToken = dataTokens.getString("access_token");
                        String refreshToken = dataTokens.getString("refresh_token");
                        String expAccessToken = dataTokens.getString("exp_access_token");
                        String expRefreshToken = dataTokens.getString("exp_refresh_token");
                        Log.d(TAG, "Обновили таблицу");
                        db.updateDataTokens(accessToken, expAccessToken, refreshToken, expRefreshToken);

                        Log.d(TAG, "Получили новый токен:" + accessToken);

                        switch (tag) {
                            case "req_update_balance":
                                updateBalance(transactionDetails,addingBalance);
                                break;
                            case "req_update_number_view":
                                updateNumberView();
                                break;
                            case "req_update_number_adding":
                                 updateNumberAdding();
                                break;
                        }

                    } else {
                        // Error in login. Get the error message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(), "Ошибка: " +
                                errorMsg, Toast.LENGTH_LONG).show();
                    }

                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof TimeoutError) {
                    Toast.makeText(getApplicationContext(),
                            "Серверная ошибка.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<>();
                params.put("refresh_token", refreshToken);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);
    }

    private void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

}
