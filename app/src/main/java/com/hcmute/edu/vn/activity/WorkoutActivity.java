package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutActivity extends AppCompatActivity {
    private String username;
    private String userId;

    private String currentPlanId = "";
    private int userGoalId = -1;

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

        setupWindowInsets();

        // Chỉ lấy User ID và Username 1 lần ở onCreate
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");

        initViews();
        setupClickListeners();
        setupBottomNavigation();

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        tvCurrentTime.setText(sdf.format(new Date()));

        // Không gọi API ở đây nữa để tránh lỗi không update khi chuyển tab
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        int newGoalId = pref.getInt("USER_FITNESS_GOAL_ID", 1);
        boolean hasChanged = pref.getBoolean("TARGET_CHANGED", false);

        // Nếu ID mục tiêu khác HOẶC có cờ báo đã thay đổi
        if (newGoalId != userGoalId || hasChanged || currentPlanId.isEmpty()) {
            userGoalId = newGoalId;
            // Sau khi đọc xong thì tắt cờ báo hiệu đi
            pref.edit().putBoolean("TARGET_CHANGED", false).apply();
            fetchWorkoutPlanByGoalId();
        } else {
            setupTodayWorkout(currentPlanId);
        }
    }

    private void setupWindowInsets() {
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ScrollView mainScrollView = findViewById(R.id.mainScrollView);
            if (mainScrollView != null) mainScrollView.setPadding(0, systemBars.top, 0, systemBars.bottom);
            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) bottomNav.setPadding(0, 0, 0, systemBars.bottom - 10);
            return insets;
        });
    }

    private void initViews() {
        tvWorkoutPlanTitle = findViewById(R.id.tvWorkoutPlanTitle);
        ivWorkoutPlan = findViewById(R.id.ivWorkoutPlan);
        cardWorkoutPlan = findViewById(R.id.cardWorkoutPlan);

        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTodayWorkoutTitle = findViewById(R.id.tvTodayWorkoutTitle);
        ivTodayWorkout = findViewById(R.id.ivTodayWorkout);
        cardTodayWorkout = findViewById(R.id.cardTodayWorkout);
        cardFreeWorkout = findViewById(R.id.cardFreeWorkout);

        // Ẩn 3 cái Tab cũ đi
        TextView tvBeginner = findViewById(R.id.tvBeginner);
        TextView tvIntermediate = findViewById(R.id.tvIntermediate);
        TextView tvAdvanced = findViewById(R.id.tvAdvanced);

        if (tvBeginner != null) tvBeginner.setVisibility(View.GONE);
        if (tvIntermediate != null) tvIntermediate.setVisibility(View.GONE);
        if (tvAdvanced != null) tvAdvanced.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        cardWorkoutPlan.setOnClickListener(v -> {
            if (currentPlanId == null || currentPlanId.isEmpty()) {
                Toast.makeText(WorkoutActivity.this, "Đang tải lộ trình, vui lòng đợi...", Toast.LENGTH_SHORT).show();
                return;
            }
            // Truyền ĐÚNG ID gói tập sang màn hình Detail
            Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
            intent.putExtra("PLAN_ID", currentPlanId);
            startActivity(intent);
        });

        cardFreeWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, EquipmentSelectionActivity.class);
            startActivity(intent);
        });
    }

    private void fetchWorkoutPlanByGoalId() {
        tvWorkoutPlanTitle.setText("Đang tải lộ trình...");
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getWorkoutPlanByGoalId("eq." + userGoalId, "*").enqueue(new Callback<List<WorkoutPlan>>() {
            @Override
            public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutPlan plan = response.body().get(0);
                    currentPlanId = plan.getId();
                    tvWorkoutPlanTitle.setText(plan.getName() != null ? plan.getName() : "Lộ trình luyện tập");

                    String planName = (plan.getName() != null) ? plan.getName().toLowerCase() : "";
                    if (planName.contains("giảm")) {
                        ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv1);
                    } else if (planName.contains("tăng")) {
                        ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv2);
                    } else {
                        ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv3);
                    }

                    // Luôn gọi Today Workout SAU KHI đã có currentPlanId mới
                    setupTodayWorkout(currentPlanId);
                } else {
                    tvWorkoutPlanTitle.setText("Chưa có lộ trình cho mục tiêu này");
                }
            }
            @Override public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                tvWorkoutPlanTitle.setText("Lỗi kết nối");
            }
        });
    }

    private void setupTodayWorkout(String planId) {
        if (userId == null || planId == null || planId.isEmpty()) return;

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // CHỈ LẤY LỊCH SỬ CỦA PLAN HIỆN TẠI
        apiService.getUserWorkoutHistoryByPlan("eq." + userId, "eq." + planId, "*, workout_days(*)").enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    // Đã có lịch sử tập Plan này -> Lấy ngày tiếp theo
                    List<UserWorkoutSession> sessions = response.body();
                    UserWorkoutSession latest = sessions.get(sessions.size() - 1);
                    fetchCompletedDayOrder(planId, latest.getDayId());
                } else {
                    fetchNextWorkoutDayByOrder(planId, 1);
                }
            }
            @Override public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) {
                tvTodayWorkoutTitle.setText("Lỗi kết nối");
            }
        });
    }

    private void loadDefaultWorkoutDay1(String planId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getNextWorkoutDay("eq." + planId, "eq.1", "*").enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutDay day1 = response.body().get(0);
                    tvTodayWorkoutTitle.setText("Ngày 1: " + day1.getName());
                    ivTodayWorkout.setImageResource(R.drawable.workout_1);
                    setupClickForTodayWorkout(day1.getId(), 1);
                } else {
                    tvTodayWorkoutTitle.setText("Bắt đầu lộ trình mới!");
                    cardTodayWorkout.setOnClickListener(v -> {
                        Intent intent = new Intent(WorkoutActivity.this, WorkoutDetailActivity.class);
                        intent.putExtra("PLAN_ID", currentPlanId);
                        startActivity(intent);
                    });
                }
            }
            @Override public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
        });
    }

    private void fetchCompletedDayOrder(String planId, String lastDayId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getExercisesForSpecificDay("eq." + lastDayId, "*").enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    int nextDayOrder = response.body().get(0).getDayOrder() + 1;
                    fetchNextWorkoutDayByOrder(planId, nextDayOrder);
                }
            }
            @Override public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
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
                    setupClickForTodayWorkout(nextDay.getId(), nextDay.getDayOrder());
                } else {
                    tvTodayWorkoutTitle.setText("Bạn đã hoàn thành lộ trình. Hãy đổi Mục tiêu!");
                    cardTodayWorkout.setOnClickListener(null);
                }
            }
            @Override public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
        });
    }

    private void setupClickForTodayWorkout(String nextDayId, int order) {
        cardTodayWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ExerciseListActivity.class);
            intent.putExtra("EXTRA_DAY_ID", nextDayId);
            intent.putExtra("EXTRA_DAY_TITLE", "Ngày " + order);
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