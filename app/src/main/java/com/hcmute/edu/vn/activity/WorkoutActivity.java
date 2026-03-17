package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutActivity extends AppCompatActivity {
    String username;
    String userId; // Thêm biến lưu ID của user thật

    private String currentPlanId = "";

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
            if (mainScrollView != null) {
                mainScrollView.setPadding(0, systemBars.top, 0, systemBars.bottom);
            }

            View bottomNav = findViewById(R.id.bottomNav);
            if (bottomNav != null) {
                bottomNav.setPadding(0, 0, 0, systemBars.bottom-10);
            }

            return insets;
        });

        // Lấy thông tin user đang đăng nhập từ SharedPreferences
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", ""); // Lấy ID động

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
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");
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
            if (currentPlanId == null || currentPlanId.isEmpty() || currentPlanId.contains("TRÊN_SUPABASE")) {
                Toast.makeText(WorkoutActivity.this, "Bạn cần thay thế ID Supabase trong code!", Toast.LENGTH_SHORT).show();
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

    // --- LOGIC LẤY BÀI TẬP HÔM NAY ---
    private void setupTodayWorkout() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        tvCurrentTime.setText(sdf.format(new Date()));

        tvTodayWorkoutTitle.setText("Đang phân tích dữ liệu...");

        // Xử lý an toàn nếu user chưa đăng nhập hoặc không tìm thấy ID
        if (userId == null || userId.isEmpty()) {
            tvTodayWorkoutTitle.setText("Ngày 1: Khởi động & Làm quen");
            ivTodayWorkout.setImageResource(R.drawable.workout_1);
            // Bạn có thể sửa chuỗi dưới đây thành ID Ngày 1 của gói Beginner trên Supabase
            setupClickForTodayWorkout("14dd7725-ba8d-4151-8acd-efc8d0a54d8a");
            return;
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // Gọi API lấy lịch sử tập bằng userId động
        String selectQuery = "*, workout_days(*)";
        apiService.getUserWorkoutHistory("eq." + userId, selectQuery).enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<UserWorkoutSession> sessions = response.body();
                    UserWorkoutSession latestSession = sessions.get(sessions.size() - 1);
                    String currentPlanId = latestSession.getPlanId();

                    fetchNextDayDetails(apiService, currentPlanId, latestSession.getDayId());

                } else {
                    // Mới tải app, chưa tập lần nào
                    tvTodayWorkoutTitle.setText("Ngày 1: Khởi động & Làm quen");
                    ivTodayWorkout.setImageResource(R.drawable.workout_1);
                    // Sửa chuỗi dưới đây thành ID Ngày 1 của gói Beginner trên Supabase
                    setupClickForTodayWorkout("14dd7725-ba8d-4151-8acd-efc8d0a54d8a");
                }
            }

            @Override
            public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) {
                tvTodayWorkoutTitle.setText("Lỗi kết nối");
            }
        });
    }

    private void fetchNextDayDetails(SupabaseApiService apiService, String planId, String lastDayId) {
        apiService.getExercisesForSpecificDay("eq." + lastDayId, "*").enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutDay lastDay = response.body().get(0);
                    int nextDayOrder = lastDay.getDayOrder() + 1;

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
            }
            @Override
            public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {}
        });
    }

    private void setupClickForTodayWorkout(String nextDayId) {
        cardTodayWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(WorkoutActivity.this, ExerciseListActivity.class);
            intent.putExtra("EXTRA_DAY_ID", nextDayId);
            startActivity(intent);
        });
    }

    // --- LOGIC CHỌN LEVEL ---
    private void setupLevelListeners() {
        tvBeginner.setOnClickListener(v -> selectLevel("BEGINNER"));
        tvIntermediate.setOnClickListener(v -> selectLevel("INTERMEDIATE"));
        tvAdvanced.setOnClickListener(v -> selectLevel("ADVANCED"));
    }

    private void selectLevel(String level) {
        resetLevelButtons();
        switch (level) {
            case "BEGINNER":
                currentPlanId = "a1111111-1111-1111-1111-111111111111";

                tvBeginner.setBackgroundResource(R.drawable.bg_workout_chip_active);
                tvBeginner.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Khởi động & Làm quen");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv1);
                break;
            case "INTERMEDIATE":
                currentPlanId = "a2222222-2222-2222-2222-222222222222";

                tvIntermediate.setBackgroundResource(R.drawable.bg_workout_chip_active);
                tvIntermediate.setTextColor(Color.WHITE);
                tvWorkoutPlanTitle.setText("Tăng cơ & Giảm mỡ");
                ivWorkoutPlan.setImageResource(R.drawable.img_workout_lv2);
                break;
            case "ADVANCED":
                currentPlanId = "05fea3e9-377e-4108-bee3-15a78150dc43";

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