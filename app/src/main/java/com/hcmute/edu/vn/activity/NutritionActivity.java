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

    // === GIAO DIỆN MỚI ===
    private RecyclerView rvCalendar;
    private RecyclerView rvBreakfast, rvLunch, rvDinner;
    private TextView tvEmptyBreakfast, tvEmptyLunch, tvEmptyDinner;
    private TextView btnAddBreakfast, btnAddLunch, btnAddDinner;

    // === DASHBOARD ===
    private CircularProgressIndicator progressCalories;
    private LinearProgressIndicator progressCarb, progressProtein, progressFat;
    private TextView tvTotalCalories, tvTargetCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;
    private TextView tvAllergiesWarning;
    private androidx.cardview.widget.CardView cardAllergiesWarning;

    // === BIẾN DỮ LIỆU ===
    private String username, userId;
    private Date selectedDate; // Ngày đang được chọn trên lịch
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DailyMealAdapter breakfastAdapter, lunchAdapter, dinnerAdapter;
    private List<UserDailyMeal> breakfastList = new ArrayList<>();
    private List<UserDailyMeal> lunchList = new ArrayList<>();
    private List<UserDailyMeal> dinnerList = new ArrayList<>();

    // === MỤC TIÊU (TARGET) ===
    private double targetCalories = 0.0;
    private double targetCarb = 0.0;
    private double targetProtein = 0.0;
    private double targetFat = 0.0;
    private boolean isGeneratingMeals = false;

    // === ĐIỀU HƯỚNG TUẦN ===
    private TextView tvWeekLabel, btnPrevWeek, btnNextWeek;
    private Calendar currentWeekBase;

    // Lắng nghe khi thêm món mới xong thì reload lại dữ liệu
    private ActivityResultLauncher<Intent> addMealLauncher;

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
        currentWeekBase.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        initViews();
        setupCalendar();
        setupMealAdapters();
        setupClickListeners();

        loadUserAllergies();
        loadMealsForSelectedDate(); 

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
                            // SAU KHI XÓA: Reset cờ hiệu và tải lại Macro để kích hoạt AI tạo mới
                            isGeneratingMeals = false;
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
                    Toast.makeText(this, "Đã áp dụng Macro mới. Vui lòng thêm/bớt món ăn để khớp Calo nhé!", Toast.LENGTH_LONG).show();
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

        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
        if (tvWeekLabel != null) {
            tvWeekLabel.setText("Tháng " + monthFormat.format(cal.getTime()));
        }

        int selectedIndex = 0; 
        SimpleDateFormat matchFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String targetDateStr = matchFormat.format(selectedDate); 

        for (int i = 0; i < 7; i++) {
            dates.add(cal.getTime());
            if (matchFormat.format(cal.getTime()).equals(targetDateStr)) {
                selectedIndex = i;
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        CalendarAdapter calendarAdapter = new CalendarAdapter(this, dates, selectedIndex, date -> {
            selectedDate = date;
            loadMealsForSelectedDate();
        });

        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCalendar.setAdapter(calendarAdapter);
        rvCalendar.scrollToPosition(selectedIndex);
    }

    private void setupMealAdapters() {
        DailyMealAdapter.OnMealItemListener mealListener = new DailyMealAdapter.OnMealItemListener() {
            @Override
            public void onDeleteClick(UserDailyMeal meal) {
                deleteMealFromDatabase(meal);
            }

            @Override
            public void onSwapClick(UserDailyMeal meal) {
                handleSwapMeal(meal);
            }
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
        
        if (btnPrevWeek != null) {
            btnPrevWeek.setOnClickListener(v -> {
                currentWeekBase.add(Calendar.WEEK_OF_YEAR, -1);
                Calendar sdCal = Calendar.getInstance();
                sdCal.setTime(selectedDate);
                sdCal.add(Calendar.DAY_OF_YEAR, -7);
                selectedDate = sdCal.getTime();
                setupCalendar();
                loadMealsForSelectedDate();
            });
        }

        if (btnNextWeek != null) {
            btnNextWeek.setOnClickListener(v -> {
                currentWeekBase.add(Calendar.WEEK_OF_YEAR, 1);
                Calendar sdCal = Calendar.getInstance();
                sdCal.setTime(selectedDate);
                sdCal.add(Calendar.DAY_OF_YEAR, 7);
                selectedDate = sdCal.getTime();
                setupCalendar();
                loadMealsForSelectedDate();
            });
        }
    }

    private void openMealSearch(String mealType) {
        Intent intent = new Intent(NutritionActivity.this, MealSearchActivity.class);
        intent.putExtra("EXTRA_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", mealType);
        addMealLauncher.launch(intent);
    }

    private void loadMealsForSelectedDate() {
        if (userId == null || userId.isEmpty()) return;

        String formattedDate = apiDateFormat.format(selectedDate);
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getDailyMeals("eq." + userId, "eq." + formattedDate, "*, foods(*)").enqueue(new Callback<List<UserDailyMeal>>() {
            @Override
            public void onResponse(Call<List<UserDailyMeal>> call, Response<List<UserDailyMeal>> response) {
                breakfastList.clear();
                lunchList.clear();
                dinnerList.clear();

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

                    Calendar todayMon = Calendar.getInstance();
                    todayMon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    todayMon.set(Calendar.HOUR_OF_DAY, 0); 

                    Calendar selectedMon = Calendar.getInstance();
                    selectedMon.setTime(selectedDate);
                    selectedMon.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    selectedMon.set(Calendar.HOUR_OF_DAY, 0);

                    if (!selectedMon.getTime().before(todayMon.getTime())) {
                        // ĐÃ SỬA: Luôn kiểm tra targetCalories. Nếu chưa có, loadUserAllergies sẽ lo việc kích hoạt AI sau.
                        if (!isGeneratingMeals && targetCalories > 0) {
                            triggerAIGeneration();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserDailyMeal>> call, Throwable t) {
                Toast.makeText(NutritionActivity.this, "Lỗi kết nối tải thực đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerAIGeneration() {
        isGeneratingMeals = true;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.searchFoods(new java.util.HashMap<>()).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> resFoods) {
                if (resFoods.isSuccessful() && resFoods.body() != null) {
                    autoGenerateWeeklyMeals(resFoods.body());
                } else {
                    isGeneratingMeals = false;
                }
            }
            @Override
            public void onFailure(Call<List<Food>> call, Throwable t) {
                isGeneratingMeals = false;
            }
        });
    }

    private void updateMealUI() {
        breakfastAdapter.updateList(breakfastList);
        lunchAdapter.updateList(lunchList);
        dinnerAdapter.updateList(dinnerList);

        tvEmptyBreakfast.setVisibility(breakfastList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyLunch.setVisibility(lunchList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyDinner.setVisibility(dinnerList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void deleteMealFromDatabase(UserDailyMeal meal) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.deleteDailyMeal("eq." + meal.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NutritionActivity.this, "Đã xóa món ăn!", Toast.LENGTH_SHORT).show();
                    loadMealsForSelectedDate(); 
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(NutritionActivity.this, "Xóa thất bại!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAndDisplayTotals(List<UserDailyMeal> allMealsForToday) {
        double totalCal = 0, totalCarb = 0, totalProtein = 0, totalFat = 0;

        if (allMealsForToday != null) {
            for (UserDailyMeal meal : allMealsForToday) {
                if (meal.getFood() != null) {
                    double qty = meal.getQuantityMultiplier();
                    totalCal += (meal.getFood().getCalories() != null ? meal.getFood().getCalories() : 0) * qty;
                    totalCarb += (meal.getFood().getCarbG() != null ? meal.getFood().getCarbG() : 0) * qty;
                    totalProtein += (meal.getFood().getProteinG() != null ? meal.getFood().getProteinG() : 0) * qty;
                    totalFat += (meal.getFood().getFatG() != null ? meal.getFood().getFatG() : 0) * qty;
                }
            }
        }

        tvTotalCalories.setText(String.valueOf(Math.round(totalCal)));
        tvTargetCalories.setText("/ " + Math.round(targetCalories) + " Kcal");
        tvTotalCarb.setText(Math.round(totalCarb) + "g/" + Math.round(targetCarb) + "g");
        tvTotalProtein.setText(Math.round(totalProtein) + "g/" + Math.round(targetProtein) + "g");
        tvTotalFat.setText(Math.round(totalFat) + "g/" + Math.round(targetFat) + "g");

        if (targetCalories > 0) {
            int progCal = (int) ((totalCal / targetCalories) * 100);
            int progCarb = (int) ((totalCarb / targetCarb) * 100);
            int progPro = (int) ((totalProtein / targetProtein) * 100);
            int progFat = (int) ((totalFat / targetFat) * 100);

            animateProgress(progressCalories, progCal);
            animateProgress(progressCarb, progCarb);
            animateProgress(progressProtein, progPro);
            animateProgress(progressFat, progFat);
        }

        if (targetCalories > 0 && totalCal >= (targetCalories - 50)) {
            btnAddBreakfast.setVisibility(View.GONE);
            btnAddLunch.setVisibility(View.GONE);
            btnAddDinner.setVisibility(View.GONE);
        } else {
            btnAddBreakfast.setVisibility(View.VISIBLE);
            btnAddLunch.setVisibility(View.VISIBLE);
            btnAddDinner.setVisibility(View.VISIBLE);
        }
    }

    private void animateProgress(android.widget.ProgressBar progressBar, int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void loadUserAllergies() {
        if (username == null || username.isEmpty()) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);

                    if (currentUser.getCurrentDailyCalories() != null) {
                        int goalId = currentUser.getFitnessGoalId() != null ? currentUser.getFitnessGoalId() : 3;
                        calculateTargetMacros(currentUser.getCurrentDailyCalories(), goalId);

                        // ĐÃ SỬA: Sau khi tính xong Macro mới, nếu đang trống danh sách, kích hoạt AI ngay
                        if (breakfastList.isEmpty() && lunchList.isEmpty() && dinnerList.isEmpty()) {
                            if (!isGeneratingMeals) triggerAIGeneration();
                        } else {
                            // Nếu đã có danh sách, chỉ cần cập nhật lại dashboard
                            calculateAndDisplayTotals(new ArrayList<>());
                            loadMealsForSelectedDate();
                        }
                    }

                    List<String> allergyList = new ArrayList<>();
                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                String type = mc.getType();
                                if (type != null && (type.toLowerCase().contains("allergy") || type.toLowerCase().contains("dị ứng"))) {
                                    allergyList.add(mc.getName());
                                }
                            }
                        }
                    }
                    setupWarningDisplay(allergyList);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
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
    
    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(NutritionActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(NutritionActivity.this, WorkoutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(NutritionActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void calculateTargetMacros(double tdee, int goalId) {
        double proteinRatio = 0.3, carbRatio = 0.4, fatRatio = 0.3;
        if (goalId == 1) { proteinRatio = 0.4; carbRatio = 0.3; fatRatio = 0.3; }
        else if (goalId == 2) { proteinRatio = 0.3; carbRatio = 0.5; fatRatio = 0.2; }

        targetCalories = tdee;
        targetProtein = (tdee * proteinRatio) / 4.0;
        targetCarb = (tdee * carbRatio) / 4.0;
        targetFat = (tdee * fatRatio) / 9.0;
    }

    private void autoGenerateWeeklyMeals(List<Food> safeFoods) {
        if (safeFoods == null || safeFoods.isEmpty() || targetCalories <= 0) {
            isGeneratingMeals = false;
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate); 
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        List<UserDailyMeal> generatedMeals = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        double breakfastTarget = targetCalories * 0.25;
        double lunchTarget = targetCalories * 0.40;
        double dinnerTarget = targetCalories * 0.35;

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            Food breakfastFood = safeFoods.get((int) (Math.random() * safeFoods.size()));
            Food lunchFood = safeFoods.get((int) (Math.random() * safeFoods.size()));
            Food dinnerFood = safeFoods.get((int) (Math.random() * safeFoods.size()));

            double bCal = breakfastFood.getCalories() > 0 ? breakfastFood.getCalories() : 1;
            double lCal = lunchFood.getCalories() > 0 ? lunchFood.getCalories() : 1;
            double dCal = dinnerFood.getCalories() > 0 ? dinnerFood.getCalories() : 1;

            double bMultiplier = 1.0, lMultiplier = 1.0, dMultiplier = 1.0;

            while ((bMultiplier * bCal) + (lMultiplier * lCal) + (dMultiplier * dCal) < targetCalories - 50) {
                if ((bMultiplier * bCal) < breakfastTarget) bMultiplier += 0.5;
                else if ((lMultiplier * lCal) < lunchTarget) lMultiplier += 0.5;
                else if ((dMultiplier * dCal) < dinnerTarget) dMultiplier += 0.5;
                else lMultiplier += 0.5;
                if (bMultiplier > 8 || lMultiplier > 8 || dMultiplier > 8) break;
            }

            generatedMeals.add(new UserDailyMeal(userId, dateStr, "BREAKFAST", breakfastFood.getId(), bMultiplier));
            generatedMeals.add(new UserDailyMeal(userId, dateStr, "LUNCH", lunchFood.getId(), lMultiplier));
            generatedMeals.add(new UserDailyMeal(userId, dateStr, "DINNER", dinnerFood.getId(), dMultiplier));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.addMultipleDailyMeals(generatedMeals).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                isGeneratingMeals = false;
                if (response.isSuccessful()) {
                    Toast.makeText(NutritionActivity.this, "Đã tạo thực đơn tuần mới!", Toast.LENGTH_SHORT).show();
                    loadMealsForSelectedDate(); 
                } else {
                    Toast.makeText(NutritionActivity.this, "Lỗi Server: Không thể lưu thực đơn!", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                isGeneratingMeals = false;
                Toast.makeText(NutritionActivity.this, "Lỗi mạng khi tạo thực đơn tự động!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSwapMeal(UserDailyMeal oldMeal) {
        if (oldMeal.getFood() == null) return;
        double baseCalo = oldMeal.getFood().getCalories() != null ? oldMeal.getFood().getCalories() : 0.0;
        double targetKcalToSwap = baseCalo * oldMeal.getQuantityMultiplier();

        Intent intent = new Intent(NutritionActivity.this, MealSwapActivity.class);
        intent.putExtra("EXTRA_OLD_MEAL_ID", oldMeal.getId());
        intent.putExtra("EXTRA_TARGET_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", oldMeal.getMealType());
        intent.putExtra("EXTRA_TARGET_KCAL", targetKcalToSwap);
        addMealLauncher.launch(intent);
    }
}
