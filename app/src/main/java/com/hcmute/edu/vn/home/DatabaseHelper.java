package com.hcmute.edu.vn.home;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hcmute.edu.vn.home.model.User;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ProductManager.db";
    private static final int DATABASE_VERSION = 1;

    // User
    private static final String TABLE_USER = "users";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_FULLNAME = "fullname";
    private static final String COLUMN_DOB = "dob";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_HEIGHT = "height";
    private static final String COLUMN_WEIGHT = "weight";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                COLUMN_USERNAME + " TEXT PRIMARY KEY, " +
                COLUMN_PASSWORD + " TEXT, " +
                COLUMN_FULLNAME + " TEXT, " +
                COLUMN_DOB + " TEXT, " +
                COLUMN_GENDER + " TEXT, " +
                COLUMN_ADDRESS + " TEXT, " +
                COLUMN_HEIGHT + " REAL, " +
                COLUMN_WEIGHT + " REAL)";
        db.execSQL(createUserTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    public boolean addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_PASSWORD, user.getPassword());
        values.put(COLUMN_FULLNAME, user.getFullName());
        values.put(COLUMN_DOB, user.getDob());
        values.put(COLUMN_GENDER, user.getGender());
        values.put(COLUMN_ADDRESS, user.getAddress());
        values.put(COLUMN_HEIGHT, user.getHeight());
        values.put(COLUMN_WEIGHT, user.getWeight());

        long result = db.insert(TABLE_USER, null, values);
        db.close();
        return result != -1;
    }

    public boolean updateUserProfile(String username, String fullname, String dob, String gender, String address, double height, double weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_FULLNAME, fullname);
        values.put(COLUMN_DOB, dob);
        values.put(COLUMN_GENDER, gender);
        values.put(COLUMN_ADDRESS, address);
        values.put(COLUMN_HEIGHT, height);
        values.put(COLUMN_WEIGHT, weight);


        long result = db.update(TABLE_USER, values, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();

        return result != -1;
    }

    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " WHERE " +
                COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public User getUserDetails(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});
        User user = null;
        if (cursor.moveToFirst()) {
            String pass = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PASSWORD));
            String full = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FULLNAME));
            String dob = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOB));
            String gen = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
            String addr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS));
            double height = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_HEIGHT));
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_WEIGHT));
            user = new User(username, pass, full, dob, gen, addr, height, weight);
        }
        cursor.close();
        return user;
    }
}