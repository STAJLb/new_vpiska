package ru.vpiska.interfaces;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import ru.vpiska.app.ServerResponse;

public interface ApiService {
    @Multipart
    @POST("avatars/update/image")
    Call<ServerResponse> uploadFile(@Part MultipartBody.Part file, @Part("file") RequestBody name,@Part("time") String time,@Part("ext") String ext,@Part("access_token") String token);
}