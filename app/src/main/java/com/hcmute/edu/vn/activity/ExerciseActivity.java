package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;

public class ExerciseActivity extends AppCompatActivity {

    private ArrayList<Exercise> exerciseList;
    private int currentIndex = 0;

    private ImageView ivExercise;
    private TextView tvExerciseName, tvTimer, tvExerciseProgress;
    private ImageButton btnNext, btnPrevious, btnClose;
    private Button btnPause;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);
        
        // Cấu hình thanh trạng thái
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_EXERCISE_LIST")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                exerciseList = intent.getSerializableExtra("EXTRA_EXERCISE_LIST", ArrayList.class);
            } else {
                exerciseList = (ArrayList<Exercise>) intent.getSerializableExtra("EXTRA_EXERCISE_LIST");
            }
        }

        if (exerciseList != null && !exerciseList.isEmpty()) {
            currentIndex = 0;
            updateExerciseUI();
        } else {
            Toast.makeText(this, "Không có dữ liệu bài tập!", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupListeners();
    }

    private void initViews() {
        ivExercise = findViewById(R.id.ivExercise);
        tvExerciseName = findViewById(R.id.tvExerciseName);
        tvTimer = findViewById(R.id.tvTimer);
        tvExerciseProgress = findViewById(R.id.tvExerciseProgress);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnClose = findViewById(R.id.btnClose);
        btnPause = findViewById(R.id.btnPause);
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (currentIndex < exerciseList.size() - 1) {
                currentIndex++;
                updateExerciseUI();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateExerciseUI();
            }
        });

        btnPause.setOnClickListener(v -> {
            Toast.makeText(ExerciseActivity.this, "Đã bấm Pause", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateExerciseUI() {
        if (exerciseList == null || exerciseList.isEmpty()) return;

        Exercise currentExercise = exerciseList.get(currentIndex);

        // 1. Set tên bài tập
        if (currentExercise.getName() != null) {
            tvExerciseName.setText(currentExercise.getName().toUpperCase());
        }

        // 2. Xử lý hiển thị Reps/Thời gian và Nút Pause
        if (currentExercise.getBaseRecommendedReps() != null) {
            String repsData = currentExercise.getBaseRecommendedReps();
            tvTimer.setText(repsData);

            // Kiểm tra: Nếu chuỗi có chứa dấu ":" (ví dụ 00:30) thì là đếm giờ -> Hiện nút Pause
            // Ngược lại (ví dụ 15, 12) thì là đếm số lần (Reps) -> Ẩn nút Pause
            if (repsData.contains(":")) {
                btnPause.setVisibility(View.VISIBLE);
            } else {
                btnPause.setVisibility(View.INVISIBLE);
            }
        } else {
            // Đề phòng trường hợp dữ liệu null
            btnPause.setVisibility(View.INVISIBLE);
        }

        // 3. Load ảnh bằng Glide
        com.bumptech.glide.Glide.with(this)
                .load(currentExercise.getImageUrl()) // Lấy link ảnh từ Supabase
                .placeholder(R.drawable.workout_1)   // Ảnh chờ trong lúc load mạng
                .error(R.drawable.workout_1)         // Ảnh mặc định nếu link hỏng/không có link
                .into(ivExercise);

        // 4. Set số thứ tự bài tập
        String progressText = (currentIndex + 1) + " / " + exerciseList.size();
        tvExerciseProgress.setText(progressText);

        // 5. Ẩn/Hiện nút Next, Previous ở đầu/cuối danh sách
        btnPrevious.setVisibility(currentIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setVisibility(currentIndex == exerciseList.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }
}