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
    private TextView tvTotalCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;
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
    private final double TARGET_CALORIES = 2000.0;
    private final double TARGET_CARB = 250.0;
    private final double TARGET_PROTEIN = 120.0;
    private final double TARGET_FAT = 65.0;

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
        userId = pref.getString("KEY_USER_ID", ""); // Lấy ID của User để truy vấn bảng Meal

        selectedDate = new Date(); // Mặc định mở app lên là hôm nay

        initViews();
        setupCalendar();
        setupMealAdapters();
        setupClickListeners();

        loadUserAllergies();
        loadMealsForSelectedDate(); // Tải thực đơn của hôm nay

        setupBottomNavigation();

        // Nơi nhận tín hiệu quay về sau khi Thêm món xong
        addMealLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Vừa thêm món xong -> Tải lại danh sách thức ăn của ngày đó
                        loadMealsForSelectedDate();
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        boolean isAllergyDirty = pref.getBoolean("ALLERGY_DIRTY", true);

        if (isAllergyDirty) {
            new android.os.Handler().postDelayed(() -> {
                loadUserAllergies();
            }, 300);

            pref.edit().putBoolean("ALLERGY_DIRTY", false).apply();
        }
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
        progressCarb = findViewById(R.id.progressCarb);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);
        progressProtein = findViewById(R.id.progressProtein);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        progressFat = findViewById(R.id.progressFat);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvAllergiesWarning = findViewById(R.id.tvAllergiesWarning);
        cardAllergiesWarning = findViewById(R.id.cardAllergiesWarning);
    }

    // ===============================================
    // THANH CHỌN NGÀY (LỊCH 7 NGÀY)
    // ===============================================
    private void setupCalendar() {
        List<Date> dates = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();

        // Tạo danh sách 7 ngày (Từ hôm nay đến 6 ngày tới)
        for (int i = 0; i < 7; i++) {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        CalendarAdapter calendarAdapter = new CalendarAdapter(this, dates, date -> {
            selectedDate = date; // Đổi ngày đang chọn
            loadMealsForSelectedDate(); // Tải lại thực đơn của ngày mới
        });

        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCalendar.setAdapter(calendarAdapter);
    }

    // ===============================================
    // CÀI ĐẶT 3 DANH SÁCH BỮA ĂN
    // ===============================================
    private void setupMealAdapters() {
        DailyMealAdapter.OnMealDeleteListener deleteListener = meal -> deleteMealFromDatabase(meal);

        breakfastAdapter = new DailyMealAdapter(this, breakfastList, deleteListener);
        lunchAdapter = new DailyMealAdapter(this, lunchList, deleteListener);
        dinnerAdapter = new DailyMealAdapter(this, dinnerList, deleteListener);

        // Tắt tính năng cuộn lồng nhau để NestedScrollView cha hoạt động mượt
        rvBreakfast.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvLunch.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvDinner.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});

        rvBreakfast.setAdapter(breakfastAdapter);
        rvLunch.setAdapter(lunchAdapter);
        rvDinner.setAdapter(dinnerAdapter);
    }

    // ===============================================
    // BẮT SỰ KIỆN NÚT [+] THÊM MÓN
    // ===============================================
    private void setupClickListeners() {
        btnAddBreakfast.setOnClickListener(v -> openMealSearch("BREAKFAST"));
        btnAddLunch.setOnClickListener(v -> openMealSearch("LUNCH"));
        btnAddDinner.setOnClickListener(v -> openMealSearch("DINNER"));
    }

    private void openMealSearch(String mealType) {
        // TẠM THỜI MÌNH SẼ TRUYỀN SANG MÀN HÌNH TÌM KIẾM (Sẽ tạo ở bước sau)
        Intent intent = new Intent(NutritionActivity.this, MealSearchActivity.class);
        intent.putExtra("EXTRA_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", mealType);
        addMealLauncher.launch(intent);
    }

    // ===============================================
    // LẤY DỮ LIỆU THỰC ĐƠN TRONG NGÀY
    // ===============================================
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

                if (response.isSuccessful() && response.body() != null) {
                    // Phân loại thức ăn vào đúng bữa
                    for (UserDailyMeal meal : response.body()) {
                        if ("BREAKFAST".equalsIgnoreCase(meal.getMealType())) breakfastList.add(meal);
                        else if ("LUNCH".equalsIgnoreCase(meal.getMealType())) lunchList.add(meal);
                        else if ("DINNER".equalsIgnoreCase(meal.getMealType())) dinnerList.add(meal);
                    }
                }

                // Cập nhật Giao diện
                updateMealUI();
                // Tính toán Macro dựa trên đống thức ăn này
                calculateAndDisplayTotals(response.body());
            }

            @Override
            public void onFailure(Call<List<UserDailyMeal>> call, Throwable t) {
                Toast.makeText(NutritionActivity.this, "Lỗi kết nối tải thực đơn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMealUI() {
        breakfastAdapter.updateList(breakfastList);
        lunchAdapter.updateList(lunchList);
        dinnerAdapter.updateList(dinnerList);

        // Ẩn/Hiện chữ "Chưa có thực đơn"
        tvEmptyBreakfast.setVisibility(breakfastList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyLunch.setVisibility(lunchList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyDinner.setVisibility(dinnerList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ===============================================
    // XÓA MÓN ĂN VÀ TÍNH LẠI MACRO
    // ===============================================
    private void deleteMealFromDatabase(UserDailyMeal meal) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.deleteDailyMeal("eq." + meal.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(NutritionActivity.this, "Đã xóa món ăn!", Toast.LENGTH_SHORT).show();
                    loadMealsForSelectedDate(); // Tải lại danh sách
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
        tvTotalCarb.setText(Math.round(totalCarb) + "g/" + Math.round(TARGET_CARB) + "g");
        tvTotalProtein.setText(Math.round(totalProtein) + "g/" + Math.round(TARGET_PROTEIN) + "g");
        tvTotalFat.setText(Math.round(totalFat) + "g/" + Math.round(TARGET_FAT) + "g");

        int progCal = (int) ((totalCal / TARGET_CALORIES) * 100);
        int progCarb = (int) ((totalCarb / TARGET_CARB) * 100);
        int progPro = (int) ((totalProtein / TARGET_PROTEIN) * 100);
        int progFat = (int) ((totalFat / TARGET_FAT) * 100);

        animateProgress(progressCalories, progCal);
        animateProgress(progressCarb, progCarb);
        animateProgress(progressProtein, progPro);
        animateProgress(progressFat, progFat);
    }

    private void animateProgress(android.widget.ProgressBar progressBar, int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    // ===============================================
    // CÁC HÀM CŨ GIỮ NGUYÊN (Dị ứng, Thanh điều hướng)
    // ===============================================
    // ===============================================
    // HÀM LẤY DANH SÁCH DỊ ỨNG VÀ HIỂN THỊ (CÓ RÚT GỌN)
    // ===============================================
    private void loadUserAllergies() {
        if (username == null || username.isEmpty()) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);
                    List<String> allergyList = new ArrayList<>();

                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                String type = mc.getType();
                                // Lọc riêng phần Dị ứng
                                if (type != null && (type.toLowerCase().contains("allergy") || type.toLowerCase().contains("dị ứng"))) {
                                    allergyList.add(mc.getName());
                                }
                            }
                        }
                    }

                    // Gọi hàm thiết lập giao diện
                    setupWarningDisplay(allergyList);
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    // HÀM HELPER: RÚT GỌN CHUỖI VÀ TẠO SỰ KIỆN CLICK
    private void setupWarningDisplay(List<String> dataList) {
        if (dataList.isEmpty()) {
            tvAllergiesWarning.setText("⚠️ Tránh: Không có");
            cardAllergiesWarning.setOnClickListener(null); // Không có gì thì khóa click
            return;
        }

        StringBuilder displayStr = new StringBuilder();
        StringBuilder fullStr = new StringBuilder();

        for (int i = 0; i < dataList.size(); i++) {
            String item = "• " + dataList.get(i) + "\n";
            fullStr.append(item);

            if (i < 3) {
                displayStr.append(item);
            }
        }

        if (dataList.size() > 3) {
            int extra = dataList.size() - 3;
            displayStr.append("+ ").append(extra).append(" mục khác...");
        }

        tvAllergiesWarning.setText("⚠️ Tránh:\n" + displayStr.toString().trim());

        // Bắt sự kiện mở Pop-up
        String finalFullContent = fullStr.toString().trim();
        cardAllergiesWarning.setOnClickListener(v -> showDetailDialog("Thực phẩm cần tránh", finalFullContent));
    }

    // HÀM TẠO POP-UP (MATERIAL DIALOG)
    private void showDetailDialog(String title, String fullContent) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(fullContent)
                .setPositiveButton("ĐÓNG", (dialog, which) -> dialog.dismiss())
                .show();
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
}