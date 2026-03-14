package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hcmute.edu.vn.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WorkoutActivity extends AppCompatActivity {
    String username;

    // View cho phần Chọn Level Giáo Án
    private TextView tvBeginner, tvIntermediate, tvAdvanced;
    private TextView tvWorkoutPlanTitle;
    private ImageView ivWorkoutPlan;
    private CardView cardWorkoutPlan;

    // View cho phần Today's Workout (Hôm nay tập gì)
    private TextView tvCurrentTime, tvTodayWorkoutTitle;
    private ImageView ivTodayWorkout;
    private CardView cardTodayWorkout;

    // View cho phần Free Workout (Tập tự do)
    private CardView cardFreeWorkout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            float density = getResources().getDisplayMetrics().density;

            // 1. Xử lý Padding cho ScrollView
            ScrollView mainScrollView = findViewById(R.id.mainScrollView);
            if (mainScrollView != null) {
                int topPadding = systemBars.top + (int)(24 * density);
                int bottomPadding = systemBars.bottom + (int)(86 * density);
                mainScrollView.setPadding(0, topPadding, 0, bottomPadding);
            }

            // 2. Đẩy thanh Bottom Navigation lên trên thanh hệ thống
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                params.bottomMargin = systemBars.bottom + (int)(16 * density);
                bottomNav.setLayoutParams(params);
            }

            return insets;
        });

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // 1. Ánh xạ toàn bộ view (Tránh lỗi NullPointerException)
        initViews();

        // 2. Setup Logic cho Cụm Chọn Level
        setupLevelListeners();
        selectLevel("BEGINNER"); // Mặc định mở lên chọn thẻ Beginner

        // 3. Setup Logic cho Cụm "Hôm nay tập gì" (Next Day Logic)
        setupTodayWorkout();

        // 4. Cài đặt các sự kiện click chuyển trang (CardView)
        setupClickListeners();

        // 5. Cài đặt Bottom Navigation
        setupBottomNavigation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
    }


    private void initViews() {
        // Khởi tạo view cho Level
        tvBeginner = findViewById(R.id.tvBeginner);
        tvIntermediate = findViewById(R.id.tvIntermediate);
        tvAdvanced = findViewById(R.id.tvAdvanced);
        tvWorkoutPlanTitle = findViewById(R.id.tvWorkoutPlanTitle);
        ivWorkoutPlan = findViewById(R.id.ivWorkoutPlan);
        cardWorkoutPlan = findViewById(R.id.cardWorkoutPlan);

        // Khởi tạo view cho Today's Workout
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTodayWorkoutTitle = findViewById(R.id.tvTodayWorkoutTitle);
        ivTodayWorkout = findViewById(R.id.ivTodayWorkout);
        cardTodayWorkout = findViewById(R.id.cardTodayWorkout);

        // Khởi tạo view Free Workout
        cardFreeWorkout = findViewById(R.id.cardFreeWorkout);
    }

    private void setupClickListeners() {
        // Bấm vào Card Giáo Án -> Chuyển sang màn hình Chi tiết Giáo Án
        cardWorkoutPlan.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
            startActivity(intent);
        });

        // Bấm vào Card Tập Tự Do -> Chuyển sang màn hình Lọc bài tập
        cardFreeWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, EquipmentSelectionActivity.class);
            startActivity(intent);
        });
    }

    // =========================================================
    // LOGIC 1: TODAY'S WORKOUT (HÔM NAY TẬP GÌ)
    // =========================================================
    private void setupTodayWorkout() {
        // Cập nhật ngày tháng động
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String currentDate = sdf.format(new Date());
        tvCurrentTime.setText(currentDate);

        // TODO: Giả lập Logic lấy ngày tập tiếp theo từ Database
        int lastCompletedDay = 1; // Giả sử user vừa tập xong Ngày 1
        int nextDayOrder = lastCompletedDay + 1; // Hệ thống tự nhảy sang Ngày 2

        if (nextDayOrder == 1) {
            tvTodayWorkoutTitle.setText("Ngày 1: Ngực & Tay sau");
            ivTodayWorkout.setImageResource(R.drawable.workout_1);
        } else if (nextDayOrder == 2) {
            tvTodayWorkoutTitle.setText("Ngày 2: Lưng & Bắp tay trước");
            ivTodayWorkout.setImageResource(R.drawable.workout_2);
        } else {
            tvTodayWorkoutTitle.setText("Ngày " + nextDayOrder + ": Chân & Mông");
            ivTodayWorkout.setImageResource(R.drawable.workout_3);
        }

        // Sự kiện click bắt đầu tập
        cardTodayWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ExerciseListActivity.class);
            intent.putExtra("PLAN_ID", 1);
            intent.putExtra("DAY_ORDER", nextDayOrder);
            startActivity(intent);
        });
    }

    // =========================================================
    // LOGIC 2: CHOOSE WORKOUT PLAN (CHỌN LEVEL)
    // =========================================================
    private void setupLevelListeners() {
        tvBeginner.setOnClickListener(v -> selectLevel("BEGINNER"));
        tvIntermediate.setOnClickListener(v -> selectLevel("INTERMEDIATE"));
        tvAdvanced.setOnClickListener(v -> selectLevel("ADVANCED"));
    }

    private void selectLevel(String level) {
        resetLevelButtons();
        switch (level) {
            case "BEGINNER":
                tvBeginner.setBackgroundResource(R.drawable.bg_workout_chip_active);
                tvBeginner.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Khởi động & Làm quen");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv1);
                break;
            case "INTERMEDIATE":
                tvIntermediate.setBackgroundResource(R.drawable.bg_workout_chip_active);
                tvIntermediate.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Tăng cơ & Giảm mỡ");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv2);
                break;
            case "ADVANCED":
                tvAdvanced.setBackgroundResource(R.drawable.bg_workout_chip_active);
                tvAdvanced.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Đốt mỡ cường độ cao");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv3);
                break;
        }
    }

    private void resetLevelButtons() {
        tvBeginner.setBackgroundResource(R.drawable.bg_workout_chip_inactive);
        tvBeginner.setTextColor(Color.parseColor("#757575"));

        tvIntermediate.setBackgroundResource(R.drawable.bg_workout_chip_inactive);
        tvIntermediate.setTextColor(Color.parseColor("#757575"));

        tvAdvanced.setBackgroundResource(R.drawable.bg_workout_chip_inactive);
        tvAdvanced.setTextColor(Color.parseColor("#757575"));
    }

    // =========================================================
    // LOGIC 3: BOTTOM NAVIGATION
    // =========================================================
    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        navNutrition.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, NutritionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        LinearLayout navProfile = findViewById(R.id.nav_profile);
        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}