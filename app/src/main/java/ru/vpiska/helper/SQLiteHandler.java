package ru.vpiska.helper;

/**
 * Created by Кирилл on 09.10.2017.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.HashMap;

public class SQLiteHandler extends SQLiteOpenHelper {

    private static final String TAG = SQLiteHandler.class.getSimpleName();

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 10;

    // Database Name
    private static final String DATABASE_NAME = "kvartirnik";

    // Login table name
    private static final String TABLE_DATA_TOKENS = "data_tokens";

    // Login Table Columns names
    private static final String KEY_ID = "id";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_EXP_ACCESS_TOKEN = "exp_access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXP_REFRESH_TOKEN = "exp_refresh_token";
//    private static final String KEY_UID = "uid";
//    private static final String IMAGE = "image";
//    private static final String SEX = "sex";
//    private static final String AGE = "age";
//    private static final String KEY_CREATED_AT = "created_at";

    public SQLiteHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_DATA_TOKENS_TABLE = "CREATE TABLE " + TABLE_DATA_TOKENS + "("
                + KEY_ID + " INTEGER PRIMARY KEY," + KEY_ACCESS_TOKEN + " TEXT ," + KEY_EXP_ACCESS_TOKEN + " TEXT ," +  KEY_REFRESH_TOKEN + " TEXT," +  KEY_EXP_REFRESH_TOKEN + " TEXT" + ")";
        db.execSQL(CREATE_DATA_TOKENS_TABLE);

        Log.d(TAG, "Database tables created");
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA_TOKENS);

        // Create tables again
        onCreate(db);
    }

    /**
     * Storing user details in database
     * */
    public void addDataTokens(String accessToken,String expAccessToken,String refreshToken,String expRefreshToken) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ACCESS_TOKEN, accessToken);
        values.put(KEY_EXP_ACCESS_TOKEN, expAccessToken);
        values.put(KEY_REFRESH_TOKEN, refreshToken);
        values.put(KEY_EXP_REFRESH_TOKEN, expRefreshToken);

        // Inserting Row
        long id = db.insert(TABLE_DATA_TOKENS, null, values);
        db.close(); // Closing database connection

        Log.d(TAG, "New token inserted into sqlite: " + id);
    }

    public void updateDataTokens(String accessToken,String expAccessToken,String refreshToken,String expRefreshToken){
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_ACCESS_TOKEN, accessToken);
        values.put(KEY_EXP_ACCESS_TOKEN, expAccessToken);
        values.put(KEY_REFRESH_TOKEN, refreshToken);
        values.put(KEY_EXP_REFRESH_TOKEN, expRefreshToken);





        db.update(TABLE_DATA_TOKENS,values,"id = ?",new String[] { "1" });
        db.close();

    }

//    public void updateUser(String uid,String firstName, String nikName,String age,String sex, String image){
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(KEY_FIRST_NAME, firstName);
//        values.put(KEY_NIK_NAME, nikName);
//        values.put(AGE,age);
//        values.put(SEX,sex);
//        values.put(IMAGE,image);
//
//
//
//
//        db.update(TABLE_USER,values,"uid = ?",new String[] { uid });
//        db.close();
//
//    }

//    public void updateAvatar(String uid,String image){
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues values = new ContentValues();
//        values.put(IMAGE,image);
//
//        db.update(TABLE_USER,values,"uid = ?",new String[] { uid });
//        db.close();
//    }

    /**
     * Getting user data from database
     * */
    public HashMap<String, String> getDataTokens() {
        HashMap<String, String> token = new HashMap<String, String>();
        String selectQuery = "SELECT  * FROM " + TABLE_DATA_TOKENS;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // Move to first row
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            token.put("access_token", cursor.getString(1));
            token.put("exp_access_token", cursor.getString(2));
            token.put("refresh_token", cursor.getString(3));
            token.put("exp_refresh_token", cursor.getString(4));

        }
        cursor.close();
        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + token.toString());

        return token;
    }
//
//    public HashMap<String, String> getUserDetails() {
//        HashMap<String, String> user = new HashMap<String, String>();
//        String selectQuery = "SELECT  * FROM " + TABLE_USER;
//
//        SQLiteDatabase db = this.getReadableDatabase();
//        Cursor cursor = db.rawQuery(selectQuery, null);
//        // Move to first row
//        cursor.moveToFirst();
//        if (cursor.getCount() > 0) {
//            user.put("uid", cursor.getString(1));
//            user.put("first_name", cursor.getString(2));
//            user.put("nik_name", cursor.getString(3));
//            user.put("age", cursor.getString(4));
//            user.put("sex", cursor.getString(5));
//            user.put("image", cursor.getString(6));
//            user.put("created_at", cursor.getString(7));
//
//
//        }
//        cursor.close();
//        db.close();
//        // return user
//        Log.d(TAG, "Fetching user from Sqlite: " + user.toString());
//
//        return user;
//    }

    /**
     * Re crate database Delete all tables and create them again
     * */
    public void deleteDataTokensTable() {
        SQLiteDatabase db = this.getWritableDatabase();

        // Delete All Rows
        db.delete(TABLE_DATA_TOKENS, null, null);
        db.close();

        Log.d(TAG, "Deleted all user info from sqlite");
    }

}