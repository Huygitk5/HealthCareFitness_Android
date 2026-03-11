package com.hcmute.edu.vn.workout.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.workout.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.workout.model.ExerciseItem;

import java.util.ArrayList;

public class ExerciseListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise_list);

        RecyclerView rv = findViewById(R.id.rvExercises);
        ArrayList<ExerciseItem> exercises = new ArrayList<>();

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

        findViewById(R.id.btnStartWorkout).setOnClickListener(v -> {
            // Intent chứa action mang theo data để chuyển sang ExerciseActivity
            Intent intent = new Intent(ExerciseListActivity.this, ExerciseActivity.class);
            // Truyền mảng exercises qua Intent với một key định danh
            intent.putExtra("EXTRA_EXERCISE_LIST", exercises);
            startActivity(intent);
        });
    }
}