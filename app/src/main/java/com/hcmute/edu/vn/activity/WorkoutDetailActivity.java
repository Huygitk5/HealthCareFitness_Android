package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.WorkoutAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.ConditionRestrictedMuscle;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyWorkout;
import com.hcmute.edu.vn.model.UserMedicalCondition;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutDetailActivity extends AppCompatActivity {

    private RecyclerView rvWorkoutDays;
    private WorkoutAdapter adapter;
    private List<WorkoutDay> data = new ArrayList<>();
    private java.util.List<String> completedDayIdsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_workout);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(),
                getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        rvWorkoutDays = findViewById(R.id.rvWorkoutDays);
        ViewCompat.setOnApplyWindowInsetsListener(rvWorkoutDays, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        rvWorkoutDays.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String planId = getIntent().getStringExtra("PLAN_ID");
        String goalName = getIntent().getStringExtra("GOAL_NAME");
        if (goalName != null && !goalName.isEmpty()) {
            android.widget.TextView tvDetailTitle = findViewById(R.id.tvDetailTitle);
            if (tvDetailTitle != null) {
                tvDetailTitle.setText(goalName);
            }
        }
        ArrayList<String> ids = getIntent().getStringArrayListExtra("COMPLETED_DAY_IDS");
        if (ids != null) {
            completedDayIdsList.addAll(ids);
        }

        if (planId != null && !planId.isEmpty()) {
            fetchWorkoutPlan(planId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID gói tập!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchWorkoutPlan(String planId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*,workout_days(*, workout_day_exercises(*, exercise:exercises(*)))";

        apiService.getWorkoutPlanByIdAndSort("eq." + planId, selectQuery, "day_order.asc")
                .enqueue(new Callback<List<WorkoutPlan>>() {
                    @Override
                    public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            WorkoutPlan plan = response.body().get(0);
                            String userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID",
                                    "");
                            fetchPersonalizedDetails(plan, userId);
                        } else {
                            Toast.makeText(WorkoutDetailActivity.this, "Dữ liệu rỗng hoặc lỗi API", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                        Toast.makeText(WorkoutDetailActivity.this, "Lỗi kết nối mạng: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterAndDisplayPlan(WorkoutPlan plan) {
        String username = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER", "");
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);

        // 1. Lấy thông tin bệnh lý
        api.getUserByUsername("eq." + username, "user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    List<Integer> conditionIds = new ArrayList<>();
                    if (user.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition c : user.getUserMedicalConditions()) {
                            if (c.getConditionId() != null)
                                conditionIds.add(c.getConditionId());
                        }
                    }

                    if (conditionIds.isEmpty()) {
                        setupFinalUI(plan);
                    } else {
                        loadBannedMuscles(plan, conditionIds);
                    }
                } else {
                    setupFinalUI(plan);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                setupFinalUI(plan);
            }
        });
    }

    private void loadBannedMuscles(WorkoutPlan plan, List<Integer> conditionIds) {
        StringBuilder query = new StringBuilder("in.(");
        for (int i = 0; i < conditionIds.size(); i++) {
            query.append(conditionIds.get(i));
            if (i < conditionIds.size() - 1)
                query.append(",");
        }
        query.append(")");

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getBannedMuscles(query.toString(), "*").enqueue(new Callback<List<ConditionRestrictedMuscle>>() {
            @Override
            public void onResponse(Call<List<ConditionRestrictedMuscle>> call,
                    Response<List<ConditionRestrictedMuscle>> response) {
                List<Integer> bannedMuscleIds = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (ConditionRestrictedMuscle m : response.body())
                        bannedMuscleIds.add(m.getMuscleGroupId());
                }

                // Thực hiện lọc sơ bộ các bài tập không an toàn
                if (plan.getDays() != null) {
                    for (WorkoutDay day : plan.getDays()) {
                        if (day.getExercises() != null) {
                            List<WorkoutDayExercise> safeList = new ArrayList<>();
                            for (WorkoutDayExercise wde : day.getExercises()) {
                                if (wde.getExercise() != null) {
                                    if (!bannedMuscleIds.contains(wde.getExercise().getMuscleGroupId())) {
                                        safeList.add(wde);
                                    }
                                }
                            }
                            day.setExercises(safeList);
                        }
                    }
                }
                setupFinalUI(plan);
            }

            @Override
            public void onFailure(Call<List<ConditionRestrictedMuscle>> call, Throwable t) {
                setupFinalUI(plan);
            }
        });
    }

    private void fetchPersonalizedDetails(WorkoutPlan plan, String userId) {
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserDailyWorkoutsByPlan("eq." + userId, "eq." + plan.getId(), "*,exercises(*)")
                .enqueue(new Callback<List<UserDailyWorkout>>() {
                    @Override
                    public void onResponse(Call<List<UserDailyWorkout>> call,
                            Response<List<UserDailyWorkout>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            List<UserDailyWorkout> personalRecords = response.body();

                            // Sắp xếp bài tập theo thứ tự (exercise_order)
                            Collections.sort(personalRecords, (o1, o2) -> {
                                int order1 = o1.getExerciseOrder() != null ? o1.getExerciseOrder() : 0;
                                int order2 = o2.getExerciseOrder() != null ? o2.getExerciseOrder() : 0;
                                return Integer.compare(order1, order2);
                            });

                            java.util.Map<String, List<WorkoutDayExercise>> dayMap = new java.util.HashMap<>();
                            for (UserDailyWorkout udw : personalRecords) {
                                if (udw.getDayId() != null && udw.getExercise() != null) {
                                    if (!dayMap.containsKey(udw.getDayId()))
                                        dayMap.put(udw.getDayId(), new ArrayList<>());

                                    // Sử dụng constructor 4 tham số
                                    WorkoutDayExercise wde = new WorkoutDayExercise(
                                            udw.getExercise(),
                                            udw.getSets(),
                                            udw.getReps(),
                                            udw.getRestTimeSeconds());
                                    dayMap.get(udw.getDayId()).add(wde);
                                }
                            }

                            if (plan.getDays() != null) {
                                for (WorkoutDay day : plan.getDays()) {
                                    if (dayMap.containsKey(day.getId())) {
                                        day.setExercises(dayMap.get(day.getId()));
                                    }
                                }
                            }
                            setupFinalUI(plan);
                        } else {
                            filterAndDisplayPlan(plan);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<UserDailyWorkout>> call, Throwable t) {
                        filterAndDisplayPlan(plan);
                    }
                });
    }

    private void setupFinalUI(WorkoutPlan plan) {
        if (plan.getDays() != null) {
            data.clear();
            data.addAll(plan.getDays());

            int currentDayIndex = 0;
            for(int i = 0; i < data.size(); i++) {
                 if(!completedDayIdsList.contains(data.get(i).getId())) {
                      currentDayIndex = i; break;
                 }
            }
            if(currentDayIndex == 0 && !data.isEmpty() && completedDayIdsList.contains(data.get(0).getId())) {
                 currentDayIndex = data.size() - 1;
            }

            int total = data.size();
            int completed = completedDayIdsList.size();
            android.widget.TextView tvDaysLeft = findViewById(R.id.tvDaysLeft);
            if(tvDaysLeft != null) {
                 tvDaysLeft.setText((total - completed) + " ngày còn lại");
            }
            android.widget.ProgressBar pbWorkout = findViewById(R.id.pbWorkout);
            if(pbWorkout != null) {
                 pbWorkout.setMax(100);
                 int progress = total > 0 ? (int)((completed * 100.0) / total) : 0;
                 pbWorkout.setProgress(progress);
            }

            adapter = new WorkoutAdapter(data, plan.getId(), new java.util.HashSet<>(completedDayIdsList), currentDayIndex);
            rvWorkoutDays.setAdapter(adapter);
            rvWorkoutDays.scrollToPosition(Math.max(0, currentDayIndex - 1));
        } else {
            Toast.makeText(WorkoutDetailActivity.this, "Gói tập chưa có ngày nào!", Toast.LENGTH_SHORT).show();
        }
    }
}