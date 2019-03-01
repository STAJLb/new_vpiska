package ru.vpiska.interfaces

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import ru.vpiska.app.ServerResponse

interface ApiService {
    @Multipart
    @POST("avatars/update/image")
    fun uploadFile(@Part file: MultipartBody.Part, @Part("file") name: RequestBody, @Part("time") time: String, @Part("ext") ext: String, @Part("access_token") token: String): Call<ServerResponse>
}