package com.hcmute.edu.vn.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.WorkoutAdapter;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkoutDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Kích hoạt giao diện tràn viền chuẩn Material 3
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_workout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvWorkoutDays), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        RecyclerView rvWorkoutDays = findViewById(R.id.rvWorkoutDays);
        List<WorkoutDay> data = new ArrayList<>();

        String dummyPlanId = UUID.randomUUID().toString(); // Tạo ID ảo cho giáo án

        // 2. Seed dữ liệu 30 ngày tập theo đúng chuẩn Model mới
        for (int i = 1; i <= 30; i++) {
            List<WorkoutDayExercise> dailyExercises = new ArrayList<>();
            String dayName = "Ngày " + i;

            if (i % 4 == 0) {
            } else {
                for (int j = 0; j < 11; j++) {
                    dailyExercises.add(new WorkoutDayExercise(null, 3, "x15", 30));
                }
            }

            data.add(new WorkoutDay(UUID.randomUUID().toString(), dummyPlanId, dayName, i, dailyExercises));
        }

        WorkoutAdapter adapter = new WorkoutAdapter(data);
        rvWorkoutDays.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutDays.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}