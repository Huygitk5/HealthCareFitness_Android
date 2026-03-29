package com.hcmute.edu.vn.activity;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.adapter.ExerciseSelectionAdapter;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExerciseListActivity extends AppCompatActivity {

    private ArrayList<Exercise> exercises = new ArrayList<>();
    private RecyclerView rvExercises;
    private ExerciseAdapter exerciseAdapter;
    private String userId, currentDate, currentPlanId, currentDayId, username;
    private int currentDayOrder = 1;
    private boolean isEditMode = false;
    private List<Integer> userBannedMuscleIds = new ArrayList<>();
    private List<UserDailyWorkout> userDailyWorkoutIds = new ArrayList<>(); // To store DB record IDs for updates
    private List<String> completedDayIds = new ArrayList<>(); // Lịch sử buổi tập đã hoàn thành

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_exercise_list);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        rvExercises = findViewById(R.id.rvExercises);
        ViewCompat.setOnApplyWindowInsetsListener(rvExercises, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvExercises.setLayoutManager(new LinearLayoutManager(this));

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", "");
        userId = pref.getString("KEY_USER_ID", "");
        currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        if (userId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng!", Toast.LENGTH_SHORT).show();
        }

        // Nhận thông tin từ màn hình trước
        currentDayId = getIntent().getStringExtra("EXTRA_DAY_ID");
        currentDayOrder = getIntent().getIntExtra("EXTRA_DAY_ORDER", 1);
        currentPlanId = getIntent().getStringExtra("EXTRA_PLAN_ID");
        String dayTitle = getIntent().getStringExtra("EXTRA_DAY_TITLE");

        if (currentDayId == null) {
            // Toast.makeText(this, "Lưu ý: Đang hiển thị danh sách từ Intent", Toast.LENGTH_SHORT).show();
        }

        TextView tvDayTitle = findViewById(R.id.tvDayTitle);
        if(tvDayTitle != null) tvDayTitle.setText(dayTitle != null ? dayTitle : "Bài tập");

        if (getIntent().hasExtra("EXTRA_EXERCISE_LIST") && !getIntent().hasExtra("EXTRA_DAY_ID")) {
            // Trường hợp tập tự do hoặc đã có list (Thường từ Journey chuyển qua)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                exercises = getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST", ArrayList.class);
            } else {
                exercises = (ArrayList<Exercise>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");
            }
            setupAdapter();
        } else {
            // Ưu tiên tải từ bảng cá nhân (user_daily_workouts)
            checkAndLoadPersonalWorkouts();
        }

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            isEditMode = !isEditMode;
            if (exerciseAdapter != null) {
                exerciseAdapter.setEditMode(isEditMode);
            }
            ((ImageView)v).setColorFilter(isEditMode ? Color.parseColor("#4DAA9A") : Color.parseColor("#757575"));
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chuyển danh sách bài tập vào màn hình tập (ExerciseActivity)
        findViewById(R.id.btnStartWorkout).setOnClickListener(v -> {
            if (exercises == null || exercises.isEmpty()) {
                Toast.makeText(ExerciseListActivity.this, "Danh sách bài tập trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExerciseListActivity.this, ExerciseActivity.class);
            intent.putExtra("EXTRA_EXERCISE_LIST", exercises);
            intent.putExtra("EXTRA_DAY_ID", currentDayId);
            intent.putExtra("EXTRA_PLAN_ID", currentPlanId);

            if (getIntent().hasExtra("IS_FREE_WORKOUT")) {
                intent.putExtra("IS_FREE_WORKOUT", getIntent().getBooleanExtra("IS_FREE_WORKOUT", false));
            }
            startActivity(intent);
        });
    }

    private void setupAdapter() {
        if (exerciseAdapter == null) {
            exerciseAdapter = new ExerciseAdapter(exercises);
            exerciseAdapter.setEditMode(isEditMode);
            exerciseAdapter.setOnSwapClickListener((exercise, position) -> {
                openSwapDialog(exercise, position);
            });
            rvExercises.setAdapter(exerciseAdapter);
        } else {
            exerciseAdapter.setEditMode(isEditMode);
            exerciseAdapter.notifyDataSetChanged();
        }
    }

    private void checkAndLoadPersonalWorkouts() {
        showLoading(true);
        // Bước 0: Đồng bộ danh sách ngày đã hoàn thành để tránh xóa bậy
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserWorkoutHistory("eq." + userId, "day_id").enqueue(new Callback<List<UserWorkoutSession>>() {
            @Override
            public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                completedDayIds.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (UserWorkoutSession s : response.body()) {
                        if (s.getDayId() != null) completedDayIds.add(s.getDayId());
                    }
                }
                loadBannedMusclesThenCheckWorkouts();
            }
            @Override public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) { loadBannedMusclesThenCheckWorkouts(); }
        });
    }

    private void loadBannedMusclesThenCheckWorkouts() {
        // Bước 1: Lấy danh sách bệnh lý hiện tại
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserByUsername("eq." + username, "*,user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User user = response.body().get(0);
                    List<Integer> conditionIds = new ArrayList<>();
                    if (user.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition c : user.getUserMedicalConditions()) {
                            if (c.getConditionId() != null) conditionIds.add(c.getConditionId());
                        }
                    }
                    
                    if (conditionIds.isEmpty()) {
                        fetchExistingWorkouts(new ArrayList<>());
                    } else {
                        fetchBannedMusclesThenCheckWorkouts(conditionIds);
                    }
                } else {
                    fetchExistingWorkouts(new ArrayList<>());
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { fetchExistingWorkouts(new ArrayList<>()); }
        });
    }

    private void fetchBannedMusclesThenCheckWorkouts(List<Integer> conditionIds) {
        StringBuilder query = new StringBuilder("in.(");
        for (int i = 0; i < conditionIds.size(); i++) {
            query.append(conditionIds.get(i));
            if (i < conditionIds.size() - 1) query.append(",");
        }
        query.append(")");

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getBannedMuscles(query.toString(), "*").enqueue(new Callback<List<ConditionRestrictedMuscle>>() {
            @Override
            public void onResponse(Call<List<ConditionRestrictedMuscle>> call, Response<List<ConditionRestrictedMuscle>> response) {
                List<Integer> bannedMuscleIds = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (ConditionRestrictedMuscle m : response.body()) bannedMuscleIds.add(m.getMuscleGroupId());
                }
                fetchExistingWorkouts(bannedMuscleIds);
            }
            @Override public void onFailure(Call<List<ConditionRestrictedMuscle>> call, Throwable t) { fetchExistingWorkouts(new ArrayList<>()); }
        });
    }

    private void fetchExistingWorkouts(List<Integer> bannedMuscleIds) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        // TRUY VẤN THEO NGÀY CỦA GÓI TẬP (DAY_ID) THAY VÌ NGÀY DƯƠNG LỊCH (DATE)
        apiService.getUserDailyWorkoutsByDay("eq." + userId, "eq." + currentDayId, "*,exercises(*)").enqueue(new Callback<List<UserDailyWorkout>>() {
            @Override
            public void onResponse(Call<List<UserDailyWorkout>> call, Response<List<UserDailyWorkout>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    List<UserDailyWorkout> existing = response.body();
                    
                    // NẾU NGÀY NÀY ĐÃ TẬP XONG (CÓ TRONG LOG) -> KHÔNG CHECK BỆNH LÝ, HIỂN THỊ LUÔN ĐỂ GIỮ LỊCH SỬ
                    if (completedDayIds.contains(currentDayId)) {
                        displayExistingWorkouts(existing);
                        return;
                    }

                    // Kiểm tra xem có bài nào bị cấm không
                    boolean hasIllegalExercise = false;
                    for (UserDailyWorkout udw : existing) {
                        if (udw.getExercise() != null && bannedMuscleIds.contains(udw.getExercise().getMuscleGroupId())) {
                            hasIllegalExercise = true;
                            break;
                        }
                    }

                    if (hasIllegalExercise) {
                        showResetPrompt(existing);
                    } else {
                        displayExistingWorkouts(existing);
                    }
                } else {
                    // Chưa có -> Khởi tạo cả tuần chứa ngày này
                    initializeWeek(currentDayOrder);
                }
            }
            @Override public void onFailure(Call<List<UserDailyWorkout>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ExerciseListActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showResetPrompt(List<UserDailyWorkout> existing) {
        showLoading(false);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật theo sức khỏe")
                .setMessage("Phát hiện thay đổi về bệnh lý. Bạn có muốn đặt lại lịch tập hôm nay để cập nhật theo bệnh lý mới nhất không?")
                .setPositiveButton("ĐẶT LẠI", (dialog, which) -> {
                    resetAndRebuildWorkout();
                })
                .setNegativeButton("BỎ QUA", (dialog, which) -> {
                    displayExistingWorkouts(existing);
                })
                .setCancelable(false)
                .show();
    }

    private void resetAndRebuildWorkout() {
        showLoading(true);
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        
        // CHỈ XÓA NHỮNG NGÀY CHƯA TẬP (History-Aware Reset)
        String query = null;
        if (!completedDayIds.isEmpty()) {
            StringBuilder sb = new StringBuilder("not.in.(");
            for (int i = 0; i < completedDayIds.size(); i++) {
                sb.append(completedDayIds.get(i)).append(i == completedDayIds.size() - 1 ? "" : ",");
            }
            sb.append(")");
            query = sb.toString();
        }

        api.deleteUserDailyWorkoutsFlexible("eq." + userId, "eq." + currentPlanId, query)
                .enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                initializeWeek(currentDayOrder);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                showLoading(false);
                Toast.makeText(ExerciseListActivity.this, "Không thể reset", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayExistingWorkouts(List<UserDailyWorkout> body) {
        showLoading(false);
        userDailyWorkoutIds = body;
        exercises.clear();
        for (UserDailyWorkout udw : body) {
            Exercise ex = udw.getExercise();
            if (ex != null) {
                if (udw.getReps() != null) ex.setBaseRecommendedReps(udw.getReps());
                if (udw.getSets() != null) ex.setBaseRecommendedSets(udw.getSets());
                exercises.add(ex);
            }
        }
        setupAdapter();
    }

    private void initializeWeek(int clickedDayOrder) {
        showLoading(true);
        int startDay = ((clickedDayOrder - 1) / 7) * 7 + 1;
        int endDay = startDay + 6;

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getUserByUsername("eq." + username, "*,user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                List<Integer> conditionIds = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User u = response.body().get(0);
                    if (u.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition mc : u.getUserMedicalConditions()) if (mc.getConditionId() != null) conditionIds.add(mc.getConditionId());
                    }
                }

                if (conditionIds.isEmpty()) {
                    fetchWeekExercisesFromMaster(startDay, endDay, new ArrayList<>(), clickedDayOrder);
                } else {
                    StringBuilder inQuery = new StringBuilder("in.(");
                    for (int i = 0; i < conditionIds.size(); i++) {
                        inQuery.append(conditionIds.get(i)).append(i == conditionIds.size() - 1 ? "" : ",");
                    }
                    inQuery.append(")");
                    api.getBannedMuscles(inQuery.toString(), "*").enqueue(new Callback<List<ConditionRestrictedMuscle>>() {
                        @Override
                        public void onResponse(Call<List<ConditionRestrictedMuscle>> call, Response<List<ConditionRestrictedMuscle>> response) {
                            List<Integer> bannedIds = new ArrayList<>();
                            if (response.isSuccessful() && response.body() != null) {
                                for (ConditionRestrictedMuscle rm : response.body()) bannedIds.add(rm.getMuscleGroupId());
                            }
                            fetchWeekExercisesFromMaster(startDay, endDay, bannedIds, clickedDayOrder);
                        }
                        @Override public void onFailure(Call<List<ConditionRestrictedMuscle>> call, Throwable t) {
                            fetchWeekExercisesFromMaster(startDay, endDay, new ArrayList<>(), clickedDayOrder);
                        }
                    });
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { fetchWeekExercisesFromMaster(startDay, endDay, new ArrayList<>(), clickedDayOrder); }
        });
    }

    private void fetchWeekExercisesFromMaster(int startDay, int endDay, List<Integer> bannedIds, int clickedDayOrder) {
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        
        // 1. LẤY DANH SÁCH BÀI TẬP CÁ NHÂN HIỆN CÓ ĐỂ TRÁNH TRÙNG LẶP (Deduplication)
        api.getUserDailyWorkoutsByPlan("eq." + userId, "eq." + currentPlanId, "day_id").enqueue(new Callback<List<UserDailyWorkout>>() {
            @Override
            public void onResponse(Call<List<UserDailyWorkout>> call, Response<List<UserDailyWorkout>> response) {
                List<String> existingRecordsIds = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    for (UserDailyWorkout udw : response.body()) if (udw.getDayId() != null) existingRecordsIds.add(udw.getDayId());
                }
                
                // 2. TẢI MASTER PLAN VÀ GENERATE
                String select = "*, workout_day_exercises(*, exercise:exercises(*))";
                api.getWorkoutPlanById("eq." + currentPlanId, "*,workout_days(" + select + ")").enqueue(new Callback<List<WorkoutPlan>>() {
                    @Override
                    public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            WorkoutPlan plan = response.body().get(0);
                            List<UserDailyWorkout> toInsert = new ArrayList<>();
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            Calendar cal = Calendar.getInstance();
                            try { cal.setTime(sdf.parse(currentDate)); } catch (Exception e) { cal.setTime(new Date()); }

                            if (plan.getDays() != null) {
                                for (WorkoutDay day : plan.getDays()) {
                                    int dayOrder = day.getDayOrder() != null ? day.getDayOrder() : 0;
                                    if (dayOrder >= startDay && dayOrder <= endDay) {
                                        // SKIP NẾU: Đã tập xong (History) - HOẶC - Đã có bản ghi kế hoạch (Duplicate avoidance)
                                        if (completedDayIds.contains(day.getId()) || existingRecordsIds.contains(day.getId())) continue;

                                        Calendar dayCal = (Calendar) cal.clone();
                                        dayCal.add(Calendar.DAY_OF_YEAR, dayOrder - clickedDayOrder);
                                        String targetDate = sdf.format(dayCal.getTime());

                                        if (day.getExercises() != null) {
                                            int order = 0;
                                            for (WorkoutDayExercise wde : day.getExercises()) {
                                                Exercise ex = wde.getExercise();
                                                if (ex != null && !bannedIds.contains(ex.getMuscleGroupId())) {
                                                    UserDailyWorkout udw = new UserDailyWorkout(userId, targetDate, ex.getId(), wde.getSets(), String.valueOf(wde.getReps()), wde.getRestTimeSeconds(), order++, currentPlanId, day.getId());
                                                    udw.setExercise(null);
                                                    toInsert.add(udw);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (!toInsert.isEmpty()) {
                                saveAndDisplayPersonalWorkouts(toInsert);
                            } else {
                                showLoading(false);
                                checkAndLoadPersonalWorkouts(); // Load what exists
                            }
                        } else { showLoading(false); }
                    }
                    @Override public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) { showLoading(false); }
                });
            }
            @Override public void onFailure(Call<List<UserDailyWorkout>> call, Throwable t) { showLoading(false); }
        });
    }


    private void saveAndDisplayPersonalWorkouts(List<UserDailyWorkout> list) {
        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.addUserDailyWorkouts(list).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    checkAndLoadPersonalWorkouts(); // Load lại để lấy ID từ DB
                } else {
                    Toast.makeText(ExerciseListActivity.this, "Lỗi lưu bảng cá nhân: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ExerciseListActivity.this, "Lỗi mạng khi lưu bảng cá nhân", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openSwapDialog(Exercise oldEx, int position) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_selection_list);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        tvTitle.setText("Thay đổi bài tập");

        RecyclerView rvItems = dialog.findViewById(R.id.rvItems);
        rvItems.setLayoutManager(new LinearLayoutManager(this));

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        // Lọc bài tập tương tự: Cùng nhóm cơ, cùng độ khó (hoặc chênh lệch 1)
        api.getReplacementExercises("eq." + oldEx.getMuscleGroupId(), "eq." + oldEx.getDifficultyLevelId(), "*").enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Exercise> filtered = new ArrayList<>();
                    for (Exercise e : response.body()) if (!e.getId().equals(oldEx.getId())) filtered.add(e);

                    ExerciseSelectionAdapter selectAdapter = new ExerciseSelectionAdapter(filtered, count -> {});
                    rvItems.setAdapter(selectAdapter);

                    dialog.findViewById(R.id.btnConfirm).setOnClickListener(v -> {
                        ArrayList<Exercise> selected = selectAdapter.getSelectedExercises();
                        if (!selected.isEmpty()) {
                            performSwap(selected.get(0), position);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(ExerciseListActivity.this, "Vui lòng chọn 1 bài tập", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
            @Override public void onFailure(Call<List<Exercise>> call, Throwable t) {}
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void performSwap(Exercise newEx, int position) {
        if (userDailyWorkoutIds == null || position >= userDailyWorkoutIds.size()) return;
        
        UserDailyWorkout record = userDailyWorkoutIds.get(position);
        record.setExerciseId(newEx.getId());
        record.setExercise(null); // Clear joined object for update

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.updateUserDailyWorkout("eq." + record.getId(), record).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ExerciseListActivity.this, "Đã thay đổi bài tập!", Toast.LENGTH_SHORT).show();
                    checkAndLoadPersonalWorkouts();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(ExerciseListActivity.this, "Lỗi lưu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void showLoading(boolean show) {
        findViewById(R.id.progressBar).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateStatsUI() {
        if (tvExerciseCount == null || tvTotalTime == null || tvTotalCalories == null || exercises == null) return;

        int totalExercises = exercises.size();
        int totalActiveSeconds = 0;
        double totalCalories = 0.0;

        double weightKg = getSharedPreferences("UserPrefs", MODE_PRIVATE).getFloat("USER_WEIGHT", 65.0f);

        for (Exercise ex : exercises) {
            int sets = ex.getBaseRecommendedSets() != null ? ex.getBaseRecommendedSets() : 3;

            int reps = 12;
            if (ex.getBaseRecommendedReps() != null) {
                try {
                    String numOnly = ex.getBaseRecommendedReps().replaceAll("[^0-9]", "");
                    if (!numOnly.isEmpty()) reps = Integer.parseInt(numOnly);
                } catch (Exception ignored) {}
            }

            int timePerRep = ex.getTimePerRep() != null ? ex.getTimePerRep() : 3;
            int restTimePerSet = 60;

            int activeSeconds = sets * reps * timePerRep;
            int restSeconds = (sets > 0 ? sets - 1 : 0) * restTimePerSet;

            int exerciseTotalSeconds = activeSeconds + restSeconds;
            totalActiveSeconds += exerciseTotalSeconds;

            double metValue = 5.0; // Gym
            if (ex.getExerciseTypeId() != null) {
                if (ex.getExerciseTypeId() == 2) metValue = 3.5; // Cardio
                else if (ex.getExerciseTypeId() == 3) metValue = 8.0; // HIIT
            }

            double hours = exerciseTotalSeconds / 3600.0;
            totalCalories += (metValue * weightKg * hours);
        }

        int totalMinutes = (int) Math.ceil(totalActiveSeconds / 60.0);

        tvExerciseCount.setText(String.valueOf(totalExercises));
        tvTotalTime.setText(totalMinutes + " Phút");
        tvTotalCalories.setText(String.format(Locale.getDefault(), "%.1f kcal(≈)", totalCalories));
    }
}