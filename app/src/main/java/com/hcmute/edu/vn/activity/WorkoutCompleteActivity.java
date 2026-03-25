package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.CompletedExerciseAdapter; // Nhớ import Adapter nhé

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class WorkoutCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_complete);

        // Làm thanh trạng thái chữ trắng (để phù hợp với ảnh nền tối phía trên)
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(false);

        // 1. Ánh xạ View
        TextView tvTotalExercises = findViewById(R.id.tvTotalExercises);
        TextView tvTotalCalories = findViewById(R.id.tvTotalCalories);
        TextView tvTotalTime = findViewById(R.id.tvTotalTime);
        MaterialButton btnBackToHome = findViewById(R.id.btnBackToHome);
        RecyclerView rvCompletedExercises = findViewById(R.id.rvCompletedExercises);

        // 2. Nhận dữ liệu từ ExerciseActivity truyền sang
        Intent intent = getIntent();
        int exercisesCount = intent.getIntExtra("TOTAL_EXERCISES", 0);
        double calories = intent.getDoubleExtra("TOTAL_CALORIES", 0.0);
        long timeInMillis = intent.getLongExtra("TOTAL_TIME_MILLIS", 0);

        // Nhận mảng danh sách bài tập và thời gian
        ArrayList<String> listNames = intent.getStringArrayListExtra("LIST_NAMES");
        long[] listDurations = intent.getLongArrayExtra("LIST_DURATIONS");

        // 3. Hiển thị thông số tổng quát lên 3 cột
        tvTotalExercises.setText(String.valueOf(exercisesCount));
        tvTotalCalories.setText(String.format(Locale.getDefault(), "%.1f", calories));

        // Format Thời gian từ millisecond sang dạng MM:SS
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        tvTotalTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        // 4. Cài đặt danh sách chi tiết từng bài tập ở khoảng trắng phía dưới
        if (listNames != null && listDurations != null) {
            rvCompletedExercises.setLayoutManager(new LinearLayoutManager(this));
            CompletedExerciseAdapter adapter = new CompletedExerciseAdapter(listNames, listDurations);
            rvCompletedExercises.setAdapter(adapter);
        }

        // 5. Xử lý nút Tiếp theo (Quay về trang hành trình)
        btnBackToHome.setOnClickListener(v -> {
            Intent homeIntent = new Intent(WorkoutCompleteActivity.this, WorkoutJourneyActivity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });
    }

}