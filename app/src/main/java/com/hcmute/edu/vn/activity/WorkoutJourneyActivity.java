package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.JourneyDayAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Exercise;
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

    private TextView tvStreak, tvHeroTitle, tvProgressFraction, tvTodaySub, tvTodayName, tvTodayPercent;
    private ProgressBar pbOverallProgress, pbTodayCircular;
    private Button btnTodayAction;
    private CardView cvTodayWorkout;
    private RecyclerView rvWorkoutDays;
    private TextView tvSeeAllDays;

    private String username, userId, currentPlanId;
    private int totalPlanDays = 0;
    private List<WorkoutDay> allDays = new ArrayList<>();
    private java.util.Set<String> completedDayIds = new java.util.HashSet<>();
    private WorkoutDay todayWorkoutDay = null; // Lưu trữ buổi tập hôm nay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_journey);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
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
        loadUserJourney();
    }

    private void initViews() {
        tvStreak = findViewById(R.id.tvStreak);
        tvHeroTitle = findViewById(R.id.tvHeroTitle);
        tvProgressFraction = findViewById(R.id.tvProgressFraction);
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        pbTodayCircular = findViewById(R.id.pbTodayCircular);
        tvTodayPercent = findViewById(R.id.tvTodayPercent);
        tvTodaySub = findViewById(R.id.tvTodaySub);
        tvTodayName = findViewById(R.id.tvTodayName);
        btnTodayAction = findViewById(R.id.btnTodayAction);
        cvTodayWorkout = findViewById(R.id.cvTodayWorkout);
        rvWorkoutDays = findViewById(R.id.rvWorkoutDays);
        tvSeeAllDays = findViewById(R.id.tvSeeAllDays);

        rvWorkoutDays.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        View.OnClickListener workoutClickListener = v -> {
            if (todayWorkoutDay != null) {
                openDayExercises(todayWorkoutDay);
            } else {
                if (currentPlanId == null || currentPlanId.isEmpty()) {
                    Toast.makeText(this, "Hệ thống đang thiết lập lịch tập. Vui lòng đợi trong giây lát!", Toast.LENGTH_LONG).show();
                    loadUserJourney(); // Thử tải lại
                } else {
                    Toast.makeText(this, "Đang tải dữ liệu bài tập...", Toast.LENGTH_SHORT).show();
                }
            }
        };
        btnTodayAction.setOnClickListener(workoutClickListener);
        cvTodayWorkout.setOnClickListener(workoutClickListener);

        tvSeeAllDays.setOnClickListener(v -> {
            if (currentPlanId != null && !currentPlanId.isEmpty()) {
                Intent intent = new Intent(this, WorkoutDetailActivity.class);
                intent.putExtra("PLAN_ID", currentPlanId);
                startActivity(intent);
            }
        });

        findViewById(R.id.layoutCustomWorkout).setOnClickListener(v -> 
                startActivity(new Intent(this, EquipmentSelectionActivity.class)));
    }

    private void loadUserJourney() {
        if (username == null || username.isEmpty()) return;
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserByUsername("eq." + username, "*").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    
                    if (userId == null || userId.isEmpty()) {
                        userId = user.getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putString("KEY_USER_ID", userId).apply();
                    }
                    
                    currentPlanId = user.getCurrentWorkoutPlanId();
                    if (currentPlanId == null || currentPlanId.isEmpty()) {
                        // Gán plan mặc định nếu user chưa có (Beginner Plan)
                        currentPlanId = "a1111111-1111-1111-1111-111111111111";
                    }

                    int[] journeyDays = calcJourneyDays(user.getCreatedAt(), user.getTargetDate());
                    loadCompletedSessionsThenPlan();
                    tvHeroTitle.setText("Your Journey:\n" + resolveGoalName(user.getFitnessGoalId()));
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void loadCompletedSessionsThenPlan() {
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserWorkoutHistory("eq." + userId, "*").enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                completedDayIds.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (UserWorkoutSession s : response.body()) {
                        // CHỈ lấy các buổi tập của gói tập hiện tại!
                        if (s.getPlanId() != null && s.getPlanId().equals(currentPlanId) && s.getDayId() != null) {
                            completedDayIds.add(s.getDayId());
                        }
                    }
                    tvStreak.setText("🔥 " + calcStreak(response.body()) + " Day Streak");
                }
                loadPlanDays();
            }
            @Override public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) { loadPlanDays(); }
        });
    }

    private void loadPlanDays() {
        if (currentPlanId == null || currentPlanId.isEmpty()) return;
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getWorkoutPlanById("eq." + currentPlanId, "*,workout_days(*, workout_day_exercises(*, exercise:exercises(*)))").enqueue(new Callback<List<WorkoutPlan>>() {
            @Override
            public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutPlan plan = response.body().get(0);
                    if (plan.getDays() != null && !plan.getDays().isEmpty()) {
                        allDays = plan.getDays();
                        java.util.Collections.sort(allDays, (a, b) -> Integer.compare(a.getDayOrder() != null ? a.getDayOrder() : 0, b.getDayOrder() != null ? b.getDayOrder() : 0));
                        
                        totalPlanDays = allDays.size();
                        
                        int targetIndex = 0;
                        for (int i = 0; i < allDays.size(); i++) {
                            if (!completedDayIds.contains(allDays.get(i).getId())) {
                                targetIndex = i;
                                break;
                            }
                        }
                        if (targetIndex == 0 && !allDays.isEmpty() && completedDayIds.contains(allDays.get(0).getId())) {
                            targetIndex = allDays.size() - 1;
                        }

                        int currentDayIndex = targetIndex;
                        todayWorkoutDay = allDays.get(currentDayIndex);
                        boolean todayDone = completedDayIds.contains(todayWorkoutDay.getId());

                        tvTodaySub.setText("NGÀY " + (todayWorkoutDay.getDayOrder() != null ? todayWorkoutDay.getDayOrder() : (currentDayIndex + 1)));
                        tvTodayName.setText(todayWorkoutDay.getName() != null ? todayWorkoutDay.getName() : "Ngày tập");
                        tvTodayPercent.setText(todayDone ? "100%" : "0%");
                        animateProgress(pbTodayCircular, todayDone ? 100 : 0, 1000);

                        if (todayDone) {
                            btnTodayAction.setText("✅ HOÀN THÀNH");
                            btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                        } else {
                            btnTodayAction.setText("BẮT ĐẦU");
                            btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4DAA9A")));
                        }

                        tvProgressFraction.setText(completedDayIds.size() + " / " + totalPlanDays + " ngày hoàn thành");
                        animateProgress(pbOverallProgress, totalPlanDays > 0 ? (int) ((completedDayIds.size() * 100.0) / totalPlanDays) : 0, 1200);
                        setupTimeline(currentDayIndex);
                    }
                }
            }
            @Override public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {}
        });
    }

    private void openDayExercises(WorkoutDay day) {
        if (day == null) return;
        Intent intent = new Intent(this, ExerciseListActivity.class);
        String title = "Ngày " + (day.getDayOrder() != null ? day.getDayOrder() : "?");
        
        if (day.getId() != null) {
            intent.putExtra("EXTRA_DAY_ID", day.getId());
            intent.putExtra("EXTRA_DAY_TITLE", title);
        } else {
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
            intent.putExtra("EXTRA_EXERCISE_LIST", exercisesToPass);
            intent.putExtra("EXTRA_DAY_TITLE", "Ngày " + day.getDayOrder() + ": " + day.getName());
        }
        startActivity(intent);
    }

    private void setupTimeline(int currentDayIndex) {
        rvWorkoutDays.setAdapter(new JourneyDayAdapter(allDays, currentDayIndex, completedDayIds, this::openDayExercises));
        rvWorkoutDays.scrollToPosition(Math.max(0, currentDayIndex - 1));
    }

    private void animateProgress(ProgressBar pb, int target, long duration) {
        ObjectAnimator anim = ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), target);
        anim.setDuration(duration);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    private int[] calcJourneyDays(String createdAt, String targetDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        try {
            Date start = sdf.parse(createdAt != null ? createdAt.substring(0, 10) : "");
            Date today = new Date();
            if (targetDate != null && !targetDate.isEmpty()) {
                Date end = sdf.parse(targetDate);
                long total = TimeUnit.MILLISECONDS.toDays(end.getTime() - start.getTime());
                long elapsed = TimeUnit.MILLISECONDS.toDays(today.getTime() - start.getTime());
                return new int[]{(int) Math.max(1, total), (int) Math.max(0, Math.min(elapsed, total))};
            }
        } catch (Exception ignored) {}
        return new int[]{30, 0};
    }

    private int calcStreak(List<UserWorkoutSession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        java.util.Set<String> dates = new java.util.HashSet<>();
        for (UserWorkoutSession s : sessions) if (s.getStartedAt() != null && s.getStartedAt().length() >= 10) dates.add(s.getStartedAt().substring(0, 10));
        int streak = 0; Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 365; i++) {
            if (dates.contains(sdf.format(cal.getTime()))) { streak++; cal.add(Calendar.DAY_OF_YEAR, -1); } else break;
        }
        return streak;
    }

    private String resolveGoalName(Integer goalId) {
        if (goalId == null) return "Mục tiêu của bạn";
        if (goalId == 1) return "Giảm mỡ";
        if (goalId == 2) return "Tăng cơ";
        return "Giữ dáng";
    }

    private void setupBottomNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> { startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_nutrition).setOnClickListener(v -> { startActivity(new Intent(this, NutritionActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_profile).setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
    }
}