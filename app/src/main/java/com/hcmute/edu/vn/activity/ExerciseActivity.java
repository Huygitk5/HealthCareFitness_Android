package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ExerciseActivity extends AppCompatActivity {

    private ArrayList<Exercise> exerciseList;
    private int currentIndex = 0;
    private String todayDate;
    private long currentExerciseStartTime;
    private long[] exerciseDurations;
    private ImageView ivExercise;
    private TextView tvExerciseName, tvTimer, tvExerciseProgress;
    private ImageButton btnNext, btnPrevious, btnClose;
    private Button btnPause;
    private ActivityResultLauncher<Intent> restActivityLauncher;

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);
        
        // Cấu hình thanh trạng thái
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // Lắng nghe (đợi đêm ngược xong)
        restActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Khi nghỉ ngơi xong -> Tăng biến đếm và load bài tập tiếp theo
                        if (currentIndex < exerciseList.size() - 1) {
                            currentIndex++;
                            updateExerciseUI();
                        }
                    }
                }
        );

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_EXERCISE_LIST")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                exerciseList = intent.getSerializableExtra("EXTRA_EXERCISE_LIST", ArrayList.class);
            } else {
                exerciseList = (ArrayList<Exercise>) intent.getSerializableExtra("EXTRA_EXERCISE_LIST");
            }
        }

        if (exerciseList != null && !exerciseList.isEmpty()) {
            exerciseDurations = new long[exerciseList.size()];
            // Lấy userId hiện tại
            String currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");

            SharedPreferences pref = getSharedPreferences("WorkoutProgress", MODE_PRIVATE);
            currentIndex = pref.getInt("CURRENT_INDEX_" + currentUserId + "_" + todayDate, 0);

            if (currentIndex >= exerciseList.size()) {
                currentIndex = 0;
                pref.edit()
                        .putInt("CURRENT_INDEX_" + currentUserId + "_" + todayDate, 0)
                        .putInt("PROGRESS_" + currentUserId + "_" + todayDate, 0)
                        .apply();
            }
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
            exerciseDurations[currentIndex] += (System.currentTimeMillis() - currentExerciseStartTime);
            int completedCount = currentIndex + 1;
            saveDailyProgress(completedCount);
            if (currentIndex < exerciseList.size() - 1) {
                // Lấy tên bài tập tiếp theo để truyền sang màn hình Nghỉ ngơi hiển thị
                String nextExerciseName = exerciseList.get(currentIndex + 1).getName();
                if (nextExerciseName == null) nextExerciseName = "Bài tập tiếp theo";

                // Mở màn hình Nghỉ ngơi
                Intent intent = new Intent(ExerciseActivity.this, RestActivity.class);
                intent.putExtra("NEXT_EXERCISE_NAME", nextExerciseName);

                // Dùng launcher để phóng intent đi và chờ nó về
                restActivityLauncher.launch(intent);
            } else {
                // TÍNH TOÁN DỮ LIỆU ĐỂ TRUYỀN SANG MÀN HÌNH CHÚC MỪNG
                long totalTime = 0;
                ArrayList<String> exerciseNames = new ArrayList<>();
                for (int i = 0; i < exerciseList.size(); i++) {
                    totalTime += exerciseDurations[i];
                    exerciseNames.add(exerciseList.get(i).getName());
                }

                // Tính tạm Calo (Ví dụ: 8 kcal / phút)
                double totalMinutes = totalTime / 60000.0;
                double totalCalories = totalMinutes * 8.0;

                Intent intent = new Intent(ExerciseActivity.this, WorkoutCompleteActivity.class);
                intent.putExtra("TOTAL_EXERCISES", exerciseList.size());
                intent.putExtra("TOTAL_TIME_MILLIS", totalTime);
                intent.putExtra("TOTAL_CALORIES", totalCalories);

                // Gửi danh sách bài và thời gian từng bài
                intent.putStringArrayListExtra("LIST_NAMES", exerciseNames);
                intent.putExtra("LIST_DURATIONS", exerciseDurations);

                startActivity(intent);
                finish(); // Đóng luôn màn hình tập hiện tại
            }
        });

        btnPrevious.setOnClickListener(v -> {
            exerciseDurations[currentIndex] += (System.currentTimeMillis() - currentExerciseStartTime);
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
        currentExerciseStartTime = System.currentTimeMillis();
    }

    // =======================================================
    // HÀM LƯU TIẾN TRÌNH TẬP THEO NGÀY
    // =======================================================
    private void saveDailyProgress(int completedExercises) {
        if (exerciseList == null || exerciseList.isEmpty()) return;

        // Lấy userId hiện tại
        String currentUserId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");

        SharedPreferences pref = getSharedPreferences("WorkoutProgress", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        // 1. Lưu lại Index có gắn UserID
        editor.putInt("CURRENT_INDEX_" + currentUserId + "_" + todayDate, completedExercises);

        // 2. Tính và lưu phần trăm có gắn UserID
        int progressPercent = (int) (((float) completedExercises / exerciseList.size()) * 100);
        int currentSavedPercent = pref.getInt("PROGRESS_" + currentUserId + "_" + todayDate, 0);

        if (progressPercent > currentSavedPercent) {
            editor.putInt("PROGRESS_" + currentUserId + "_" + todayDate, progressPercent);
        }

        editor.apply();
    }
}