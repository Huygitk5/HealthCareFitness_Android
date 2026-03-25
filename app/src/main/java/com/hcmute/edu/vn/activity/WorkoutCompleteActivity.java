package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.UserWorkoutSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutCompleteActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        String userId  = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");
        String planId  = getIntent().getStringExtra("EXTRA_PLAN_ID");
        String dayId   = getIntent().getStringExtra("EXTRA_DAY_ID");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        SharedPreferences wpPref = getSharedPreferences("WorkoutProgress", MODE_PRIVATE);
        long startMillis = wpPref.getLong("SESSION_START_" + userId + "_" + todayDate, 0);
        long endMillis   = System.currentTimeMillis();

        if (startMillis > 0 && planId != null && dayId != null) {
            SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            UserWorkoutSession session = new UserWorkoutSession(
                UUID.randomUUID().toString(),
                userId,
                planId,
                dayId,
                isoFmt.format(new Date(startMillis)),
                isoFmt.format(new Date(endMillis)),
                null
            );

            SupabaseClient.getClient().create(SupabaseApiService.class)
                .saveWorkoutSession(session)
                .enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        wpPref.edit().remove("SESSION_START_" + userId + "_" + todayDate).apply();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {
                        Log.e("WorkoutComplete", "Failed to save session: " + t.getMessage());
                    }
                });
        }

        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        btnBackToHome.setOnClickListener(v -> {
            // Quay về màn hình hành trình tập luyện (hoặc trang chủ)
            Intent intent = new Intent(WorkoutCompleteActivity.this, WorkoutJourneyActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}