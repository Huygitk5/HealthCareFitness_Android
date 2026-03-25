package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;

public class RestActivity extends AppCompatActivity {

    private TextView tvTimer, tvNextExerciseName;
    private MaterialButton btnMinus10, btnPlus10, btnSkipRest;

    // Biến thời gian (Mặc định 20 giây)
    private int timeLeft = 20;

    // Bộ đếm thời gian linh hoạt
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);

        // 1. Ánh xạ View
        tvTimer = findViewById(R.id.tvTimer);
        tvNextExerciseName = findViewById(R.id.tvNextExerciseName);
        btnMinus10 = findViewById(R.id.btnMinus10);
        btnPlus10 = findViewById(R.id.btnPlus10);
        btnSkipRest = findViewById(R.id.btnSkipRest);

        // 2. Nhận tên bài tập tiếp theo từ Intent (Nếu có)
        String nextExercise = getIntent().getStringExtra("NEXT_EXERCISE_NAME");
        if (nextExercise != null && !nextExercise.isEmpty()) {
            tvNextExerciseName.setText("Tiếp theo: " + nextExercise);
        }

        updateTimerUI(); // Hiển thị số 20s lên màn hình ngay lập tức

        // 3. Bắt sự kiện các nút
        btnMinus10.setOnClickListener(v -> {
            timeLeft -= 10;
            if (timeLeft < 1) timeLeft = 1; // Không cho âm, tối thiểu 1s để kịp bấm qua bài
            updateTimerUI();
        });

        btnPlus10.setOnClickListener(v -> {
            timeLeft += 10;
            updateTimerUI();
        });

        btnSkipRest.setOnClickListener(v -> {
            finishRestAndGoNext();
        });

        // 4. Bắt đầu đếm ngược
        startTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                timeLeft--;
                updateTimerUI();

                if (timeLeft <= 0) {
                    // Hết giờ -> Tự động qua bài
                    finishRestAndGoNext();
                } else {
                    // Lặp lại việc đếm ngược sau 1000ms (1 giây)
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        // Kích hoạt ngay nhịp đếm đầu tiên
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void updateTimerUI() {
        tvTimer.setText(String.valueOf(timeLeft));
    }

    private void finishRestAndGoNext() {
        // Ngắt bộ đếm để tránh lỗi rò rỉ bộ nhớ (Memory Leak)
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }

        // Đóng màn hình nghỉ ngơi, quay lại bài tập tiếp theo
        // (Nếu bạn dùng StartActivityForResult thì set kết quả ở đây)
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cực kỳ quan trọng: Nếu user bấm nút Back thoát app giữa chừng, phải tắt đồng hồ
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}