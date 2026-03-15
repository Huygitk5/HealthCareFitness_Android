package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;

public class ExerciseListActivity extends AppCompatActivity {

    private ArrayList<Exercise> exercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_exercise_list);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvExercises), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        RecyclerView rv = findViewById(R.id.rvExercises);

        // Nhận dữ liệu bài tập đã chọn truyền từ Intent
        if (getIntent().hasExtra("EXTRA_EXERCISE_LIST")) {
            exercises = (ArrayList<Exercise>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");

            // Đổi tên Title thành Tập Tự Do (Tuỳ chọn hiển thị trên UI)
            TextView tvDayTitle = findViewById(R.id.tvDayTitle);
            if(tvDayTitle != null) {
                tvDayTitle.setText("Tập Tự Do");
            }
        } else {
            Toast.makeText(this, "Không tìm thấy dữ liệu bài tập", Toast.LENGTH_SHORT).show();
        }

        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ExerciseAdapter(exercises));

        // Nút Back trên cùng
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Nút START để bắt đầu vào màn hình tập (ExerciseActivity)
        findViewById(R.id.btnStartWorkout).setOnClickListener(v -> {
            if (exercises == null || exercises.isEmpty()) {
                Toast.makeText(ExerciseListActivity.this, "Danh sách bài tập trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExerciseListActivity.this, ExerciseActivity.class);
            intent.putExtra("EXTRA_EXERCISE_LIST", exercises);

            // Chuyển tiếp cờ "tập tự do" nếu có
            if (getIntent().hasExtra("IS_FREE_WORKOUT")) {
                intent.putExtra("IS_FREE_WORKOUT", getIntent().getBooleanExtra("IS_FREE_WORKOUT", false));
            }

            startActivity(intent);
        });
    }
}