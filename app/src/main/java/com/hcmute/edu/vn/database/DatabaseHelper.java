package com.hcmute.edu.vn.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.hcmute.edu.vn.model.User;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "HealthCareFitness.db";
    private static final int DATABASE_VERSION = 3;

    private static final String TABLE_USER = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DOB = "date_of_birth";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_HEIGHT = "height";
    private static final String COLUMN_WEIGHT = "weight";
    private static final String COLUMN_FITNESS_GOAL_ID = "fitness_goal_id";
    private static final String COLUMN_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUserTable = "CREATE TABLE " + TABLE_USER + " (" +
                COLUMN_ID + " TEXT, " +
                COLUMN_USERNAME + " TEXT PRIMARY KEY, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_DOB + " TEXT, " +
                COLUMN_GENDER + " TEXT, " +
                COLUMN_HEIGHT + " REAL, " +
                COLUMN_WEIGHT + " REAL, " +
                COLUMN_FITNESS_GOAL_ID + " INTEGER, " +
                COLUMN_CREATED_AT + " TEXT)";
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

        values.put(COLUMN_ID, user.getId());
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_NAME, user.getName());
        values.put(COLUMN_DOB, user.getDateOfBirth());
        values.put(COLUMN_GENDER, user.getGender());
        values.put(COLUMN_HEIGHT, user.getHeight());
        values.put(COLUMN_WEIGHT, user.getWeight());
        values.put(COLUMN_FITNESS_GOAL_ID, user.getFitnessGoalId());
        values.put(COLUMN_CREATED_AT, user.getCreatedAt());

        long result = db.insert(TABLE_USER, null, values);
        db.close();
        return result != -1;
    }

    public boolean updateUserProfile(String username, String name, String dob, String gender, Double height, Double weight, Integer fitnessGoalId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, name);
        values.put(COLUMN_DOB, dob);
        values.put(COLUMN_GENDER, gender);
        values.put(COLUMN_HEIGHT, height);
        values.put(COLUMN_WEIGHT, weight);
        values.put(COLUMN_FITNESS_GOAL_ID, fitnessGoalId);

        long result = db.update(TABLE_USER, values, COLUMN_USERNAME + " = ?", new String[]{username});
        db.close();

        return result != -1;
    }

    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public User getUserDetails(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USER + " WHERE " + COLUMN_USERNAME + " = ?", new String[]{username});
        User user = null;

        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
            String dob = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DOB));
            String gender = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER));
            String createdAt = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));

            // Xử lý chống null cho các Object kiểu số (Double, Integer)
            int heightIndex = cursor.getColumnIndexOrThrow(COLUMN_HEIGHT);
            Double height = cursor.isNull(heightIndex) ? null : cursor.getDouble(heightIndex);

            int weightIndex = cursor.getColumnIndexOrThrow(COLUMN_WEIGHT);
            Double weight = cursor.isNull(weightIndex) ? null : cursor.getDouble(weightIndex);

            int goalIndex = cursor.getColumnIndexOrThrow(COLUMN_FITNESS_GOAL_ID);
            Integer fitnessGoalId = cursor.isNull(goalIndex) ? null : cursor.getInt(goalIndex);

            user = new User(id, username, name, dob, gender, height, weight, fitnessGoalId, createdAt, null);
        }
        cursor.close();
        return user;
    }
}