package ru.vpiska.interfaces;

import com.google.android.gms.tasks.Task;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Created by Кирилл on 06.03.2018.
 */

public interface UpdateToken {
    @GET("/tasks")
    Call<List<Task>> updateToken();
}
