package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        // 1. Ánh xạ View
        initViews();

        // 2. Nhận danh sách bài tập từ Intent
        if (getIntent() != null && getIntent().hasExtra("EXTRA_EXERCISE_LIST")) {
            try {
                exerciseList = (ArrayList<Exercise>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");
            } catch (Exception e) {
                Log.e("ExerciseActivity", "Lỗi nhận dữ liệu Intent: " + e.getMessage());
            }
        }

        // 3. Hiển thị bài tập đầu tiên
        if (exerciseList != null && !exerciseList.isEmpty()) {
            currentIndex = 0;
            updateExerciseUI();
        } else {
            Toast.makeText(this, "Không có dữ liệu bài tập!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 4. Lắng nghe sự kiện click Next/Prev
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
        // Bấm X để đóng
        btnClose.setOnClickListener(v -> finish());

        // Bấm Next để qua bài
        btnNext.setOnClickListener(v -> {
            if (currentIndex < exerciseList.size() - 1) {
                currentIndex++;
                updateExerciseUI();
            }
        });

        // Bấm Prev để lùi bài
        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateExerciseUI();
            }
        });

        // Tạm thời để nút Pause
        btnPause.setOnClickListener(v -> {
            Toast.makeText(this, "Đã bấm Pause", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Hàm lấy bài tập hiện tại và nạp vào Giao diện
     */
    private void updateExerciseUI() {
        if (exerciseList == null || exerciseList.isEmpty()) return;

        Exercise currentExercise = exerciseList.get(currentIndex);

        // Đổ Tên và Thời gian
        if (currentExercise.getName() != null) {
            tvExerciseName.setText(currentExercise.getName().toUpperCase());
        }

        if (currentExercise.getBaseRecommendedReps() != null) {
            tvTimer.setText(currentExercise.getBaseRecommendedReps()); 
        }

        // --- XỬ LÝ ẢNH ---
        try {
            String imageString = currentExercise.getImageUrl();

            if (imageString != null && !imageString.isEmpty()) {
                // Giả sử imageUrl chứa ID của resource (ví dụ: "2131230856")
                int imageResId = Integer.parseInt(imageString);
                ivExercise.setImageResource(imageResId);
            } else {
                ivExercise.setImageResource(R.drawable.workout_1);
            }
        } catch (Exception e) {
            Log.e("ExerciseActivity", "Lỗi load ảnh: " + e.getMessage());
            ivExercise.setImageResource(R.drawable.workout_1);
        }

        // Cập nhật bộ đếm (Ví dụ: 1 / 11)
        if (tvExerciseProgress != null) {
            String progressText = (currentIndex + 1) + " / " + exerciseList.size();
            tvExerciseProgress.setText(progressText);
        }

        // Ẩn hiện nút Next/Prev dựa vào vị trí bài tập
        btnPrevious.setVisibility(currentIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setVisibility(currentIndex == exerciseList.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }
}