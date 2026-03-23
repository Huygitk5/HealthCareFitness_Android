package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.CalendarAdapter;
import com.hcmute.edu.vn.adapter.DailyMealAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyMeal;
import com.hcmute.edu.vn.model.UserMedicalCondition;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NutritionActivity extends AppCompatActivity {

    private RecyclerView rvCalendar;
    private RecyclerView rvBreakfast, rvLunch, rvDinner;
    private TextView tvEmptyBreakfast, tvEmptyLunch, tvEmptyDinner;
    private TextView btnAddBreakfast, btnAddLunch, btnAddDinner;

    private CircularProgressIndicator progressCalories;
    private LinearProgressIndicator progressCarb, progressProtein, progressFat;
    private TextView tvTotalCalories, tvTargetCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;
    private TextView tvAllergiesWarning;
    private androidx.cardview.widget.CardView cardAllergiesWarning;

    private String username, userId;
    private Date selectedDate;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DailyMealAdapter breakfastAdapter, lunchAdapter, dinnerAdapter;
    private List<UserDailyMeal> breakfastList = new ArrayList<>();
    private List<UserDailyMeal> lunchList = new ArrayList<>();
    private List<UserDailyMeal> dinnerList = new ArrayList<>();

    private List<String> currentAllergies = new ArrayList<>();

    private double targetCalories = 0.0;
    private double targetCarb = 0.0;
    private double targetProtein = 0.0;
    private double targetFat = 0.0;
    private boolean isGeneratingMeals = false;

    private TextView tvWeekLabel, btnPrevWeek, btnNextWeek;
    private Calendar currentWeekBase;

    private ActivityResultLauncher<Intent> addMealLauncher;

    List<String> userAllergies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nutrition);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");

        selectedDate = new Date();
        currentWeekBase = Calendar.getInstance();
        currentWeekBase.setFirstDayOfWeek(Calendar.MONDAY);
        currentWeekBase.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        initViews();
        setupCalendar();
        setupMealAdapters();
        setupClickListeners();

        loadUserAllergies();

        setupBottomNavigation();

        com.google.android.material.floatingactionbutton.FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);
        com.hcmute.edu.vn.util.ChatbotHelper.setupChatbotFAB(this, fabChatbot);

        addMealLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadMealsForSelectedDate();
                    }
                }
        );

        if (getIntent().hasExtra("EXTRA_ALLERGIES")) {
            userAllergies = getIntent().getStringArrayListExtra("EXTRA_ALLERGIES");
        }
        if (userAllergies == null) {
            userAllergies = new ArrayList<>();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (pref.getBoolean("TARGET_CHANGED", false)) {
            pref.edit().putBoolean("TARGET_CHANGED", false).apply();
            showRegenerateMealDialog();
        }
        else if (pref.getBoolean("ALLERGY_DIRTY", false)) {
            new android.os.Handler().postDelayed(this::loadUserAllergies, 300);
            pref.edit().putBoolean("ALLERGY_DIRTY", false).apply();
        }
    }

    private void showRegenerateMealDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Mục tiêu đã thay đổi! 🎯")
                .setMessage("Thực đơn hiện tại có thể không còn phù hợp với lượng Calo mới.\n\nBạn có muốn AI xóa sạch và thiết kế lại thực đơn từ đầu không?")
                .setPositiveButton("Tạo lại từ đầu", (dialog, which) -> {
                    Toast.makeText(this, "Đang dọn dẹp thực đơn cũ...", Toast.LENGTH_SHORT).show();
                    SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
                    apiService.deleteMealsByUserId("eq." + userId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            isGeneratingMeals = false;
                            breakfastList.clear();
                            lunchList.clear();
                            dinnerList.clear();
                            loadUserAllergies();
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            isGeneratingMeals = false;
                            loadUserAllergies();
                        }
                    });
                })
                .setNegativeButton("Giữ thực đơn cũ", (dialog, which) -> {
                    loadUserAllergies();
                })
                .setCancelable(false)
                .show();
    }

    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        rvBreakfast = findViewById(R.id.rvBreakfast);
        rvLunch = findViewById(R.id.rvLunch);
        rvDinner = findViewById(R.id.rvDinner);
        tvEmptyBreakfast = findViewById(R.id.tvEmptyBreakfast);
        tvEmptyLunch = findViewById(R.id.tvEmptyLunch);
        tvEmptyDinner = findViewById(R.id.tvEmptyDinner);
        btnAddBreakfast = findViewById(R.id.btnAddBreakfast);
        btnAddLunch = findViewById(R.id.btnAddLunch);
        btnAddDinner = findViewById(R.id.btnAddDinner);
        progressCalories = findViewById(R.id.progressCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTargetCalories = findViewById(R.id.tvTargetCalories);
        progressCarb = findViewById(R.id.progressCarb);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);
        progressProtein = findViewById(R.id.progressProtein);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        progressFat = findViewById(R.id.progressFat);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvAllergiesWarning = findViewById(R.id.tvAllergiesWarning);
        cardAllergiesWarning = findViewById(R.id.cardAllergiesWarning);
        tvWeekLabel = findViewById(R.id.tvWeekLabel);
        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);
    }

    private void setupCalendar() {
        List<Date> dates = new ArrayList<>();
        Calendar cal = (Calendar) currentWeekBase.clone();
        int selectedIndex = 0;
        SimpleDateFormat matchFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String targetDateStr = matchFormat.format(selectedDate);

        for (int i = 0; i < 7; i++) {
            dates.add(cal.getTime());
            if (matchFormat.format(cal.getTime()).equals(targetDateStr)) selectedIndex = i;
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        updateMonthLabel(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(this, dates, selectedIndex, date -> {
            selectedDate = date;
            updateMonthLabel(date);
            loadMealsForSelectedDate();
        });

        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCalendar.setAdapter(calendarAdapter);
        rvCalendar.scrollToPosition(selectedIndex);
    }

    private void updateMonthLabel(Date date) {
        if (tvWeekLabel == null) return;
        tvWeekLabel.setText("Tháng " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(date));
    }

    private void setupMealAdapters() {
        DailyMealAdapter.OnMealItemListener mealListener = new DailyMealAdapter.OnMealItemListener() {
            @Override public void onDeleteClick(UserDailyMeal meal) { deleteMealFromDatabase(meal); }
            @Override public void onSwapClick(UserDailyMeal meal) { handleSwapMeal(meal); }
        };
        breakfastAdapter = new DailyMealAdapter(this, breakfastList, mealListener);
        lunchAdapter = new DailyMealAdapter(this, lunchList, mealListener);
        dinnerAdapter = new DailyMealAdapter(this, dinnerList, mealListener);
        rvBreakfast.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvLunch.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvDinner.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvBreakfast.setAdapter(breakfastAdapter);
        rvLunch.setAdapter(lunchAdapter);
        rvDinner.setAdapter(dinnerAdapter);
    }

    private void setupClickListeners() {
        btnAddBreakfast.setOnClickListener(v -> openMealSearch("BREAKFAST"));
        btnAddLunch.setOnClickListener(v -> openMealSearch("LUNCH"));
        btnAddDinner.setOnClickListener(v -> openMealSearch("DINNER"));
        if (btnPrevWeek != null) btnPrevWeek.setOnClickListener(v -> shiftWeek(-7));
        if (btnNextWeek != null) btnNextWeek.setOnClickListener(v -> shiftWeek(7));
    }

    private void shiftWeek(int days) {
        currentWeekBase.add(Calendar.DAY_OF_YEAR, days);
        Calendar sdCal = Calendar.getInstance(); sdCal.setTime(selectedDate);
        sdCal.add(Calendar.DAY_OF_YEAR, days);
        selectedDate = sdCal.getTime();
        setupCalendar();
        loadMealsForSelectedDate();
    }

    private void openMealSearch(String mealType) {
        Intent intent = new Intent(NutritionActivity.this, MealSearchActivity.class);
        intent.putExtra("EXTRA_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", mealType);
        intent.putStringArrayListExtra("EXTRA_ALLERGIES", new ArrayList<>(currentAllergies));
        addMealLauncher.launch(intent);
    }

    private void loadMealsForSelectedDate() {
        if (userId == null || userId.isEmpty()) return;
        String formattedDate = apiDateFormat.format(selectedDate);
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getDailyMeals("eq." + userId, "eq." + formattedDate, "*, foods(*)").enqueue(new Callback<List<UserDailyMeal>>() {
            @Override
            public void onResponse(Call<List<UserDailyMeal>> call, Response<List<UserDailyMeal>> response) {
                breakfastList.clear(); lunchList.clear(); dinnerList.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (UserDailyMeal meal : response.body()) {
                        if ("BREAKFAST".equalsIgnoreCase(meal.getMealType())) breakfastList.add(meal);
                        else if ("LUNCH".equalsIgnoreCase(meal.getMealType())) lunchList.add(meal);
                        else if ("DINNER".equalsIgnoreCase(meal.getMealType())) dinnerList.add(meal);
                    }
                    updateMealUI();
                    calculateAndDisplayTotals(response.body());
                } else {
                    updateMealUI();
                    calculateAndDisplayTotals(new ArrayList<>());
                    Calendar today = Calendar.getInstance(); today.set(Calendar.HOUR_OF_DAY, 0);
                    Calendar selected = Calendar.getInstance(); selected.setTime(selectedDate);
                    if (!selected.before(today) && !isGeneratingMeals && targetCalories > 0) triggerAIGeneration();
                }
            }
            @Override public void onFailure(Call<List<UserDailyMeal>> call, Throwable t) {}
        });
    }

    private void triggerAIGeneration() {
        isGeneratingMeals = true;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.searchFoods(new java.util.HashMap<>()).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> res) {
                if (res.isSuccessful() && res.body() != null) autoGenerateWeeklyMeals(res.body());
                else isGeneratingMeals = false;
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) { isGeneratingMeals = false; }
        });
    }

    private void autoGenerateWeeklyMeals(List<Food> allFoods) {
        if (allFoods == null || allFoods.isEmpty() || targetCalories <= 0) {
            isGeneratingMeals = false;
            return;
        }

        // --- BƯỚC LỌC THỰC PHẨM DỊ ỨNG CHO AI ---
        List<Food> safeFoods = new ArrayList<>();
        for (Food food : allFoods) {
            boolean isAllergic = false;
            // Kiểm tra xem tên thực phẩm có chứa từ khóa dị ứng nào không
            if (food.getName() != null) {
                String foodName = food.getName().toLowerCase();
                for (String allergy : currentAllergies) {
                    if (allergy != null && !allergy.isEmpty()) {
                        String keyword = allergy.toLowerCase().replace("dị ứng", "").replace("allergy", "").trim();
                        if (!keyword.isEmpty() && foodName.contains(keyword)) {
                            isAllergic = true;
                            break; // Lập tức loại món này
                        }
                    }
                }
            }
            // Món nào qua ải thì mới được đưa vào danh sách an toàn
            if (!isAllergic) {
                safeFoods.add(food);
            }
        }

        // Fallback: Tránh crash nếu danh sách an toàn bị rỗng
        if (safeFoods.isEmpty()) {
            safeFoods = allFoods;
        }

        // --- BẮT ĐẦU TẠO THỰC ĐƠN TỪ DANH SÁCH AN TOÀN ---
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        int diff = (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) ? 6 : (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff);

        List<UserDailyMeal> generatedMeals = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        double[] targets = {targetCalories * 0.25, targetCalories * 0.40, targetCalories * 0.35};
        String[] types = {"BREAKFAST", "LUNCH", "DINNER"};

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            for (int m = 0; m < 3; m++) {
                double currentTarget = targets[m], accumulatedCal = 0;
                int count = 0;
                while (accumulatedCal < currentTarget - 15 && count < 6) {
                    // AI sẽ bốc random từ safeFoods
                    Food f = safeFoods.get((int) (Math.random() * safeFoods.size()));
                    double fCal = (f.getCalories() != null && f.getCalories() > 0) ? f.getCalories() : 100;
                    double multiplier = Math.max(0.3, Math.min(Math.round(((currentTarget - accumulatedCal) / fCal) * 10.0) / 10.0, 3.0));
                    if (count > 0 && (accumulatedCal + fCal * multiplier) > currentTarget + 50) {
                        multiplier = 0.3;
                        if (accumulatedCal + fCal * multiplier > currentTarget + 50) continue;
                    }
                    generatedMeals.add(new UserDailyMeal(userId, dateStr, types[m], f.getId(), multiplier));
                    accumulatedCal += (fCal * multiplier);
                    count++;
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.addMultipleDailyMeals(generatedMeals).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> res) {
                new android.os.Handler().postDelayed(() -> {
                    isGeneratingMeals = false;
                    loadMealsForSelectedDate();
                }, 1000);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                isGeneratingMeals = false;
            }
        });
    }

    private void updateMealUI() {
        breakfastAdapter.updateList(breakfastList); lunchAdapter.updateList(lunchList); dinnerAdapter.updateList(dinnerList);
        tvEmptyBreakfast.setVisibility(breakfastList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyLunch.setVisibility(lunchList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyDinner.setVisibility(dinnerList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void calculateAndDisplayTotals(List<UserDailyMeal> allMeals) {
        double totalCal = 0, totalCarb = 0, totalProtein = 0, totalFat = 0;
        if (allMeals != null) {
            for (UserDailyMeal m : allMeals) {
                if (m.getFood() != null) {
                    double q = m.getQuantityMultiplier();
                    totalCal += (m.getFood().getCalories() != null ? m.getFood().getCalories() : 0) * q;
                    totalCarb += (m.getFood().getCarbG() != null ? m.getFood().getCarbG() : 0) * q;
                    totalProtein += (m.getFood().getProteinG() != null ? m.getFood().getProteinG() : 0) * q;
                    totalFat += (m.getFood().getFatG() != null ? m.getFood().getFatG() : 0) * q;
                }
            }
        }
        tvTotalCalories.setText(String.valueOf(Math.round(totalCal)));
        tvTargetCalories.setText("/ " + Math.round(targetCalories) + " Kcal");
        tvTotalCarb.setText(Math.round(totalCarb) + "g/" + Math.round(targetCarb) + "g");
        tvTotalProtein.setText(Math.round(totalProtein) + "g/" + Math.round(targetProtein) + "g");
        tvTotalFat.setText(Math.round(totalFat) + "g/" + Math.round(targetFat) + "g");

        if (targetCalories > 0) {
            animateProgress(progressCalories, (int) ((totalCal / targetCalories) * 100));
            animateProgress(progressCarb, (int) ((totalCarb / targetCarb) * 100));
            animateProgress(progressProtein, (int) ((totalProtein / targetProtein) * 100));
            animateProgress(progressFat, (int) ((totalFat / targetFat) * 100));
        }
    }

    private void animateProgress(android.widget.ProgressBar pb, int p) {
        ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), p).setDuration(500).start();
    }

    private void loadUserAllergies() {
        if (username == null || username.isEmpty()) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getUserByUsername("eq." + username, "*, user_medical_conditions(*, medical_conditions(*))").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    User u = res.body().get(0);
                    // 1. Lưu ID
                    if (userId == null || userId.isEmpty()) {
                        userId = u.getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putString("KEY_USER_ID", userId).apply();
                    }

                    // 2. Tính toán Calories
                    if (u.getCurrentDailyCalories() != null) {
                        calculateTargetMacros(u.getCurrentDailyCalories(), u.getFitnessGoalId() != null ? u.getFitnessGoalId() : 3);
                    }

                    // 3. KHÔI PHỤC LOGIC LẤY DỊ ỨNG
                    currentAllergies.clear();
                    if (u.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : u.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null && mc.getType() != null) {
                                String type = mc.getType().toLowerCase();
                                if (type.contains("allergy") || type.contains("dị ứng")) {
                                    currentAllergies.add(mc.getName());
                                }
                            }
                        }
                    }
                    // 4. Hiển thị lên UI
                    setupWarningDisplay(currentAllergies);

                    // 5. Load meal
                    loadMealsForSelectedDate();
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                loadMealsForSelectedDate();
            }
        });
    }

    private void setupWarningDisplay(List<String> dataList) {
        if (dataList.isEmpty()) {
            tvAllergiesWarning.setText("⚠️ Cần tránh: Không có");
            cardAllergiesWarning.setOnClickListener(null);
            return;
        }

        StringBuilder displayStr = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            if (i < 3) displayStr.append("• ").append(dataList.get(i)).append("\n");
        }
        if (dataList.size() > 3) {
            int extra = dataList.size() - 3;
            displayStr.append("+ ").append(extra).append(" mục khác...");
        }
        tvAllergiesWarning.setText("⚠️ Cần tránh:\n" + displayStr.toString().trim());
        cardAllergiesWarning.setOnClickListener(v -> showCustomChipDialog("Thực phẩm cần tránh", dataList, true));
    }

    private void showCustomChipDialog(String title, List<String> items, boolean isAllergy) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_chips, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        com.google.android.material.chip.ChipGroup chipGroupItems = dialogView.findViewById(R.id.chipGroupItems);
        com.google.android.material.button.MaterialButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        tvDialogTitle.setText(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        for (String itemName : items) {
            TextView chip = new TextView(this);
            chip.setText(itemName);
            chip.setTextSize(14f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);
            int padX = (int) (16 * getResources().getDisplayMetrics().density);
            int padY = (int) (8 * getResources().getDisplayMetrics().density);
            chip.setPadding(padX, padY, padX, padY);

            android.graphics.drawable.GradientDrawable chipGradient = new android.graphics.drawable.GradientDrawable();
            chipGradient.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
            chipGradient.setCornerRadius(100f);

            if (isAllergy) {
                chipGradient.setColors(new int[]{android.graphics.Color.parseColor("#FFE0B2"), android.graphics.Color.parseColor("#FFCCBC")});
                chip.setTextColor(android.graphics.Color.parseColor("#BF360C"));
            } else {
                chipGradient.setColors(new int[]{android.graphics.Color.parseColor("#E0F2F1"), android.graphics.Color.parseColor("#B2DFDB")});
                chip.setTextColor(android.graphics.Color.parseColor("#004D40"));
            }
            chip.setBackground(chipGradient);
            chipGroupItems.addView(chip);
        }

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }


    private void calculateTargetMacros(double tdee, int goalId) {
        double pR = 0.3, cR = 0.4, fR = 0.3;
        if (goalId == 1) { pR = 0.4; cR = 0.3; fR = 0.3; } else if (goalId == 2) { pR = 0.3; cR = 0.5; fR = 0.2; }
        targetCalories = tdee; targetProtein = (tdee * pR) / 4.0; targetCarb = (tdee * cR) / 4.0; targetFat = (tdee * fR) / 9.0;
    }

    private void deleteMealFromDatabase(UserDailyMeal meal) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.deleteDailyMeal("eq." + meal.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadMealsForSelectedDate(); }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void handleSwapMeal(UserDailyMeal oldMeal) {
        if (oldMeal.getFood() == null) return;
        Intent intent = new Intent(this, MealSwapActivity.class);
        intent.putExtra("EXTRA_OLD_MEAL_ID", oldMeal.getId());
        intent.putExtra("EXTRA_TARGET_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", oldMeal.getMealType());
        intent.putExtra("EXTRA_TARGET_KCAL", (oldMeal.getFood().getCalories() != null ? oldMeal.getFood().getCalories() : 0.0) * oldMeal.getQuantityMultiplier());
        intent.putStringArrayListExtra("EXTRA_ALLERGIES", new ArrayList<>(currentAllergies));
        addMealLauncher.launch(intent);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> { startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_workout).setOnClickListener(v -> { startActivity(new Intent(this, WorkoutJourneyActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_profile).setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
    }
}