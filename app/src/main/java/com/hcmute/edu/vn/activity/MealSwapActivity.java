package com.hcmute.edu.vn.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodSwapAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyMeal;
import com.hcmute.edu.vn.model.UserMedicalCondition;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealSwapActivity extends AppCompatActivity {

    private ImageButton btnBackSearch;
    private TextView tvSearchTitle;
    private EditText edtSearchFood;
    private RecyclerView rvFoodSearch;
    private MaterialButton btnSaveMeal;

    private FoodSwapAdapter adapter;
    private List<Food> safeFoodsList = new ArrayList<>(); // Các món đã qua lọc dị ứng và calo
    private List<Double> safeQuantitiesList = new ArrayList<>();

    private List<Food> displayFoods = new ArrayList<>(); // Mảng hiển thị (dùng khi search)
    private List<Double> displayQuantities = new ArrayList<>();

    private String oldMealId, targetDate, targetMealType, userId, username;
    private double targetKcal;

    private List<String> restrictedIngredientIds = new ArrayList<>();
    private List<String> userAllergiesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TÁI SỬ DỤNG GIAO DIỆN TÌM KIẾM
        setContentView(R.layout.activity_meal_search);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = pref.getString("KEY_USER_ID", "");
        username = pref.getString("KEY_USER", "");

        oldMealId = getIntent().getStringExtra("EXTRA_OLD_MEAL_ID");
        targetDate = getIntent().getStringExtra("EXTRA_TARGET_DATE");
        targetMealType = getIntent().getStringExtra("EXTRA_MEAL_TYPE");
        targetKcal = getIntent().getDoubleExtra("EXTRA_TARGET_KCAL", 0);

        initViews();
        loadRestrictedIngredientsThenFoods();
    }

    private void initViews() {
        btnBackSearch = findViewById(R.id.btnBackSearch);
        tvSearchTitle = findViewById(R.id.tvSearchTitle);
        edtSearchFood = findViewById(R.id.edtSearchFood);
        rvFoodSearch = findViewById(R.id.rvFoodSearch);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);

        rvFoodSearch.setLayoutManager(new LinearLayoutManager(this));
        btnBackSearch.setOnClickListener(v -> finish());

        // Cập nhật giao diện riêng cho Swap
        tvSearchTitle.setText("Gợi ý thay thế (~" + Math.round(targetKcal) + " Kcal)");
        btnSaveMeal.setVisibility(View.GONE); // Ẩn nút Lưu vì Đổi món bấm vào là đổi ngay luôn!

        // Sự kiện gõ tìm kiếm
        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchFoodsLocally(s.toString());
            }
        });
    }

    // TẢI DỊ ỨNG (Giữ nguyên logic cực xịn của bạn)
    private void loadRestrictedIngredientsThenFoods() {
        if (username == null || username.isEmpty()) { loadAllFoods(); return; }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*, condition_restricted_ingredients(*)))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                restrictedIngredientIds.clear(); userAllergiesList.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);
                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                String type = mc.getType();
                                if (type != null && (type.toLowerCase().contains("allergy") || type.toLowerCase().contains("dị ứng"))) {
                                    userAllergiesList.add(mc.getName().toLowerCase());
                                }
                                if (mc.getRestrictedIngredients() != null) {
                                    for (com.hcmute.edu.vn.model.ConditionRestrictedIngredient cri : mc.getRestrictedIngredients()) {
                                        if (cri.getIngredientId() != null) restrictedIngredientIds.add(cri.getIngredientId());
                                    }
                                }
                            }
                        }
                    }
                }
                loadAllFoods();
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) { loadAllFoods(); }
        });
    }

    // TẢI VÀ LỌC MÓN THEO CẢ DỊ ỨNG & CALO (SWAP LOGIC)
    private void loadAllFoods() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        java.util.Map<String, String> queryMap = new java.util.HashMap<>();
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> allFoodsFetched = response.body();
                    safeFoodsList.clear();
                    safeQuantitiesList.clear();

                    for (Food food : allFoodsFetched) {
                        boolean isSafe = true;

                        // 1. Lọc dị ứng bằng Tên
                        if (food.getName() != null) {
                            String foodName = food.getName().toLowerCase();
                            for (String allergyName : userAllergiesList) {
                                if (foodName.contains(allergyName) || allergyName.contains(foodName)) { isSafe = false; break; }
                            }
                        }

                        // 2. Lọc dị ứng bằng Nguyên liệu
                        if (isSafe && food.getFoodIngredients() != null) {
                            for (com.hcmute.edu.vn.model.FoodIngredient fi : food.getFoodIngredients()) {
                                if (fi.getIngredientId() != null && restrictedIngredientIds.contains(fi.getIngredientId())) {
                                    isSafe = false; break;
                                }
                            }
                        }

                        // 3. THUẬT TOÁN SWAP: Nếu an toàn -> Tính toán hệ số phần ăn
                        if (isSafe) {
                            double baseCalo = food.getCalories() > 0 ? food.getCalories() : 1;
                            double newMultiplier = Math.round((targetKcal / baseCalo) * 2) / 2.0; // Làm tròn 0.5

                            // Chỉ gợi ý nếu ăn từ 0.5 đến 4.0 phần
                            if (newMultiplier >= 0.5 && newMultiplier <= 4.0) {
                                double actualNewCalo = newMultiplier * baseCalo;
                                // Calo mới phải ngang ngửa Calo cũ (Chênh lệch tối đa 50 kcal)
                                if (Math.abs(actualNewCalo - targetKcal) <= 50) {
                                    safeFoodsList.add(food);
                                    safeQuantitiesList.add(newMultiplier);
                                }
                            }
                        }
                    }
                    searchFoodsLocally(""); // Load dữ liệu ra RecyclerView
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {}
        });
    }

    private void searchFoodsLocally(String query) {
        displayFoods.clear();
        displayQuantities.clear();

        if (query.isEmpty()) {
            displayFoods.addAll(safeFoodsList);
            displayQuantities.addAll(safeQuantitiesList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (int i = 0; i < safeFoodsList.size(); i++) {
                if (safeFoodsList.get(i).getName().toLowerCase().contains(lowerCaseQuery)) {
                    displayFoods.add(safeFoodsList.get(i));
                    displayQuantities.add(safeQuantitiesList.get(i));
                }
            }
        }
        setupAdapter();
    }

    private void setupAdapter() {
        adapter = new FoodSwapAdapter(this, displayFoods, displayQuantities, (selectedFood, quantity) -> {
            // =======================================================
            // KHI USER CLICK CHỌN MÓN ĐỂ SWAP -> LƯU VÀO DB VÀ QUAY VỀ
            // =======================================================
            Toast.makeText(this, "Đang đổi món...", Toast.LENGTH_SHORT).show();
            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

            // Xóa món cũ
            apiService.deleteDailyMeal("eq." + oldMealId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    // Lưu món mới
                    UserDailyMeal newMeal = new UserDailyMeal(userId, targetDate, targetMealType, selectedFood.getId(), quantity);
                    apiService.addDailyMeal(newMeal).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            setResult(RESULT_OK); // Báo cho NutritionActivity biết là đã đổi thành công
                            finish(); // Đóng màn hình Swap
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) { finish(); }
                    });
                }
                @Override public void onFailure(Call<Void> call, Throwable t) { finish(); }
            });
        });
        rvFoodSearch.setAdapter(adapter);
    }
}