package ru.vpiska.auth;

import android.Manifest;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Bundle;


import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ru.vpiska.MainScreenActivity;
import ru.vpiska.R;
import ru.vpiska.app.AppConfig;
import ru.vpiska.app.AppController;
import ru.vpiska.app.HttpsTrustManager;

import ru.vpiska.helper.CheckConnection;
import ru.vpiska.helper.SessionManager;


public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = RegisterActivity.class.getSimpleName();


    private EditText inputFirstName;
    private EditText inputNikName;
    private EditText inputPassword;


    private TextView txtAge;

    private RadioGroup radioGroup;

    private ProgressDialog pDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        TextView txtOkContract = findViewById(R.id.txtOkContract);
        txtAge = findViewById(R.id.txtAge);

        inputFirstName = findViewById(R.id.first_name);
        inputNikName = findViewById(R.id.nik_name);
        inputPassword = findViewById(R.id.password);

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLinkToLogin = findViewById(R.id.btnLinkToLoginScreen);
        Button btnInputAge = findViewById(R.id.inputAge);


        CheckBox checkContrakt = findViewById(R.id.checkContrakt);
        radioGroup = findViewById(R.id.radioGroup);

        // Progress dialog
        pDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        pDialog.setCancelable(false);

        // Session manager
        SessionManager session = new SessionManager(getApplicationContext());

        // Check if user is already logged in or not
        if (session.isLoggedIn()) {
            // User is already logged in. Take him to main activity
            Intent intent = new Intent(RegisterActivity.this,
                    MainScreenActivity.class);
            startActivity(intent);
            finish();
        }

        txtOkContract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RegisterActivity.this, AgreementActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Register Button Click event
        btnRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (CheckConnection.hasConnection(RegisterActivity.this)) {
                    sendRegisterRequset();
                }

            }
        });

        // Link to Login Screen
        btnLinkToLogin.setOnClickListener(new View.OnClickListener() {

            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),
                        LoginActivity.class);
                startActivity(i);
                finish();

            }
        });

        btnInputAge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                DialogFragment dateDialog = new ru.vpiska.helper.DatePicker();
                dateDialog.show(getSupportFragmentManager(), "datePicker");


            }
        });

    }

    private void openSiteDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_perm_for_reg)
                .setTitle(R.string.title_perf_for_reg)
                .setPositiveButton(R.string.good, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        checkPermission();
                    }
                });

        // Create the AlertDialog object and return it
        builder.create();
        builder.show();
    }

    private void checkPermission() {
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_PHONE_STATE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        sendRegisterRequset();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(getApplicationContext(), "Доступ ограничен.", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();

    }

    private void hasPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE);
        if (result != PackageManager.PERMISSION_GRANTED) {
            openSiteDialog();
        } else {
            sendRegisterRequset();
        }
    }
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    private synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }


    private void sendRegisterRequset() {
        String deviceUniqueIdentifier = id(this);
//        String deviceUniqueIdentifier = null;
//        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//        if (null != tm) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            deviceUniqueIdentifier = tm.getDeviceId();
//        }
//        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
//            deviceUniqueIdentifier = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
//        }

        String firstName = inputFirstName.getText().toString().trim();
        String nikName = inputNikName.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String age = txtAge.getText().toString().trim();
        String sex = null;

        int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
        switch (checkedRadioButtonId){
            case R.id.rbMan:
                sex = "m";
                break;
            case R.id.rbWoman:
                sex = "w";
                break;
        }


        if (!firstName.isEmpty() && !nikName.isEmpty() && !password.isEmpty()) {
            registerUser(firstName, nikName, password, sex, deviceUniqueIdentifier,age);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Пожалуйста, заполните все поля!", Toast.LENGTH_LONG)
                    .show();
        }
    }




    /**
     * Function to store user in MySQL database will post params(tag, name,
     * email, password) to register url
     * */
    private void registerUser(final String firstName, final String nikName,
                              final String password, final String sex,final String imei,final  String age) {
        // Tag used to cancel the request
        HttpsTrustManager.allowAllSSL();
        String tag_string_req = "req_register";

        pDialog.setMessage("Идет регистрация ...");
        showDialog();

        StringRequest strReq = new StringRequest(Method.POST,
                AppConfig.URL_REGISTER, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.d(TAG, "Register Response: " + response);
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    boolean error = jObj.getBoolean("error");
                    if (!error) {

                        Toast.makeText(getApplicationContext(), "Регистрация прошла успешно. Пожалуйста, войдите.", Toast.LENGTH_LONG).show();

                        // Launch login activity
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    } else {

                        // Error occurred in registration. Get the error
                        // message
                        String errorMsg = jObj.getString("error_msg");
                        Toast.makeText(getApplicationContext(),
                                errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Registration Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting params to register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("first_name", firstName);
                params.put("nik_name", nikName);
                params.put("password", password);
                params.put("sex", sex);
                params.put("age", age);
                params.put("imei", imei);


                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_string_req);

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(intent);
        finish();
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