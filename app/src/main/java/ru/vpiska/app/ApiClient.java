package ru.vpiska.app;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit = null;
    public static int unique_id;
    private final static String URL_SERVER = "https://clickcoffee.ru/api/v2/";

    public static Retrofit getClient() {

        OkHttpClient okHttpClient = UnsafeOkHttpClient.getUnsafeOkHttpClient();
        if (retrofit==null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(URL_SERVER)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}