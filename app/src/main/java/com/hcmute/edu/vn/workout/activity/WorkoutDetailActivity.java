package com.hcmute.edu.vn.workout.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.workout.adapter.WorkoutAdapter;
import com.hcmute.edu.vn.workout.model.WorkoutDay;

import java.util.ArrayList;
import java.util.List;

public class WorkoutDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_workout);

        RecyclerView rvWorkoutDays = findViewById(R.id.rvWorkoutDays);
        List<WorkoutDay> data = new ArrayList<>();

        // Seed dữ liệu 30 ngày tập
        for (int i = 1; i <= 30; i++) {
            if (i % 4 == 0) { // Cứ ngày thứ 4 là nghỉ
                data.add(new WorkoutDay("Ngày " + i, "Nghỉ ngơi", true));
            } else {
                data.add(new WorkoutDay("Ngày " + i, "11 Bài tập", false));
            }
        }

        WorkoutAdapter adapter = new WorkoutAdapter(data);
        rvWorkoutDays.setLayoutManager(new LinearLayoutManager(this));
        rvWorkoutDays.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}