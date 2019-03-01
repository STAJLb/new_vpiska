package ru.vpiska.app

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val URL_SERVER = "https://clickcoffee.ru/api/v2/"

    val client: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl(URL_SERVER)
                .client(UnsafeOkHttpClient.unsafeOkHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

    }
}