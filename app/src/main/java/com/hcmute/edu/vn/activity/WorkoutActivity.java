package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.UserWorkoutSession;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutActivity extends AppCompatActivity {
    String username;
    String userId;

    private String currentPlanId = "";
    private Map<String, String> planMap = new HashMap<>(); // Lưu trữ ID động
    private String userTarget = "BEGINNER"; // Mặc định

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ScrollView mainScrollView = findViewById(R.id.mainScrollView);
            if (mainScrollView != null) mainScrollView.setPadding(0, systemBars.top, 0, systemBars.bottom);
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setPadding(0, 0, 0, systemBars.bottom-10);
            return insets;
        });

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");
        // Lấy mục tiêu thực tế của người dùng, nếu không có mặc định là BEGINNER
        userTarget = pref.getString("USER_FITNESS_GOAL", "BEGINNER").toUpperCase();

        initViews();
        setupLevelListeners();
        setupClickListeners();
        setupBottomNavigation();

        // Cập nhật ngày tháng
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        tvCurrentTime.setText(sdf.format(new Date()));

        // Tải danh sách Gói tập động từ Supabase
        fetchAllWorkoutPlansAndSelectTarget();

        // Tải tiến độ bài tập hôm nay
        setupTodayWorkout();
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
            if (currentPlanId == null || currentPlanId.isEmpty()) {
                Toast.makeText(WorkoutActivity.this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
            intent.putExtra("PLAN_ID", currentPlanId);
            startActivity(intent);
        });

        cardFreeWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, EquipmentSelectionActivity.class);
            startActivity(intent);
        });
    }

    // --- TẢI DANH SÁCH GÓI TẬP ĐỘNG KHÔNG DÙNG HARDCODE ---
    private void fetchAllWorkoutPlansAndSelectTarget() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllWorkoutPlans("*").enqueue(new Callback<List<WorkoutPlan>>() {
            @Override
            public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (WorkoutPlan plan : response.body()) {
                        // Tạm dùng tên plan (BEGINNER, INTERMEDIATE...) làm key
                        planMap.put(plan.getName().toUpperCase(), plan.getId());
                    }
                    selectLevel(userTarget); // Tự động click vào tab mục tiêu của user
                }
            }
            @Override
            public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                Toast.makeText(WorkoutActivity.this, "Lỗi tải gói tập", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLevelListeners() {
        tvBeginner.setOnClickListener(v -> selectLevel("BEGINNER"));
        tvIntermediate.setOnClickListener(v -> selectLevel("INTERMEDIATE"));
        tvAdvanced.setOnClickListener(v -> selectLevel("ADVANCED"));
    }

    private void selectLevel(String level) {
        resetLevelButtons();

        // Lấy ID động từ Map. Nếu rỗng (chưa fetch xong) thì để tạm
        currentPlanId = planMap.getOrDefault(level, "");

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

    // --- LOGIC LẤY BÀI TẬP HÔM NAY ---
    private void setupTodayWorkout() {
        tvTodayWorkoutTitle.setText("Đang phân tích dữ liệu...");
        if (userId == null || userId.isEmpty()) {
            loadDefaultWorkout();
            return;
        }
        fetchUserWorkoutHistory();
    }

    private void fetchUserWorkoutHistory() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getUserWorkoutHistory("eq." + userId, "*, workout_days(*)").enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<UserWorkoutSession> sessions = response.body();
                    UserWorkoutSession latestSession = sessions.get(sessions.size() - 1);
                    // Chuyển sang bước tiếp theo
                    fetchCompletedDayOrder(latestSession.getPlanId(), latestSession.getDayId());
                } else {
                    loadDefaultWorkout();
                }
            }
            @Override
            public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) {
                tvTodayWorkoutTitle.setText("Lỗi kết nối");
            }
        });
    }

    private void fetchCompletedDayOrder(String planId, String lastDayId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getExercisesForSpecificDay("eq." + lastDayId, "*").enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutDay lastDay = response.body().get(0);
                    int nextDayOrder = lastDay.getDayOrder() + 1;
                    fetchNextWorkoutDayByOrder(planId, nextDayOrder);
                }
            }
            @Override
            public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
        });
    }

    private void fetchNextWorkoutDayByOrder(String planId, int nextDayOrder) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getNextWorkoutDay("eq." + planId, "eq." + nextDayOrder, "*").enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutDay nextDay = response.body().get(0);
                    tvTodayWorkoutTitle.setText("Ngày " + nextDay.getDayOrder() + ": " + nextDay.getName());
                    ivTodayWorkout.setImageResource(R.drawable.workout_2);
                    setupClickForTodayWorkout(nextDay.getId());
                } else {
                    tvTodayWorkoutTitle.setText("Chúc mừng! Bạn đã hoàn thành Gói tập.");
                    cardTodayWorkout.setOnClickListener(null);
                }
            }
            @Override
            public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
        });
    }

    private void loadDefaultWorkout() {
        tvTodayWorkoutTitle.setText("Bắt đầu lộ trình mới của bạn!");
        ivTodayWorkout.setImageResource(R.drawable.workout_1);
        // Khi user chưa có lịch sử, click vào Today Workout sẽ điều hướng thẳng sang Workout Detail
        cardTodayWorkout.setOnClickListener(v -> {
            if (!currentPlanId.isEmpty()) {
                Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
                intent.putExtra("PLAN_ID", currentPlanId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Đang tải dữ liệu...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickForTodayWorkout(String nextDayId) {
        cardTodayWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ExerciseListActivity.class);
            intent.putExtra("EXTRA_DAY_ID", nextDayId);
            startActivity(intent);
        });
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