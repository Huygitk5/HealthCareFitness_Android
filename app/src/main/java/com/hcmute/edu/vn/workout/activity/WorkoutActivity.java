package com.hcmute.edu.vn.workout.activity;

import android.content.Intent;
import android.graphics.Color; // Import thêm thư viện màu sắc
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView; // Import ImageView
import android.widget.LinearLayout;
import android.widget.TextView; // Import TextView

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.home.activity.HomeActivity;
import com.hcmute.edu.vn.nutrition.activity.NutritionActivity;
import com.hcmute.edu.vn.profile.ProfileActivity;

public class WorkoutActivity extends AppCompatActivity {
    String username;

    // =========================================================
    // KHAI BÁO CÁC VIEW CHO PHẦN CHỌN LEVEL
    // =========================================================
    private TextView tvBeginner, tvIntermediate, tvAdvanced;
    private TextView tvWorkoutPlanTitle;
    private ImageView ivWorkoutPlan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // =========================================================
        // KHỞI TẠO VÀ XỬ LÝ GIAO DIỆN LEVEL
        // =========================================================
        initLevelViews();
        setupLevelListeners();
        selectLevel("BEGINNER"); // Mặc định chọn thẻ Beginner khi mở màn hình

        // Thêm đoạn này vào cuối onCreate của WorkoutActivity
        findViewById(R.id.cardWorkoutPlan).setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
            startActivity(intent);
        });

        // =========================================================
        // XỬ LÝ CLICK BOTTOM NAVIGATION TRONG WORKOUT
        // =========================================================

        LinearLayout navHome = findViewById(R.id.nav_home);
        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WorkoutActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        navNutrition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WorkoutActivity.this, NutritionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        LinearLayout navProfile = findViewById(R.id.nav_profile);
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WorkoutActivity.this, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        CardView cardFreeWorkout = findViewById(R.id.cardFreeWorkout);

        cardFreeWorkout.setOnClickListener(v -> {
            // Chuyển sang màn hình Lọc bài tập tự do
            Intent intent = new Intent(WorkoutActivity.this, FreeWorkoutFilterActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
    }

    // =========================================================
    // CÁC HÀM XỬ LÝ LOGIC ĐỔI LEVEL (Tách riêng để code sạch)
    // =========================================================

    private void initLevelViews() {
        tvBeginner = findViewById(R.id.tvBeginner);
        tvIntermediate = findViewById(R.id.tvIntermediate);
        tvAdvanced = findViewById(R.id.tvAdvanced);
        tvWorkoutPlanTitle = findViewById(R.id.tvWorkoutPlanTitle);
        ivWorkoutPlan = findViewById(R.id.ivWorkoutPlan);
    }

    private void setupLevelListeners() {
        tvBeginner.setOnClickListener(v -> selectLevel("BEGINNER"));
        tvIntermediate.setOnClickListener(v -> selectLevel("INTERMEDIATE"));
        tvAdvanced.setOnClickListener(v -> selectLevel("ADVANCED"));
    }

    private void selectLevel(String level) {
        // Đưa tất cả các nút về trạng thái xám trước
        resetLevelButtons();

        // Tùy theo Level mà bật sáng nút và đổi nội dung thẻ CardView
        switch (level) {
            case "BEGINNER":
                tvBeginner.setBackgroundResource(R.drawable.bg_workout_level_button);
                tvBeginner.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Khởi động & Làm quen");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv1);
                break;

            case "INTERMEDIATE":
                tvIntermediate.setBackgroundResource(R.drawable.bg_workout_level_button);
                tvIntermediate.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Tăng cơ & Giảm mỡ");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv2);
                break;

            case "ADVANCED":
                tvAdvanced.setBackgroundResource(R.drawable.bg_workout_level_button);
                tvAdvanced.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Đốt mỡ cường độ cao");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv3);
                break;
        }
    }

    private void resetLevelButtons() {
        // Đưa nút về background xám và chữ màu xám
        tvBeginner.setBackgroundResource(R.drawable.bg_workout_level_inactive);
        tvBeginner.setTextColor(Color.parseColor("#888888"));

        tvIntermediate.setBackgroundResource(R.drawable.bg_workout_level_inactive);
        tvIntermediate.setTextColor(Color.parseColor("#888888"));

        tvAdvanced.setBackgroundResource(R.drawable.bg_workout_level_inactive);
        tvAdvanced.setTextColor(Color.parseColor("#888888"));
    }
}