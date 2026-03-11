package com.hcmute.edu.vn.workout.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.workout.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.workout.model.ExerciseItem;

import java.util.ArrayList;
import java.util.List;

public class ExerciseListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise_list);

        RecyclerView rv = findViewById(R.id.rvExercises);
        List<ExerciseItem> exercises = new ArrayList<>();

        // Seed dữ liệu 11 bài tập mẫu
        exercises.add(new ExerciseItem("Crunches", "00:30", R.drawable.workout_2));
        exercises.add(new ExerciseItem("Plank", "01:00", R.drawable.workout_1));
        exercises.add(new ExerciseItem("Push Ups", "x15", R.drawable.workout_3));
        exercises.add(new ExerciseItem("Leg Raises", "00:45", R.drawable.workout_2));
        exercises.add(new ExerciseItem("Russian Twist", "00:30", R.drawable.workout_1));
        // Thêm cho đủ bài...

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ExerciseAdapter(exercises));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}