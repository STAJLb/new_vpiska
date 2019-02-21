package ru.vpiska.profile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AdMobController;
import ru.vpiska.app.ApiClient;
import ru.vpiska.app.AppController;
import ru.vpiska.app.ServerResponse;
import ru.vpiska.auth.LoginActivity;
import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SQLiteHandler;
import ru.vpiska.helper.SessionManager;
import ru.vpiska.interfaces.ApiService;


public class UploadImageActivity extends AppCompatActivity {

    private String mediaPath;
    private ImageView imgView;
    private ProgressDialog progressDialog;


    private SQLiteHandler db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        progressDialog = new ProgressDialog(this,R.style.AppCompatAlertDialogStyle);
        progressDialog.setMessage("Идет загрузка изображения...");


        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);

        db = new SQLiteHandler(getApplicationContext());
        session = new SessionManager(getApplicationContext());

        Button btnUpload = findViewById(R.id.upload);
        Button btnPickImage = findViewById(R.id.pick_img);
        imgView = findViewById(R.id.preview);

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(CheckConnection.hasConnection(UploadImageActivity.this)) {
                    uploadFile();
                }


            }
        });

        btnPickImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });

        AdMobController.showBanner(this);

    }

    private void checkPermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override public void onPermissionGranted(PermissionGrantedResponse response) {

                        startGal();
                    }
                    @Override public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(), "Доступ ограничен.", Toast.LENGTH_LONG).show();
                    }
                    @Override public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    private void startGal(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, 0);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(UploadImageActivity.this,MainScreenActivity.class);
        startActivity(intent);
        finish();
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

    private void logoutUser() {
        session.setLogin(false);

        db.deleteDataTokensTable();

        // Launching the login activity
        Intent intent = new Intent(UploadImageActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            // When an Image is picked
            if (requestCode == 0 && resultCode == RESULT_OK && null != data) {

                // Get the Image from data
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                assert selectedImage != null;
                Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                assert cursor != null;
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                mediaPath = cursor.getString(columnIndex);
                // Set the Image in ImageView for Previewing the Media
                imgView.setImageBitmap(BitmapFactory.decodeFile(mediaPath));
                Bitmap img = BitmapFactory.decodeFile(mediaPath);
                img = rotateBitmap(img,mediaPath);
                imgView.setImageBitmap(img);

                cursor.close();

            } else {
                Toast.makeText(this, "Изображение не было выбрано.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка.", Toast.LENGTH_LONG).show();
        }

    }
    private static Bitmap rotateBitmap(Bitmap srcBitmap, String path) {
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert exif != null;
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(0));
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                break;
        }
        return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                srcBitmap.getHeight(), matrix, true);
    }

    // Uploading Image/Video
    private void uploadFile() {
        if(mediaPath != null) {
            progressDialog.show();


            File file = new File(mediaPath);

            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy-hh-mm-ss");
            String format = simpleDateFormat.format(new Date());

            // Parsing any Media type file
            RequestBody requestBody = RequestBody.create(MediaType.parse("*/*"), file);
            MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), requestBody);
            RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());


            db = new SQLiteHandler(getApplicationContext());

            HashMap<String, String> token = db.getDataTokens();
            String accessToken = token.get("access_token");

            ApiService getResponse = ApiClient.getClient().create(ApiService.class);
            Call<ServerResponse> call = getResponse.uploadFile(fileToUpload, filename, format, getFileExtension(file.getName()), accessToken);
            call.enqueue(new Callback<ServerResponse>() {
                @SuppressLint("Assert")
                @Override
                public void onResponse(Call<ServerResponse> call, Response<ServerResponse> response) {
                    ServerResponse serverResponse = response.body();
                    if (serverResponse != null) {
                        if (serverResponse.getSuccess()) {
                            Toast.makeText(getApplicationContext(), serverResponse.getMessage(), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(UploadImageActivity.this, MainScreenActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(getApplicationContext(), serverResponse.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        assert false;
//                        Log.v("Response", serverResponse.toString());
                    }
                    progressDialog.dismiss();
                }

                @Override
                public void onFailure(Call<ServerResponse> call, Throwable t) {

                }
            });
        }else {
            Toast.makeText(getApplicationContext(), "Изображение не выбрано.", Toast.LENGTH_SHORT).show();
        }
    }
    private static String getFileExtension(String mystr) {
        int index = mystr.indexOf('.');
        return index == -1? null : mystr.substring(index);
    }

}