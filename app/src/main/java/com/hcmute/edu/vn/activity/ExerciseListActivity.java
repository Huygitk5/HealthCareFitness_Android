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
import com.hcmute.edu.vn.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExerciseListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_exercise_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvExercises), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvExercises);
        List<Exercise> exercises = new ArrayList<>();

        // Seed dữ liệu đủ 11 bài tập mẫu
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Crunches", "Bài tập cơ bụng", 1, 1, 3, "00:30", "", String.valueOf(R.drawable.workout_2), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Plank", "Giữ thăng bằng", 1, 1, 3, "01:00", "", String.valueOf(R.drawable.workout_1), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Push Ups", "Hít đất cơ bản", 2, 2, 3, "x15", "", String.valueOf(R.drawable.workout_3), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Leg Raises", "Nâng chân", 1, 2, 3, "00:45", "", String.valueOf(R.drawable.workout_2), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Russian Twist", "Xoay lườn", 1, 2, 3, "00:30", "", String.valueOf(R.drawable.workout_1), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Squats", "Gập gối", 3, 1, 3, "x20", "", String.valueOf(R.drawable.workout_3), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Jumping Jacks", "Bài tập Cardio toàn thân", 4, 1, 3, "01:00", "", String.valueOf(R.drawable.workout_1), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Lunges", "Chùng chân tập đùi và mông", 3, 2, 3, "x16", "", String.valueOf(R.drawable.workout_2), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "High Knees", "Chạy nâng cao đùi", 4, 2, 3, "00:45", "", String.valueOf(R.drawable.workout_3), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Burpees", "Tập toàn thân cường độ cao", 4, 3, 3, "x10", "", String.valueOf(R.drawable.workout_1), null));
        exercises.add(new Exercise(UUID.randomUUID().toString(), "Bicycle Crunches", "Gập bụng đạp xe", 1, 2, 3, "00:45", "", String.valueOf(R.drawable.workout_2), null));

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ExerciseAdapter(exercises));

        // Xử lý nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}