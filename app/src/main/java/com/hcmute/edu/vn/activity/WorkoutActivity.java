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

    private TextView tvBeginner, tvIntermediate, tvAdvanced;
    private TextView tvWorkoutPlanTitle;
    private ImageView ivWorkoutPlan;
    private CardView cardWorkoutPlan;

    private TextView tvCurrentTime, tvTodayWorkoutTitle;
    private ImageView ivTodayWorkout;
    private CardView cardTodayWorkout;

    private CardView cardFreeWorkout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // ĐỒNG BỘ THANH TÁC VỤ VỚI CÁC TAB KHÁC
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            // Chỉ Padding cho ScrollView để nội dung không bị navigation che khuất
            ScrollView mainScrollView = findViewById(R.id.mainScrollView);
            if (mainScrollView != null) {
                // Thêm padding cho nội dung, giữ navigation sát đáy
                mainScrollView.setPadding(0, systemBars.top, 0, 70); // 70 là chiều cao của bottomNav
            }

            // Thanh Bottom Navigation sẽ để mặc định, không can thiệp padding/margin dư thừa
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomNav.getLayoutParams();
                params.bottomMargin = 0; 
                bottomNav.setLayoutParams(params);
                // Xóa bỏ hoàn toàn padding bottom dư thừa gây dịch icon lên
                bottomNav.setPadding(0, 0, 0, 0);
            }

            return insets;
        });

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        initViews();
        setupLevelListeners();
        selectLevel("BEGINNER");
        setupTodayWorkout();
        setupClickListeners();
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
        tvBeginner = findViewById(R.id.tvBeginner);
        tvIntermediate = findViewById(R.id.tvIntermediate);
        tvAdvanced = findViewById(R.id.tvAdvanced);
        tvWorkoutPlanTitle = findViewById(R.id.tvWorkoutPlanTitle);
        ivWorkoutPlan = findViewById(R.id.ivWorkoutPlan);
        cardWorkoutPlan = findViewById(R.id.cardWorkoutPlan);

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTodayWorkoutTitle = findViewById(R.id.tvTodayWorkoutTitle);
        ivTodayWorkout = findViewById(R.id.ivTodayWorkout);
        cardTodayWorkout = findViewById(R.id.cardTodayWorkout);

        cardFreeWorkout = findViewById(R.id.cardFreeWorkout);
    }

    private void setupClickListeners() {
        cardWorkoutPlan.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
            startActivity(intent);
        });

        cardFreeWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, EquipmentSelectionActivity.class);
            startActivity(intent);
        });
    }

    private void setupTodayWorkout() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        String currentDate = sdf.format(new Date());
        tvCurrentTime.setText(currentDate);

        int lastCompletedDay = 1; 
        int nextDayOrder = lastCompletedDay + 1; 

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

        cardTodayWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ExerciseListActivity.class);
            intent.putExtra("PLAN_ID", 1);
            intent.putExtra("DAY_ORDER", nextDayOrder);
            startActivity(intent);
        });
    }

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