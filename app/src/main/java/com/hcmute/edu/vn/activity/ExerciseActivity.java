package com.hcmute.edu.vn.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;

public class ExerciseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);

        ArrayList<Exercise> list = (ArrayList<Exercise>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");

        if (list != null && !list.isEmpty()) {
            // Log hoặc Toast để kiểm tra đã nhận được data chưa
            android.widget.Toast.makeText(this, "Bắt đầu " + list.size() + " bài tập!", android.widget.Toast.LENGTH_SHORT).show();
        }

        // Xử lý nút back để quay lại danh sách
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
    }
}