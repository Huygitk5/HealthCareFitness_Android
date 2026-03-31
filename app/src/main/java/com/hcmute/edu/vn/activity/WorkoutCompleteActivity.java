package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.CompletedExerciseAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.UserWorkoutSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutCompleteActivity extends AppCompatActivity {
    private static final String EXTRA_WORKOUT_CONTEXT_KEY = "WORKOUT_CONTEXT_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);

        TextView tvTotalExercises = findViewById(R.id.tvTotalExercises);
        TextView tvTotalCalories = findViewById(R.id.tvTotalCalories);
        TextView tvTotalTime = findViewById(R.id.tvTotalTime);
        String userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");
        String planId = getIntent().getStringExtra("EXTRA_PLAN_ID");
        String dayId = getIntent().getStringExtra("EXTRA_DAY_ID");
        boolean skipSaveWorkoutSession = getIntent().getBooleanExtra("SKIP_SAVE_WORKOUT_SESSION", false);
        boolean returnToHomeActivity = getIntent().getBooleanExtra("RETURN_TO_HOME_ACTIVITY", false);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String workoutContextKey = getIntent().getStringExtra(EXTRA_WORKOUT_CONTEXT_KEY);

        SharedPreferences wpPref = getSharedPreferences("WorkoutProgress", MODE_PRIVATE);
        String sessionKey = "SESSION_START_" + (
                workoutContextKey != null && !workoutContextKey.trim().isEmpty()
                        ? workoutContextKey
                        : userId + "_" + todayDate
        );
        long startMillis = wpPref.getLong(sessionKey, 0);
        long endMillis = System.currentTimeMillis();

        if (!skipSaveWorkoutSession && startMillis > 0 && planId != null && dayId != null) {
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
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                wpPref.edit().remove(sessionKey).apply();
                                android.widget.Toast.makeText(
                                        WorkoutCompleteActivity.this,
                                        "Đã đẩy phiên tập lên Supabase!",
                                        android.widget.Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                try {
                                    String errorBody = response.errorBody() != null
                                            ? response.errorBody().string()
                                            : "No error body";
                                    Log.e("WorkoutComplete", "API Error: " + errorBody);
                                    android.widget.Toast.makeText(
                                            WorkoutCompleteActivity.this,
                                            "Lỗi API: " + errorBody,
                                            android.widget.Toast.LENGTH_LONG
                                    ).show();
                                } catch (Exception ignored) {
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e("WorkoutComplete", "Failed to save session: " + t.getMessage());
                            android.widget.Toast.makeText(
                                    WorkoutCompleteActivity.this,
                                    "Lỗi mạng: " + t.getMessage(),
                                    android.widget.Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        } else if (startMillis > 0) {
            wpPref.edit().remove(sessionKey).apply();
        }

        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        RecyclerView rvCompletedExercises = findViewById(R.id.rvCompletedExercises);

        Intent intent = getIntent();
        int exercisesCount = intent.getIntExtra("TOTAL_EXERCISES", 0);
        double calories = intent.getDoubleExtra("TOTAL_CALORIES", 0.0);
        long timeInMillis = intent.getLongExtra("TOTAL_TIME_MILLIS", 0);

        ArrayList<String> listNames = intent.getStringArrayListExtra("LIST_NAMES");
        long[] listDurations = intent.getLongArrayExtra("LIST_DURATIONS");

        tvTotalExercises.setText(String.valueOf(exercisesCount));
        tvTotalCalories.setText(String.format(Locale.getDefault(), "%.1f", calories));

        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        tvTotalTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (listNames != null && listDurations != null) {
            rvCompletedExercises.setLayoutManager(new LinearLayoutManager(this));
            CompletedExerciseAdapter adapter = new CompletedExerciseAdapter(listNames, listDurations);
            rvCompletedExercises.setAdapter(adapter);
        }

        btnBackToHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(
                    WorkoutCompleteActivity.this,
                    returnToHomeActivity ? HomeActivity.class : WorkoutJourneyActivity.class
            );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });
    }
}
