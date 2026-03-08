package com.hcmute.edu.vn.workout.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;

public class ExerciseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);

        // Xử lý nút back để quay lại danh sách
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}