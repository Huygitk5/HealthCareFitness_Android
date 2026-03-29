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
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.JourneyDayAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.ConditionRestrictedMuscle;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyWorkout;
import com.hcmute.edu.vn.model.UserMedicalCondition;
import com.hcmute.edu.vn.model.UserWorkoutSession;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;
import com.hcmute.edu.vn.model.WorkoutPlan;
import com.hcmute.edu.vn.util.FitnessCalculator;
import com.hcmute.edu.vn.util.WorkoutGenerator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
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
    private WorkoutDay todayWorkoutDay = null; // Lưu trữ buổi tập hiển thị trên Mission Card
    private List<Integer> currentUserConditionIds = new ArrayList<>();
    
    private String lastCompletedDayIdToday = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_journey);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
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
    SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
    boolean workoutUpdateNeeded = pref.getBoolean("WORKOUT_UPDATE_NEEDED", false);

    if (workoutUpdateNeeded) {
        // Tắt cờ đi để không bị lặp lại
        pref.edit().putBoolean("WORKOUT_UPDATE_NEEDED", false).apply();

        Toast.makeText(this, "Đang thiết lập lại lộ trình tập luyện...", Toast.LENGTH_SHORT).show();
        // Lấy User và kích hoạt bộ não AI sinh lịch tập mới
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserByUsername("eq." + username, "*,user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    generateAndSavePersonalizedWorkout(response.body().get(0));
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    } else {
        currentUserConditionIds.clear();
        loadUserJourney();
    }
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
                    loadUserJourney();
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
        api.getUserByUsername("eq." + username, "*,user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);

                    if (userId == null || userId.isEmpty()) {
                        userId = user.getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putString("KEY_USER_ID", userId).apply();
                    }

                    currentUserConditionIds.clear();
                    if (user.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition condition : user.getUserMedicalConditions()) {
                            if (condition.getConditionId() != null) {
                                currentUserConditionIds.add(condition.getConditionId());
                            }
                        }
                    }

                    currentPlanId = user.getCurrentWorkoutPlanId();
                    if (currentPlanId == null || currentPlanId.isEmpty()) {
                        currentPlanId = "a1111111-1111-1111-1111-111111111111";
                    }

                    tvHeroTitle.setText("Your Journey:\n" + resolveGoalName(user.getFitnessGoalId()));
                    loadCompletedSessionsThenPlan();
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
                lastCompletedDayIdToday = null;
                
                // Lấy ngày hôm nay dưới dạng String YYYY-MM-DD
                String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                if (response.isSuccessful() && response.body() != null) {
                    for (UserWorkoutSession s : response.body()) {
                        if (s.getPlanId() != null && s.getPlanId().equals(currentPlanId) && s.getDayId() != null) {
                            completedDayIds.add(s.getDayId());
                            
                            // Kiểm tra nếu bài tập này vừa tập xong trong ngày hôm nay
                            if (s.getStartedAt() != null && s.getStartedAt().startsWith(todayDateStr)) {
                                lastCompletedDayIdToday = s.getDayId();
                            }
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
                    fetchPersonalizedDetails(plan);
                }
            }
            @Override public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {}
        });
    }

    private void setupPlanUI(WorkoutPlan plan) {
        if (plan.getDays() != null && !plan.getDays().isEmpty()) {
            allDays = plan.getDays();
            java.util.Collections.sort(allDays, (a, b) -> Integer.compare(a.getDayOrder() != null ? a.getDayOrder() : 0, b.getDayOrder() != null ? b.getDayOrder() : 0));

            totalPlanDays = allDays.size();

            // 1. TÌM INDEX CHO TIMELINE (Bài tập tiếp theo chưa làm)
            int nextIncompleteIndex = 0;
            for (int i = 0; i < allDays.size(); i++) {
                if (!completedDayIds.contains(allDays.get(i).getId())) {
                    nextIncompleteIndex = i;
                    break;
                }
            }
            if (nextIncompleteIndex == 0 && !allDays.isEmpty() && completedDayIds.contains(allDays.get(0).getId())) {
                nextIncompleteIndex = allDays.size() - 1;
            }

            // 2. TÌM INDEX CHO MISSION CARD (Ưu tiên bài đã tập xong HÔM NAY)
            int missionIndex = nextIncompleteIndex;
            if (lastCompletedDayIdToday != null) {
                for (int i = 0; i < allDays.size(); i++) {
                    if (allDays.get(i).getId().equals(lastCompletedDayIdToday)) {
                        missionIndex = i;
                        break;
                    }
                }
            }

            todayWorkoutDay = allDays.get(missionIndex);

            tvTodaySub.setText("NGÀY " + (todayWorkoutDay.getDayOrder() != null ? todayWorkoutDay.getDayOrder() : (missionIndex + 1)));
            tvTodayName.setText(todayWorkoutDay.getName() != null ? todayWorkoutDay.getName() : "Ngày tập");

            String todayDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            int currentProgress = getSharedPreferences("WorkoutProgress", MODE_PRIVATE)
                    .getInt("PROGRESS_" + userId + "_" + todayDateStr, 0);

            // LOGIC HIỂN THỊ TRẠNG THÁI HOÀN THÀNH
            boolean showAsFinished = (lastCompletedDayIdToday != null && todayWorkoutDay != null && todayWorkoutDay.getId() != null && todayWorkoutDay.getId().equals(lastCompletedDayIdToday)) || (currentProgress == 100);

            if (showAsFinished) {
                tvTodayPercent.setText("100%");
                animateProgress(pbTodayCircular, 100, 1000);
                btnTodayAction.setText("BẮT ĐẦU LẠI");
                btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            } else {
                tvTodayPercent.setText(currentProgress + "%");
                animateProgress(pbTodayCircular, currentProgress, 1000);
                btnTodayAction.setText("BẮT ĐẦU");
                btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4DAA9A")));
            }

            tvProgressFraction.setText(completedDayIds.size() + " / " + totalPlanDays + " ngày hoàn thành");
            animateProgress(pbOverallProgress, totalPlanDays > 0 ? (int) ((completedDayIds.size() * 100.0) / totalPlanDays) : 0, 1200);
            
            setupTimeline(nextIncompleteIndex);
        }
    }

    private void fetchPersonalizedDetails(WorkoutPlan plan) {
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserDailyWorkoutsByPlan("eq." + userId, "eq." + plan.getId(), "*,exercises(*)").enqueue(new Callback<List<UserDailyWorkout>>() {
            @Override
            public void onResponse(Call<List<UserDailyWorkout>> call, Response<List<UserDailyWorkout>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<UserDailyWorkout> personalRecords = response.body();
                    Collections.sort(personalRecords, (o1, o2) -> Integer.compare(
                            o1.getExerciseOrder() != null ? o1.getExerciseOrder() : 0,
                            o2.getExerciseOrder() != null ? o2.getExerciseOrder() : 0)
                    );

                    java.util.Map<String, List<WorkoutDayExercise>> dayMap = new java.util.HashMap<>();
                    for (UserDailyWorkout udw : personalRecords) {
                        if (udw.getDayId() != null && udw.getExercise() != null) {
                            if (!dayMap.containsKey(udw.getDayId())) dayMap.put(udw.getDayId(), new ArrayList<>());
                            
                            // Constructor 4 tham số
                            WorkoutDayExercise wde = new WorkoutDayExercise(
                                    udw.getExercise(), udw.getSets(),
                                    udw.getReps() != null ? udw.getReps() : "12",
                                    udw.getRestTimeSeconds() != null ? udw.getRestTimeSeconds() : 60
                            );
                            dayMap.get(udw.getDayId()).add(wde);
                        }
                    }

                    if (plan.getDays() != null) {
                        for (WorkoutDay day : plan.getDays()) {
                            if (dayMap.containsKey(day.getId())) day.setExercises(dayMap.get(day.getId()));
                        }
                    }
                    setupPlanUI(plan);
                } else {
                    filterAndReplaceExercises(plan);
                }
            }
            @Override public void onFailure(Call<List<UserDailyWorkout>> call, Throwable t) { filterAndReplaceExercises(plan); }
        });
    }

    private void filterAndReplaceExercises(WorkoutPlan plan) {
        List<Integer> userConditionIds = currentUserConditionIds;

        if (userConditionIds.isEmpty()) {
            setupPlanUI(plan);
            return;
        }

        StringBuilder queryBuilder = new StringBuilder("in.(");
        for (int i = 0; i < userConditionIds.size(); i++) {
            queryBuilder.append(userConditionIds.get(i));
            if (i < userConditionIds.size() - 1) queryBuilder.append(",");
        }
        queryBuilder.append(")");

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getBannedMuscles(queryBuilder.toString(), "*").enqueue(new Callback<List<ConditionRestrictedMuscle>>() {
            @Override
            public void onResponse(Call<List<ConditionRestrictedMuscle>> call, Response<List<ConditionRestrictedMuscle>> response) {
                List<Integer> bannedMuscleIds = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (ConditionRestrictedMuscle restriction : response.body()) {
                        if (restriction.getMuscleGroupId() != null) {
                            bannedMuscleIds.add(restriction.getMuscleGroupId());
                        }
                    }
                }

                if (bannedMuscleIds.isEmpty()) {
                    setupPlanUI(plan);
                } else {
                    fetchReplacementsAndSwap(plan, bannedMuscleIds, api);
                }
            }

            @Override
            public void onFailure(Call<List<ConditionRestrictedMuscle>> call, Throwable t) {
                setupPlanUI(plan);
            }
        });
    }

    private void fetchReplacementsAndSwap(WorkoutPlan plan, List<Integer> bannedMuscleIds, SupabaseApiService api) {
        List<Integer> musclesNeedingReplacements = new ArrayList<>();
        if (plan.getDays() != null) {
            for (WorkoutDay day : plan.getDays()) {
                if (day.getExercises() != null) {
                    for (WorkoutDayExercise wde : day.getExercises()) {
                        Exercise ex = wde.getExercise();
                        if (ex != null && bannedMuscleIds.contains(ex.getMuscleGroupId()) && ex.getDifficultyLevelId() != null && ex.getDifficultyLevelId() > 1) {
                            if (!musclesNeedingReplacements.contains(ex.getMuscleGroupId())) {
                                musclesNeedingReplacements.add(ex.getMuscleGroupId());
                            }
                        }
                    }
                }
            }
        }

        if (musclesNeedingReplacements.isEmpty()) {
            setupPlanUI(plan);
            return;
        }

        StringBuilder muscleQuery = new StringBuilder("in.(");
        for (int i = 0; i < musclesNeedingReplacements.size(); i++) {
            muscleQuery.append(musclesNeedingReplacements.get(i));
            if (i < musclesNeedingReplacements.size() - 1) muscleQuery.append(",");
        }
        muscleQuery.append(")");

        api.getReplacementExercises(muscleQuery.toString(), "eq.1", "*").enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                List<Exercise> replacementPool = (response.isSuccessful() && response.body() != null) ? response.body() : new ArrayList<>();

                if (plan.getDays() != null) {
                    for (WorkoutDay day : plan.getDays()) {
                        if (day.getExercises() != null) {
                            List<WorkoutDayExercise> finalSafeExercises = new ArrayList<>();

                            for (WorkoutDayExercise wde : day.getExercises()) {
                                Exercise ex = wde.getExercise();
                                if (ex != null && bannedMuscleIds.contains(ex.getMuscleGroupId())) {
                                    if (ex.getDifficultyLevelId() != null && ex.getDifficultyLevelId() > 1) {
                                        Exercise easyExercise = null;
                                        for (Exercise repEx : replacementPool) {
                                            if (repEx.getMuscleGroupId().equals(ex.getMuscleGroupId())) {
                                                easyExercise = repEx;
                                                break;
                                            }
                                        }

                                        if (easyExercise != null) {
                                            wde.setExercise(easyExercise);
                                            finalSafeExercises.add(wde);
                                        }
                                    } else {
                                        finalSafeExercises.add(wde);
                                    }
                                } else {
                                    finalSafeExercises.add(wde);
                                }
                            }
                            day.setExercises(finalSafeExercises);
                        }
                    }
                }
                setupPlanUI(plan);
            }

            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                setupPlanUI(plan);
            }
        });
    }

    private void setupPlanUI(WorkoutPlan plan) {
        if (plan.getDays() != null && !plan.getDays().isEmpty()) {
            allDays = plan.getDays();
            java.util.Collections.sort(allDays, (a, b) -> Integer.compare(a.getDayOrder() != null ? a.getDayOrder() : 0, b.getDayOrder() != null ? b.getDayOrder() : 0));

            totalPlanDays = allDays.size();

            // 1. TÌM INDEX CHO TIMELINE (Bài tập tiếp theo chưa làm)
            int nextIncompleteIndex = 0;
            for (int i = 0; i < allDays.size(); i++) {
                if (!completedDayIds.contains(allDays.get(i).getId())) {
                    nextIncompleteIndex = i;
                    break;
                }
            }
            if (nextIncompleteIndex == 0 && !allDays.isEmpty() && completedDayIds.contains(allDays.get(0).getId())) {
                nextIncompleteIndex = allDays.size() - 1;
            }

            // 2. TÌM INDEX CHO MISSION CARD (Ưu tiên bài đã tập xong HÔM NAY)
            int missionIndex = nextIncompleteIndex;
            if (lastCompletedDayIdToday != null) {
                for (int i = 0; i < allDays.size(); i++) {
                    if (allDays.get(i).getId().equals(lastCompletedDayIdToday)) {
                        missionIndex = i;
                        break;
                    }
                }
            }

            todayWorkoutDay = allDays.get(missionIndex);
            
            // Bài này được coi là hoàn thành nếu nằm trong tập bài đã tập xong của cả Journey
            boolean isMissionDone = completedDayIds.contains(todayWorkoutDay.getId());

            // Hiển thị Card Mission
            tvTodaySub.setText("NGÀY " + (todayWorkoutDay.getDayOrder() != null ? todayWorkoutDay.getDayOrder() : (missionIndex + 1)));
            tvTodayName.setText(todayWorkoutDay.getName() != null ? todayWorkoutDay.getName() : "Ngày tập");

            String todayDateStr = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
            int currentProgress = getSharedPreferences("WorkoutProgress", MODE_PRIVATE)
                    .getInt("PROGRESS_" + userId + "_" + todayDateStr, 0);

            // LOGIC HIỂN THỊ TRẠNG THÁI HOÀN THÀNH
            // Một bài được coi là xong 100% trên card nếu: 
            // - Nó chính là bài đã hoàn thành hôm nay (lastCompletedDayIdToday)
            // - HOẶC progress trong máy đang báo 100%
            boolean showAsFinished = (lastCompletedDayIdToday != null && todayWorkoutDay.getId().equals(lastCompletedDayIdToday)) || (currentProgress == 100);

            if (showAsFinished) {
                tvTodayPercent.setText("100%");
                animateProgress(pbTodayCircular, 100, 1000);
                btnTodayAction.setText("BẮT ĐẦU LẠI");
                btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            } else {
                tvTodayPercent.setText(currentProgress + "%");
                animateProgress(pbTodayCircular, currentProgress, 1000);
                btnTodayAction.setText("BẮT ĐẦU");
                btnTodayAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4DAA9A")));
            }

            tvProgressFraction.setText(completedDayIds.size() + " / " + totalPlanDays + " ngày hoàn thành");
            animateProgress(pbOverallProgress, totalPlanDays > 0 ? (int) ((completedDayIds.size() * 100.0) / totalPlanDays) : 0, 1200);
            
            // HIGHLIGHT TIMELINE: Luôn trỏ tới bài tiếp theo chưa làm
            setupTimeline(nextIncompleteIndex);
        }
    }

    private void openDayExercises(WorkoutDay day) {
        if (day == null) return;

        String dayNameFromDb = day.getName() != null ? day.getName() : "Bài tập";

        boolean isRestDay = dayNameFromDb.toLowerCase().contains("ngh")
                || (day.getExercises() != null && day.getExercises().isEmpty());
        if (isRestDay) {
            Intent intent = new Intent(this, RestDayCompleteActivity.class);
            intent.putExtra("EXTRA_PLAN_ID", currentPlanId);
            intent.putExtra("EXTRA_DAY_ID", day.getId());
            startActivity(intent);
            return;
        }

        Intent intent = new Intent(this, ExerciseListActivity.class);
        intent.putExtra("EXTRA_PLAN_ID", currentPlanId);
        intent.putExtra("EXTRA_DAY_ID", day.getId());
        intent.putExtra("EXTRA_DAY_TITLE", dayNameFromDb);
        intent.putExtra("EXTRA_DAY_ORDER", day.getDayOrder());

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
        
        View navHistory = findViewById(R.id.nav_history);
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> startActivity(new Intent(this, WorkoutHistoryActivity.class)));
        }
    }
}