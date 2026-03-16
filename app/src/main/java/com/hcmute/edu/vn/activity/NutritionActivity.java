package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserMedicalCondition;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NutritionActivity extends AppCompatActivity {

    RecyclerView rvBreakfastMeat, rvBreakfastVeggie, rvBreakfastCarb;
    RecyclerView rvLunchMeat, rvLunchVeggie, rvLunchCarb;
    RecyclerView rvDinnerMeat, rvDinnerVeggie, rvDinnerCarb;

    // Các thành phần của Dashboard
    CircularProgressIndicator progressCalories;
    LinearProgressIndicator progressCarb, progressProtein, progressFat;
    TextView tvTotalCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;

    // ĐÃ THÊM: Biến cho Khung cảnh báo dị ứng
    TextView tvAllergiesWarning;
    String username;

    // Biến lưu trữ món ăn ĐANG ĐƯỢC CHỌN
    Food selBfMeat, selBfVeggie, selBfCarb;
    Food selLunchMeat, selLunchVeggie, selLunchCarb;
    Food selDinnerMeat, selDinnerVeggie, selDinnerCarb;

    // Lưu trữ danh sách để có thể chèn món mới vào
    List<Food> listBfMeat, listBfVeggie, listBfCarb;
    List<Food> listLunchMeat, listLunchVeggie, listLunchCarb;
    List<Food> listDinnerMeat, listDinnerVeggie, listDinnerCarb;

    // Biến cho thanh Tab chuyển đổi
    TextView tabBreakfast, tabLunch, tabDinner;
    LinearLayout layoutBreakfast, layoutLunch, layoutDinner;

    // Mục tiêu (Target) trong ngày
    final double TARGET_CALORIES = 2000.0;
    final double TARGET_CARB = 250.0;
    final double TARGET_PROTEIN = 120.0;
    final double TARGET_FAT = 65.0;

    private androidx.activity.result.ActivityResultLauncher<Intent> foodListLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nutrition);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // ĐÃ THÊM: Lấy username để lát nữa gọi API
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        initViews();

        loadDataFromApi();

        // ĐÃ THÊM: Gọi API kéo dữ liệu Dị ứng của User
        loadUserAllergies();

        setupBottomNavigation();
        calculateAndDisplayTotals();

        // Lắng nghe dữ liệu trả về từ màn hình Xem thêm
        foodListLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String title = data.getStringExtra("CATEGORY_TITLE");

                        // Tái tạo lại món ăn user vừa chọn
                        Food newFood = new Food("999", data.getStringExtra("FOOD_NAME"), 1, "1 phần",
                                data.getDoubleExtra("FOOD_CAL", 0), data.getDoubleExtra("FOOD_P", 0),
                                data.getDoubleExtra("FOOD_C", 0), data.getDoubleExtra("FOOD_F", 0), 0.0);
                        newFood.setImageUrl(data.getStringExtra("FOOD_IMAGE"));
                        // Chèn món mới lên vị trí ĐẦU TIÊN của danh sách và chọn nó
                        if (title != null) {
                            if (title.contains("Thịt & Đạm (Bữa Sáng)")) addNewFoodToTop(rvBreakfastMeat, listBfMeat, newFood, f -> selBfMeat = f);
                            else if (title.contains("Rau củ & Chất xơ (Bữa Sáng)")) addNewFoodToTop(rvBreakfastVeggie, listBfVeggie, newFood, f -> selBfVeggie = f);
                            else if (title.contains("Tinh bột & Trái cây (Bữa Sáng)")) addNewFoodToTop(rvBreakfastCarb, listBfCarb, newFood, f -> selBfCarb = f);
                            else if (title.contains("Thịt & Đạm (Bữa Trưa)")) addNewFoodToTop(rvLunchMeat, listLunchMeat, newFood, f -> selLunchMeat = f);
                            else if (title.contains("Rau củ & Chất xơ (Bữa Trưa)")) addNewFoodToTop(rvLunchVeggie, listLunchVeggie, newFood, f -> selLunchVeggie = f);
                            else if (title.contains("Tinh bột & Trái cây (Bữa Trưa)")) addNewFoodToTop(rvLunchCarb, listLunchCarb, newFood, f -> selLunchCarb = f);
                            else if (title.contains("Thịt & Đạm (Bữa Tối)")) addNewFoodToTop(rvDinnerMeat, listDinnerMeat, newFood, f -> selDinnerMeat = f);
                            else if (title.contains("Rau củ & Chất xơ (Bữa Tối)")) addNewFoodToTop(rvDinnerVeggie, listDinnerVeggie, newFood, f -> selDinnerVeggie = f);
                            else if (title.contains("Tinh bột & Trái cây (Bữa Tối)")) addNewFoodToTop(rvDinnerCarb, listDinnerCarb, newFood, f -> selDinnerCarb = f);

                            calculateAndDisplayTotals();
                        }
                    }
                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserAllergies();
    }

    private void initViews() {
        rvBreakfastMeat = findViewById(R.id.rvBreakfastMeat);
        rvBreakfastVeggie = findViewById(R.id.rvBreakfastVeggie);
        rvBreakfastCarb = findViewById(R.id.rvBreakfastCarb);

        rvLunchMeat = findViewById(R.id.rvLunchMeat);
        rvLunchVeggie = findViewById(R.id.rvLunchVeggie);
        rvLunchCarb = findViewById(R.id.rvLunchCarb);

        rvDinnerMeat = findViewById(R.id.rvDinnerMeat);
        rvDinnerVeggie = findViewById(R.id.rvDinnerVeggie);
        rvDinnerCarb = findViewById(R.id.rvDinnerCarb);

        progressCalories = findViewById(R.id.progressCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        progressCarb = findViewById(R.id.progressCarb);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);
        progressProtein = findViewById(R.id.progressProtein);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        progressFat = findViewById(R.id.progressFat);
        tvTotalFat = findViewById(R.id.tvTotalFat);

        // ĐÃ THÊM: Ánh xạ view
        tvAllergiesWarning = findViewById(R.id.tvAllergiesWarning);

        findViewById(R.id.btnMoreBfMeat).setOnClickListener(v -> openFoodList("Thịt & Đạm (Bữa Sáng)", 1));
        findViewById(R.id.btnMoreBfVeggie).setOnClickListener(v -> openFoodList("Rau củ & Chất xơ (Bữa Sáng)", 2));
        findViewById(R.id.btnMoreBfCarb).setOnClickListener(v -> openFoodList("Tinh bột & Trái cây (Bữa Sáng)", 3));

        findViewById(R.id.btnMoreLunchMeat).setOnClickListener(v -> openFoodList("Thịt & Đạm (Bữa Trưa)", 1));
        findViewById(R.id.btnMoreLunchVeggie).setOnClickListener(v -> openFoodList("Rau củ & Chất xơ (Bữa Trưa)", 2));
        findViewById(R.id.btnMoreLunchCarb).setOnClickListener(v -> openFoodList("Tinh bột & Trái cây (Bữa Trưa)", 3));

        findViewById(R.id.btnMoreDinnerMeat).setOnClickListener(v -> openFoodList("Thịt & Đạm (Bữa Tối)", 1));
        findViewById(R.id.btnMoreDinnerVeggie).setOnClickListener(v -> openFoodList("Rau củ & Chất xơ (Bữa Tối)", 2));
        findViewById(R.id.btnMoreDinnerCarb).setOnClickListener(v -> openFoodList("Tinh bột & Trái cây (Bữa Tối)", 3));

        // Ánh xạ Tab chuyển đổi
        tabBreakfast = findViewById(R.id.tabBreakfast);
        tabLunch = findViewById(R.id.tabLunch);
        tabDinner = findViewById(R.id.tabDinner);

        layoutBreakfast = findViewById(R.id.layoutBreakfast);
        layoutLunch = findViewById(R.id.layoutLunch);
        layoutDinner = findViewById(R.id.layoutDinner);

        // Bắt sự kiện Click chuyển Tab
        tabBreakfast.setOnClickListener(v -> switchTab(0));
        tabLunch.setOnClickListener(v -> switchTab(1));
        tabDinner.setOnClickListener(v -> switchTab(2));

        // Khởi tạo các list rỗng để tránh NullPointerException khi app vừa mở
        listBfMeat = new ArrayList<>(); listBfVeggie = new ArrayList<>(); listBfCarb = new ArrayList<>();
        listLunchMeat = new ArrayList<>(); listLunchVeggie = new ArrayList<>(); listLunchCarb = new ArrayList<>();
        listDinnerMeat = new ArrayList<>(); listDinnerVeggie = new ArrayList<>(); listDinnerCarb = new ArrayList<>();
    }

    // ===============================================
    // HÀM LẤY DANH SÁCH DỊ ỨNG CỦA USER TỪ API
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
                    StringBuilder allergyStr = new StringBuilder();

                    // Chạy vòng lặp lọc ra các bệnh có type là "allergy"
                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null && "allergy".equalsIgnoreCase(mc.getType())) {
                                allergyStr.append(mc.getName()).append(", ");
                            }
                        }
                    }

                    // Cập nhật lên màn hình
                    if (allergyStr.length() > 0) {
                        String finalStr = allergyStr.substring(0, allergyStr.length() - 2);
                        tvAllergiesWarning.setText("⚠️ Tránh: " + finalStr);
                    } else {
                        tvAllergiesWarning.setText("⚠️ Tránh: Không có");
                    }
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                tvAllergiesWarning.setText("⚠️ Tránh: Lỗi kết nối mạng");
            }
        });
    }

    // ===============================================
    // HÀM TẢI DỮ LIỆU MÓN ĂN TỪ SUPABASE
    // ===============================================
    private void loadDataFromApi() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // 1. Tải danh sách Thịt & Đạm (Category = 1)
        apiService.getFoodsByCategory("eq.1", "*").enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> meats = response.body();
                    // Tạo bản sao độc lập cho 3 bữa
                    listBfMeat = new ArrayList<>(meats);
                    listLunchMeat = new ArrayList<>(meats);
                    listDinnerMeat = new ArrayList<>(meats);

                    setupRecyclerView(rvBreakfastMeat, listBfMeat, food -> selBfMeat = food);
                    setupRecyclerView(rvLunchMeat, listLunchMeat, food -> selLunchMeat = food);
                    setupRecyclerView(rvDinnerMeat, listDinnerMeat, food -> selDinnerMeat = food);
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {}
        });

        // 2. Tải danh sách Rau Củ & Chất xơ (Category = 2)
        apiService.getFoodsByCategory("eq.2", "*").enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> veggies = response.body();
                    listBfVeggie = new ArrayList<>(veggies);
                    listLunchVeggie = new ArrayList<>(veggies);
                    listDinnerVeggie = new ArrayList<>(veggies);

                    setupRecyclerView(rvBreakfastVeggie, listBfVeggie, food -> selBfVeggie = food);
                    setupRecyclerView(rvLunchVeggie, listLunchVeggie, food -> selLunchVeggie = food);
                    setupRecyclerView(rvDinnerVeggie, listDinnerVeggie, food -> selDinnerVeggie = food);
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {}
        });

        // 3. Tải danh sách Tinh bột & Trái cây (Category = 3)
        apiService.getFoodsByCategory("eq.3", "*").enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> carbs = response.body();
                    listBfCarb = new ArrayList<>(carbs);
                    listLunchCarb = new ArrayList<>(carbs);
                    listDinnerCarb = new ArrayList<>(carbs);

                    setupRecyclerView(rvBreakfastCarb, listBfCarb, food -> selBfCarb = food);
                    setupRecyclerView(rvLunchCarb, listLunchCarb, food -> selLunchCarb = food);
                    setupRecyclerView(rvDinnerCarb, listDinnerCarb, food -> selDinnerCarb = food);
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {}
        });
    }

    private void openFoodList(String title, int categoryId) {
        Intent intent = new Intent(NutritionActivity.this, FoodListActivity.class);
        intent.putExtra("CATEGORY_TITLE", title);
        intent.putExtra("CATEGORY_ID", categoryId);
        foodListLauncher.launch(intent);
    }

    // ===============================================
    // HÀM XỬ LÝ CHUYỂN ĐỔI TAB BỮA ĂN
    // ===============================================
    private void switchTab(int tabIndex) {
        tabBreakfast.setBackgroundResource(android.R.color.transparent);
        tabBreakfast.setTextColor(android.graphics.Color.parseColor("#757575"));

        tabLunch.setBackgroundResource(android.R.color.transparent);
        tabLunch.setTextColor(android.graphics.Color.parseColor("#757575"));

        tabDinner.setBackgroundResource(android.R.color.transparent);
        tabDinner.setTextColor(android.graphics.Color.parseColor("#757575"));

        layoutBreakfast.setVisibility(android.view.View.GONE);
        layoutLunch.setVisibility(android.view.View.GONE);
        layoutDinner.setVisibility(android.view.View.GONE);

        if (tabIndex == 0) {
            tabBreakfast.setBackgroundResource(R.drawable.bg_nav_active);
            tabBreakfast.setTextColor(android.graphics.Color.WHITE);
            layoutBreakfast.setVisibility(android.view.View.VISIBLE);

        } else if (tabIndex == 1) {
            tabLunch.setBackgroundResource(R.drawable.bg_nav_active);
            tabLunch.setTextColor(android.graphics.Color.WHITE);
            layoutLunch.setVisibility(android.view.View.VISIBLE);

        } else if (tabIndex == 2) {
            tabDinner.setBackgroundResource(R.drawable.bg_nav_active);
            tabDinner.setTextColor(android.graphics.Color.WHITE);
            layoutDinner.setVisibility(android.view.View.VISIBLE);
        }
    }

    // ===============================================
    // HÀM HELPER XỬ LÝ CHÈN MÓN MỚI LÊN ĐẦU DANH SÁCH
    // ===============================================
    private void addNewFoodToTop(RecyclerView rv, List<Food> list, Food newFood, OnFoodUpdate callback) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(newFood.getName())) {
                list.remove(i);
                break;
            }
        }
        list.add(0, newFood);
        setupRecyclerView(rv, list, callback);
    }

    private void calculateAndDisplayTotals() {
        double totalCal = 0, totalCarb = 0, totalProtein = 0, totalFat = 0;

        Food[] selectedFoods = {
                selBfMeat, selBfVeggie, selBfCarb,
                selLunchMeat, selLunchVeggie, selLunchCarb,
                selDinnerMeat, selDinnerVeggie, selDinnerCarb
        };

        for (Food f : selectedFoods) {
            if (f != null) {
                totalCal += f.getCalories() != null ? f.getCalories() : 0;
                totalCarb += f.getCarbG() != null ? f.getCarbG() : 0;
                totalProtein += f.getProteinG() != null ? f.getProteinG() : 0;
                totalFat += f.getFatG() != null ? f.getFatG() : 0;
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

    public interface OnFoodUpdate {
        void onUpdate(Food food);
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<Food> foodList, OnFoodUpdate callback) {
        FoodAdapter adapter = new FoodAdapter(this, foodList, food -> {
            callback.onUpdate(food);
            calculateAndDisplayTotals(); // Tính toán khi user click
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(adapter);

        // QUAN TRỌNG: Nếu có data, chọn món đầu tiên và TÍNH TOÁN LUÔN
        if (!foodList.isEmpty()) {
            callback.onUpdate(foodList.get(0));
            calculateAndDisplayTotals(); // Thêm dòng này để dashboard chạy ngay khi load xong data
        }
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