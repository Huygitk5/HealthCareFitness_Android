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
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.hcmute.edu.vn.util.ChatbotHelper;
import com.hcmute.edu.vn.util.FitnessCalculator;

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
    private CardView cardAllergiesWarning;

    private String username, userId;
    private Date selectedDate;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DailyMealAdapter breakfastAdapter, lunchAdapter, dinnerAdapter;
    private List<UserDailyMeal> breakfastList = new ArrayList<>();
    private List<UserDailyMeal> lunchList = new ArrayList<>();
    private List<UserDailyMeal> dinnerList = new ArrayList<>();

    private List<String> currentAllergies = new ArrayList<>();

    private double targetCalories = 0.0;
    private double targetCaloriesToBurn = 0.0; // kcal cần đốt qua tập (giảm mỡ)
    private String currentGoalName = ""; // "giảm" / "tăng" / "giữ"
    private double targetCarb = 0.0;
    private double targetProtein = 0.0;
    private double targetFat = 0.0;
    private boolean isGeneratingMeals = false;

    private TextView tvWeekLabel, btnPrevWeek, btnNextWeek;
    private Calendar currentWeekBase;

    private ActivityResultLauncher<Intent> addMealLauncher;

    private List<String> restrictedIngredientIds = new ArrayList<>();

    List<String> userAllergies = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nutrition);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
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

        FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);
        ChatbotHelper.setupChatbotFAB(this, fabChatbot);

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
        setupSurveyListeners();
    }

    private void setupSurveyListeners() {
        if (rgSurvey == null) return;
        rgSurvey.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                edtCustomFood.setEnabled(false);
                btnClearSurvey.setVisibility(View.VISIBLE);
            }
        });

        btnClearSurvey.setOnClickListener(v -> {
            rgSurvey.clearCheck();
            edtCustomFood.setEnabled(true);
            btnClearSurvey.setVisibility(View.GONE);
        });

        btnSubmitSurvey.setOnClickListener(v -> {
            int checkedId = rgSurvey.getCheckedRadioButtonId();
            if (checkedId != -1) {
                String message = "";
                if (checkedId == R.id.rbLess) message = "Bạn nên bổ sung thêm protein nhé!";
                else if (checkedId == R.id.rbExact) message = "Tuyệt vời, hãy giữ vững phong độ nhé!";
                else if (checkedId == R.id.rbMore) message = "Hôm nay bạn đã ăn dư calo, ngày mai hãy tập luyện thêm nhé!";
                
                new AlertDialog.Builder(this)
                        .setTitle("Nhận xét dinh dưỡng")
                        .setMessage(message)
                        .setPositiveButton("Đóng", (dialog, which) -> {
                            saveSurveyDoneStatus();
                        })
                        .show();
            } else {
                String customFood = edtCustomFood.getText().toString().trim();
                if (!customFood.isEmpty()) {
                    String surveyText = "Hôm nay tôi ăn: " + customFood + ", target của tôi là " + Math.round(targetCalories) + " kcal, hãy so sánh thức ăn tôi ăn với target kcal của ngày hôm nay được đề ra và nhận xét.";
                    Intent intent = new Intent(this, ChatbotActivity.class);
                    intent.putExtra("EXTRA_SURVEY_TEXT", surveyText);
                    startActivity(intent);
                    saveSurveyDoneStatus();
                } else {
                    Toast.makeText(this, "Vui lòng chọn hoặc nhập kết quả khẩu phần ăn hôm nay của bạn", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveSurveyDoneStatus() {
        if (userId != null && !userId.isEmpty()) {
            String targetDate = apiDateFormat.format(selectedDate);
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            pref.edit().putBoolean("SURVEY_" + userId + "_" + targetDate, true).apply();
            checkSurveyStatus();
        }
    }

    private void checkSurveyStatus() {
        if (userId != null && !userId.isEmpty() && cardSurvey != null) {
            String targetDate = apiDateFormat.format(selectedDate);
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            boolean isDone = pref.getBoolean("SURVEY_" + userId + "_" + targetDate, false);
            if (isDone) {
//                cardSurvey.setVisibility(View.GONE);
//                tvSurveyDone.setVisibility(View.VISIBLE);
            } else {
                cardSurvey.setVisibility(View.VISIBLE);
//                tvSurveyDone.setVisibility(View.GONE);
                rgSurvey.clearCheck();
                edtCustomFood.setText("");
                edtCustomFood.setEnabled(true);
                btnClearSurvey.setVisibility(View.GONE);
            }
        }
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

        java.util.Map<String, String> queryMap = new java.util.HashMap<>();
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    autoGenerateWeeklyMeals(res.body());
                } else {
                    isGeneratingMeals = false;
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {
                isGeneratingMeals = false;
            }
        });
    }

    private void autoGenerateWeeklyMeals(List<Food> allFoods) {
        if (allFoods == null || allFoods.isEmpty() || targetCalories <= 0) {
            isGeneratingMeals = false;
            return;
        }

        // --- LỌC THỰC PHẨM DỊ ỨNG ---
        List<Food> safeFoods = filterSafeFoods(allFoods);
        if (safeFoods.isEmpty()) safeFoods = allFoods;

        // --- PHÂN LOẠI MÓN ĂN THEO GOAL ---
        // Giảm mỡ  : ưu tiên protein cao, carb thấp
        // Tăng cơ  : ưu tiên carb & protein cao
        // Giữ dáng : cân bằng
        List<Food> breakfastPool = new ArrayList<>();
        List<Food> lunchPool = new ArrayList<>();
        List<Food> dinnerPool = new ArrayList<>();

        for (Food food : safeFoods) {
            double cal = food.getCalories() != null ? food.getCalories() : 0;
            if (cal <= 0) continue;

            double protein = food.getProteinG() != null ? food.getProteinG() : 0;
            double carb    = food.getCarbG()    != null ? food.getCarbG()    : 0;
            double fat     = food.getFatG()     != null ? food.getFatG()     : 0;

            // Tính "protein density" = protein% trong tổng macro
            double totalMacro = protein + carb + fat;
            double proteinPct = totalMacro > 0 ? protein / totalMacro : 0;

            boolean isHighProtein = proteinPct >= 0.3;   // ≥30% protein
            boolean isLowCalDense = cal < 350;            // Nhẹ — phù hợp bữa sáng

            // Phân loại đơn giản
            if (isLowCalDense) {
                breakfastPool.add(food);
            } else if (isHighProtein) {
                lunchPool.add(food);    // Bữa trưa: ưu tiên protein
                dinnerPool.add(food);
            } else {
                lunchPool.add(food);
                dinnerPool.add(food);
            }
        }

        // Fallback nếu pool rỗng
        if (breakfastPool.isEmpty()) breakfastPool.addAll(safeFoods);
        if (lunchPool.isEmpty())     lunchPool.addAll(safeFoods);
        if (dinnerPool.isEmpty())    dinnerPool.addAll(safeFoods);

        // --- TỶ LỆ PHÂN BỔ CALO THEO BỮA (theo goal) ---
        double bfRatio, lunchRatio, dinnerRatio;
        if (currentGoalName.contains("giảm")) {
            // Giảm mỡ: bữa tối nhẹ hơn (không muốn carb ban đêm)
            bfRatio = 0.35; lunchRatio = 0.40; dinnerRatio = 0.25;
        } else if (currentGoalName.contains("tăng")) {
            // Tăng cơ: bữa trưa nhiều nhất (carb cho tập luyện)
            bfRatio = 0.30; lunchRatio = 0.40; dinnerRatio = 0.30;
        } else {
            // Giữ dáng: đều nhau
            bfRatio = 0.25; lunchRatio = 0.40; dinnerRatio = 0.35;
        }

        double[] targets  = { targetCalories * bfRatio, targetCalories * lunchRatio, targetCalories * dinnerRatio };
        String[] types    = { "BREAKFAST", "LUNCH", "DINNER" };
        List<Food>[] pools = new List[]{ breakfastPool, lunchPool, dinnerPool };

        // --- GENERATE THỰC ĐƠN CHO 7 NGÀY  ---
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        int dow  = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff); // đầu tuần (Thứ Hai)

        List<UserDailyMeal> generatedMeals = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 0; day < 7; day++) {
            String dateStr = sdf.format(cal.getTime());

            for (int mealIdx  = 0; mealIdx < 3; mealIdx++) {
                double mealTarget      = targets[mealIdx];
                List<Food> pool        = pools[mealIdx];
                double accumulatedCal  = 0;
                int attempts           = 0;
                int addedFoods         = 0;
                int maxFoodsPerMeal    = 4;

                while (accumulatedCal < mealTarget - 20 && addedFoods < maxFoodsPerMeal && attempts < 15) {
                    attempts++;
                    Food f      = pool.get((int)(Math.random() * pool.size()));
                    double fCal = (f.getCalories() != null && f.getCalories() > 0) ? f.getCalories() : 100;

                    // Tính số phần ăn cần để đạt phần còn thiếu, làm tròn 0.5
                    double needed     = mealTarget - accumulatedCal;
                    double rawMult    = needed / fCal;
                    double multiplier = Math.round(rawMult * 2.0) / 2.0;  // làm tròn đến 0.5
                    multiplier        = Math.max(0.5, Math.min(multiplier, 3.0));

                    if (accumulatedCal + fCal * multiplier > mealTarget + 50) {
                        multiplier = 0.5;
                    }

                    generatedMeals.add(new UserDailyMeal(
                            userId, dateStr, types[mealIdx], f.getId(), multiplier));
                    accumulatedCal += fCal * multiplier;
                    addedFoods++;
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // --- LƯU LÊN SUPABASE ---
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

        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*, condition_restricted_ingredients(*)))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    User u = res.body().get(0);
                    if (userId == null || userId.isEmpty()) {
                        userId = u.getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putString("KEY_USER_ID", userId).apply();
                    }

                    if (u.getCurrentDailyCalories() != null) {
                        calculateTargetMacros(u.getCurrentDailyCalories(), u.getFitnessGoalId() != null ? u.getFitnessGoalId() : 3);
                    }

                    currentAllergies.clear();
                    restrictedIngredientIds.clear();

                    if (u.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : u.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                // 1. Lấy tên dị ứng để hiển thị UI
                                if (mc.getType() != null) {
                                    String type = mc.getType().toLowerCase();
                                    if (type.contains("allergy") || type.contains("dị ứng")) {
                                        currentAllergies.add(mc.getName());
                                    }
                                }
                                // 2. Lấy ID nguyên liệu cấm để AI né ra
                                if (mc.getRestrictedIngredients() != null) {
                                    for (com.hcmute.edu.vn.model.ConditionRestrictedIngredient cri : mc.getRestrictedIngredients()) {
                                        if (cri.getIngredientId() != null) {
                                            restrictedIngredientIds.add(cri.getIngredientId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    setupWarningDisplay(currentAllergies);
                    loadMealsForSelectedDate();
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                loadMealsForSelectedDate();
            }
        });
    }


    // -----------------------------------------------------------------------
    // FILTER SAFE FOODS
    // -----------------------------------------------------------------------

    private List<Food> filterSafeFoods(List<Food> allFoods) {
        List<Food> safeFoods = new ArrayList<>();
        for (Food food : allFoods) {
            boolean isSafe = true;

            // Lọc theo tên món
            if (food.getName() != null) {
                String foodName = food.getName().toLowerCase();
                for (String allergy : currentAllergies) {
                    if (allergy != null && !allergy.isEmpty()) {
                        String keyword = allergy.toLowerCase()
                                .replace("dị ứng", "").replace("allergy", "").trim();
                        if (!keyword.isEmpty() && foodName.contains(keyword)) {
                            isSafe = false; break;
                        }
                    }
                }
            }

            // Lọc theo nguyên liệu
            if (isSafe && food.getFoodIngredients() != null) {
                for (com.hcmute.edu.vn.model.FoodIngredient fi : food.getFoodIngredients()) {
                    if (fi.getIngredientId() != null
                            && restrictedIngredientIds.contains(fi.getIngredientId())) {
                        isSafe = false; break;
                    }
                    if (fi.getIngredient() != null && fi.getIngredient().getName() != null) {
                        String ingName = fi.getIngredient().getName().toLowerCase();
                        for (String allergy : currentAllergies) {
                            if (allergy != null && !allergy.isEmpty()) {
                                String keyword = allergy.toLowerCase()
                                        .replace("dị ứng", "").replace("allergy", "").trim();
                                if (!keyword.isEmpty() && ingName.contains(keyword)) {
                                    isSafe = false; break;
                                }
                            }
                        }
                    }
                    if (!isSafe) break;
                }
            }

            if (isSafe) safeFoods.add(food);
        }
        return safeFoods;
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

    /**
     * Tính macro targets từ dailyCalories + goalId.
     * Đồng thời lưu currentGoalName để dùng trong generate meal.
     */
    private void calculateTargetMacros(double dailyCalories, int goalId) {
        // Tỷ lệ macro theo mục tiêu
        double pRatio, cRatio, fRatio;
        if (goalId == 1) {          // Giảm mỡ: protein cao hơn, carb thấp hơn
            pRatio = 0.35; cRatio = 0.35; fRatio = 0.30;
            currentGoalName = "giảm";
        } else if (goalId == 2) {   // Tăng cơ: carb cao hơn để có năng lượng tập
            pRatio = 0.30; cRatio = 0.50; fRatio = 0.20;
            currentGoalName = "tăng";
        } else {                    // Giữ dáng
            pRatio = 0.30; cRatio = 0.40; fRatio = 0.30;
            currentGoalName = "giữ";
        }

        targetCalories = dailyCalories;
        targetProtein  = (dailyCalories * pRatio) / 4.0;
        targetCarb     = (dailyCalories * cRatio) / 4.0;
        targetFat      = (dailyCalories * fRatio) / 9.0;
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