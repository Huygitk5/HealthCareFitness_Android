package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.JourneyDayAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.FitnessGoal;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserWorkoutSession;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutJourneyActivity extends AppCompatActivity {

    // === VIEWS ===
    private TextView tvStreak, tvHeroTitle, tvProgressFraction, tvTodaySub, tvTodayName, tvTodayPercent;
    private ProgressBar pbOverallProgress, pbTodayCircular;
    private Button btnTodayAction;
    private RecyclerView rvWorkoutDays;
    private TextView tvSeeAllDays;

    // === DATA ===
    private String username, userId;
    private String currentPlanId;
    private String todayDayId;       // ID của WorkoutDay hôm nay
    private int todayDayOrder = 1;
    private int totalPlanDays = 0;
    private int completedDays = 0;
    private List<WorkoutDay> allDays = new ArrayList<>();
    private JourneyDayAdapter dayAdapter;

    // Set của các dayId đã hoàn thành (từ user_workout_sessions)
    private java.util.Set<String> completedDayIds = new java.util.HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_journey);

        androidx.core.view.WindowInsetsControllerCompat controller =
                new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");

        initViews();
        setupBottomNavigation();
        loadUserJourney();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload để cập nhật % khi quay lại sau khi tập
        loadUserJourney();
    }

    private void initViews() {
        tvStreak          = findViewById(R.id.tvStreak);
        tvHeroTitle       = findViewById(R.id.tvHeroTitle);
        tvProgressFraction = findViewById(R.id.tvProgressFraction);
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        pbTodayCircular   = findViewById(R.id.pbTodayCircular);
        tvTodayPercent    = findViewById(R.id.tvTodayPercent);
        tvTodaySub        = findViewById(R.id.tvTodaySub);
        tvTodayName       = findViewById(R.id.tvTodayName);
        btnTodayAction    = findViewById(R.id.btnTodayAction);
        rvWorkoutDays     = findViewById(R.id.rvWorkoutDays);
        tvSeeAllDays      = findViewById(R.id.tvSeeAllDays);

        rvWorkoutDays.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Nút "Xem tất cả" -> mở WorkoutDetailActivity với planId
        tvSeeAllDays.setOnClickListener(v -> {
            if (currentPlanId != null && !currentPlanId.isEmpty()) {
                Intent intent = new Intent(this, WorkoutDetailActivity.class);
                intent.putExtra("PLAN_ID", currentPlanId);
                startActivity(intent);
            }
        });

        // Create Custom Routine
        findViewById(R.id.layoutCustomWorkout).setOnClickListener(v ->
                startActivity(new Intent(this, EquipmentSelectionActivity.class)));
    }

    // =============================================================
    // BƯỚC 1: Load thông tin user (goal, target_date, plan_id)
    // =============================================================
    private void loadUserJourney() {
        if (username == null || username.isEmpty()) return;

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserByUsername("eq." + username, "*").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    currentPlanId = user.getCurrentWorkoutPlanId();

                    // Tính tổng số ngày journey từ ngày tạo tài khoản -> target_date
                    int[] journeyDays = calcJourneyDays(user.getCreatedAt(), user.getTargetDate());
                    totalPlanDays = journeyDays[0];  // tổng ngày
                    int daysSinceStart = journeyDays[1]; // số ngày đã qua từ khi bắt đầu

                    // Hiển thị tên mục tiêu (fitness goal)
                    String goalName = resolveGoalName(user.getFitnessGoalId());
                    tvHeroTitle.setText("Your Journey:\n" + goalName);

                    // Load lịch sử tập + plan days
                    loadCompletedSessionsThenPlan(daysSinceStart);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    /**
     * Tính [totalDays, daysSinceStart] từ created_at và target_date.
     * Nếu không có target_date (giữ dáng) -> dùng 30 ngày mặc định.
     */
    private int[] calcJourneyDays(String createdAt, String targetDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date start = sdf.parse(createdAt != null ? createdAt.substring(0, 10) : "");
            Date today = new Date();

            if (targetDate != null && !targetDate.isEmpty()) {
                Date end = sdf.parse(targetDate);
                long total = TimeUnit.MILLISECONDS.toDays(end.getTime() - start.getTime());
                long elapsed = TimeUnit.MILLISECONDS.toDays(today.getTime() - start.getTime());
                int totalDays = (int) Math.max(1, total);
                int elapsed2 = (int) Math.max(0, Math.min(elapsed, totalDays));
                return new int[]{totalDays, elapsed2};
            }
        } catch (Exception ignored) {}
        // fallback: giữ dáng → 30 ngày
        return new int[]{30, 0};
    }

    private String resolveGoalName(Integer goalId) {
        if (goalId == null) return "Mục tiêu của bạn";
        switch (goalId) {
            case 1: return "Giảm mỡ";
            case 2: return "Tăng cơ";
            case 3: return "Giữ dáng";
            default: return "Mục tiêu của bạn";
        }
    }

    // =============================================================
    // BƯỚC 2: Load lịch sử tập để biết ngày nào đã hoàn thành
    // =============================================================
    private void loadCompletedSessionsThenPlan(int daysSinceStart) {
        if (userId == null || userId.isEmpty()) {
            loadPlanDays(daysSinceStart, 0);
            return;
        }

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserWorkoutHistory("eq." + userId, "*").enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                completedDayIds.clear();
                completedDays = 0;

                if (response.isSuccessful() && response.body() != null) {
                    for (UserWorkoutSession s : response.body()) {
                        if (s.getDayId() != null) {
                            completedDayIds.add(s.getDayId());
                        }
                    }
                    completedDays = completedDayIds.size();

                    // Tính streak (số ngày tập liên tiếp gần nhất)
                    int streak = calcStreak(response.body());
                    tvStreak.setText("🔥 " + streak + " Day Streak");
                }
                loadPlanDays(daysSinceStart, completedDays);
            }
            @Override
            public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) {
                loadPlanDays(daysSinceStart, 0);
            }
        });
    }

    /**
     * Tính streak: đếm số ngày tập liên tiếp tính từ hôm nay về trước.
     */
    private int calcStreak(List<UserWorkoutSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.util.Set<String> sessionDates = new java.util.HashSet<>();
        for (UserWorkoutSession s : sessions) {
            if (s.getStartedAt() != null && s.getStartedAt().length() >= 10) {
                sessionDates.add(s.getStartedAt().substring(0, 10));
            }
        }

        int streak = 0;
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            String dateStr = sdf.format(cal.getTime());
            if (sessionDates.contains(dateStr)) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    // =============================================================
    // BƯỚC 3: Load danh sách ngày từ plan, xác định "hôm nay" là ngày mấy
    // =============================================================
    private void loadPlanDays(int daysSinceStart, int completedCount) {
        if (currentPlanId == null || currentPlanId.isEmpty()) {
            updateHeroSection(completedCount, 30);
            return;
        }

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*,workout_days(*, workout_day_exercises(*, exercise:exercises(*)))";

        api.getWorkoutPlanById("eq." + currentPlanId, selectQuery).enqueue(new Callback<List<WorkoutPlan>>() {
            @Override
            public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutPlan plan = response.body().get(0);
                    if (plan.getDays() != null) {
                        allDays = plan.getDays();
                        // Sort theo day_order
                        java.util.Collections.sort(allDays, (a, b) -> {
                            if (a.getDayOrder() == null) return 1;
                            if (b.getDayOrder() == null) return -1;
                            return Integer.compare(a.getDayOrder(), b.getDayOrder());
                        });

                        // Tổng ngày journey là totalPlanDays đã tính từ user
                        // Ngày hôm nay trong journey = daysSinceStart + 1 (bắt đầu từ 1)
                        // Map sang ngày trong plan (vòng lặp nếu plan ít ngày hơn journey)
                        int planSize = allDays.size();
                        int currentDayIndex = planSize > 0
                                ? (daysSinceStart % planSize)
                                : 0;

                        WorkoutDay todayDay = allDays.get(currentDayIndex);
                        todayDayId = todayDay.getId();
                        todayDayOrder = todayDay.getDayOrder() != null ? todayDay.getDayOrder() : (currentDayIndex + 1);

                        // === Today's Mission ===
                        tvTodaySub.setText("NGÀY " + todayDayOrder);
                        String dayName = todayDay.getName() != null ? todayDay.getName() : "Ngày tập";
                        tvTodayName.setText(dayName);

                        // Tính % hoàn thành hôm nay từ lịch sử
                        boolean todayDone = completedDayIds.contains(todayDayId);
                        int todayPercent = todayDone ? 100 : 0;
                        tvTodayPercent.setText(todayPercent + "%");
                        animateProgress(pbTodayCircular, todayPercent, 1000);

                        // Trạng thái nút
                        updateActionButton(todayDone, todayDay);

                        // === Hero section ===
                        updateHeroSection(completedCount, totalPlanDays > 0 ? totalPlanDays : planSize);

                        // === Timeline RecyclerView ===
                        setupTimeline(currentDayIndex);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                updateHeroSection(completedCount, totalPlanDays > 0 ? totalPlanDays : 30);
            }
        });
    }

    private void updateHeroSection(int done, int total) {
        int progress = total > 0 ? (int) ((done * 100.0) / total) : 0;
        tvProgressFraction.setText(done + " / " + total + " ngày hoàn thành");
        animateProgress(pbOverallProgress, progress, 1200);
    }

    private void updateActionButton(boolean todayDone, WorkoutDay todayDay) {
        if (todayDone) {
            btnTodayAction.setText("✅ HOÀN THÀNH");
            btnTodayAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            // Vẫn cho bấm vào để xem lại bài tập
            btnTodayAction.setOnClickListener(v -> openDayExercises(todayDay));
        } else {
            btnTodayAction.setText("BẮT ĐẦU");
            btnTodayAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#4DAA9A")));
            btnTodayAction.setOnClickListener(v -> openDayExercises(todayDay));
        }
    }

    private void openDayExercises(WorkoutDay day) {
        ArrayList<Exercise> exercisesToPass = new ArrayList<>();
        if (day.getExercises() != null) {
            for (WorkoutDayExercise wde : day.getExercises()) {
                if (wde.getExercise() != null) {
                    Exercise ex = wde.getExercise();
                    if (wde.getReps() != null) ex.setBaseRecommendedReps(String.valueOf(wde.getReps()));
                    if (wde.getSets() != null) ex.setBaseRecommendedSets(wde.getSets());
                    exercisesToPass.add(ex);
                }
            }
        }

        Intent intent = new Intent(this, ExerciseListActivity.class);
        intent.putExtra("EXTRA_EXERCISE_LIST", exercisesToPass);
        intent.putExtra("EXTRA_DAY_TITLE", "Ngày " + day.getDayOrder() + ": " + day.getName());
        intent.putExtra("EXTRA_DAY_ID_FOR_LOG", day.getId()); // để log session sau khi tập
        startActivity(intent);
    }

    // =============================================================
    // TIMELINE: Hiển thị ngang, đánh dấu ngày đã qua bằng tick
    // =============================================================
    private void setupTimeline(int currentDayIndex) {
        dayAdapter = new JourneyDayAdapter(
                allDays,
                currentDayIndex,
                completedDayIds,
                day -> openDayExercises(day) // Bấm vào ngày nào cũng mở được bài tập
        );
        rvWorkoutDays.setAdapter(dayAdapter);
        // Scroll tới ngày hôm nay
        rvWorkoutDays.scrollToPosition(Math.max(0, currentDayIndex - 1));
    }

    // =============================================================
    // ANIMATION
    // =============================================================
    private void animateProgress(ProgressBar progressBar, int target, long duration) {
        ObjectAnimator anim = ObjectAnimator.ofInt(progressBar, "progress", 0, target);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    // =============================================================
    // BOTTOM NAVIGATION
    // =============================================================
    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent intent = new Intent(this, NutritionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            // Đã ở trang này rồi, không làm gì
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}