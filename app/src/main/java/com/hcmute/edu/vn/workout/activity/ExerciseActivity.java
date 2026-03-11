package com.hcmute.edu.vn.workout.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.workout.model.ExerciseItem;

import java.util.ArrayList;

public class ExerciseActivity extends AppCompatActivity {

    private ArrayList<ExerciseItem> exerciseList;
    private int currentIndex = 0;

    // Khai báo các View
    private ImageView ivExercise;
    private TextView tvExerciseName, tvTimer, tvExerciseProgress;
    private ImageButton btnNext, btnPrevious, btnClose;
    private Button btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);

        // 1. Ánh xạ View (FindViewById)
        initViews();

        // 2. Nhận dữ liệu từ Intent
        if (getIntent() != null && getIntent().hasExtra("EXTRA_EXERCISE_LIST")) {
            exerciseList = (ArrayList<ExerciseItem>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");
        }

        // 3. Kiểm tra danh sách và hiển thị bài tập đầu tiên
        if (exerciseList != null && !exerciseList.isEmpty()) {
            currentIndex = 0;
            updateExerciseUI(); // Hàm tự viết để nạp dữ liệu lên UI
        } else {
            Toast.makeText(this, "Không có dữ liệu bài tập!", Toast.LENGTH_SHORT).show();
            finish(); // Đóng màn hình nếu lỗi
        }

        // 4. Bắt sự kiện click cho các nút
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
        // Nút Đóng
        btnClose.setOnClickListener(v -> finish());

        // Nút Next (Qua bài)
        btnNext.setOnClickListener(v -> {
            if (currentIndex < exerciseList.size() - 1) {
                currentIndex++; // Tăng vị trí lên 1
                updateExerciseUI();
            }
        });

        // Nút Previous (Lùi bài)
        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--; // Giảm vị trí đi 1
                updateExerciseUI();
            }
        });

        // Nút Pause (Tạm thời để trống, ta sẽ làm Timer sau)
        btnPause.setOnClickListener(v -> {
            Toast.makeText(ExerciseActivity.this, "Đã bấm Pause", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Hàm này có nhiệm vụ: Lấy bài tập ở vị trí currentIndex ra và nhét dữ liệu vào UI.
     * Tách hàm này ra giúp code không bị lặp lại (DRY principle).
     */
    private void updateExerciseUI() {
        ExerciseItem currentExercise = exerciseList.get(currentIndex);

        // Đổ dữ liệu vào View
        tvExerciseName.setText(currentExercise.getName().toUpperCase());
        tvTimer.setText(currentExercise.getDuration());
        ivExercise.setImageResource(currentExercise.getImageResId());

        // Cập nhật bộ đếm (Ví dụ: 1 / 11)
        String progressText = (currentIndex + 1) + " / " + exerciseList.size();
        tvExerciseProgress.setText(progressText);

        // --- XỬ LÝ BIÊN (EDGE CASES) ---
        // Nếu đang ở bài ĐẦU TIÊN -> Ẩn nút Previous
        if (currentIndex == 0) {
            btnPrevious.setVisibility(View.INVISIBLE);
        } else {
            btnPrevious.setVisibility(View.VISIBLE);
        }

        // Nếu đang ở bài CUỐI CÙNG -> Đổi nút Next thành mờ hoặc có thể đổi icon thành dấu Tick
        if (currentIndex == exerciseList.size() - 1) {
            btnNext.setVisibility(View.INVISIBLE);
        } else {
            btnNext.setVisibility(View.VISIBLE);
        }

    }
}