package com.hcmute.edu.vn.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.hcmute.edu.vn.adapter.FoodVerticalAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.FoodIngredient;
import com.hcmute.edu.vn.model.UserDailyMeal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealSearchActivity extends AppCompatActivity {

    private ImageButton btnBackSearch;
    private TextView tvSearchTitle;
    private EditText edtSearchFood;
    private RecyclerView rvFoodSearch;
    private MaterialButton btnSaveMeal;
    private FoodVerticalAdapter adapter;
    private List<Food> foodList = new ArrayList<>();
    private String targetDate;
    private String targetMealType;
    private String userId;
    private String username;
    private List<String> userAllergiesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_search);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = pref.getString("KEY_USER_ID", "");
        username = pref.getString("KEY_USER", "");

        targetDate = getIntent().getStringExtra("EXTRA_DATE");
        targetMealType = getIntent().getStringExtra("EXTRA_MEAL_TYPE");

        if (getIntent().hasExtra("EXTRA_ALLERGIES")) {
            userAllergiesList = getIntent().getStringArrayListExtra("EXTRA_ALLERGIES");
        }
        if (userAllergiesList == null) {
            userAllergiesList = new ArrayList<>();
        }

        initViews();
        setupUI();

        loadAllFoods();
    }

    private void initViews() {
        btnBackSearch = findViewById(R.id.btnBackSearch);
        tvSearchTitle = findViewById(R.id.tvSearchTitle);
        edtSearchFood = findViewById(R.id.edtSearchFood);
        rvFoodSearch = findViewById(R.id.rvFoodSearch);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);

        rvFoodSearch.setLayoutManager(new LinearLayoutManager(this));
        btnBackSearch.setOnClickListener(v -> finish());
    }

    private void setupUI() {
        String mealName = "Bữa ăn";
        if ("BREAKFAST".equals(targetMealType)) mealName = "Bữa Sáng";
        else if ("LUNCH".equals(targetMealType)) mealName = "Bữa Trưa";
        else if ("DINNER".equals(targetMealType)) mealName = "Bữa Tối";
        tvSearchTitle.setText("Thêm món " + mealName);

        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchFoodsLocally(s.toString());
            }
        });

        btnSaveMeal.setOnClickListener(v -> saveSelectedMealsToDatabase());
    }

    private void loadAllFoods() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        java.util.Map<String, String> queryMap = new java.util.HashMap<>();
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> allFoodsFetched = response.body();
                    foodList.clear();

                    int soMonBiLoai = 0;

                    for (Food food : allFoodsFetched) {
                        boolean isSafe = true;

                        // LỌC 1: KIỂM TRA TÊN MÓN ĂN
                        if (food.getName() != null) {
                            String foodName = food.getName().toLowerCase();
                            for (String allergyName : userAllergiesList) {
                                if (allergyName != null && !allergyName.isEmpty()) {
                                    String keyword = allergyName.toLowerCase().replace("dị ứng", "").replace("allergy", "").trim();

                                    if (!keyword.isEmpty() && foodName.contains(keyword)) {
                                        isSafe = false;
                                        android.util.Log.d("LOC_MON_AN", "❌ LOẠI: [" + food.getName() + "] vì tên chứa: " + keyword);
                                        break;
                                    }
                                }
                            }
                        }

                        // LỌC 2: KIỂM TRA THÀNH PHẦN NGUYÊN LIỆU (Nếu API có trả về)
                        if (isSafe && food.getFoodIngredients() != null) {
                            for (FoodIngredient fi : food.getFoodIngredients()) {
                                if (fi.getIngredient() != null && fi.getIngredient().getName() != null) {
                                    String ingName = fi.getIngredient().getName().toLowerCase();
                                    for (String allergyName : userAllergiesList) {
                                        if (allergyName != null && !allergyName.isEmpty()) {
                                            String keyword = allergyName.toLowerCase().replace("dị ứng", "").replace("allergy", "").trim();

                                            if (!keyword.isEmpty() && ingName.contains(keyword)) {
                                                isSafe = false;
                                                android.util.Log.d("LOC_MON_AN", "❌ LOẠI: [" + food.getName() + "] vì có nguyên liệu: " + keyword);
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (!isSafe) break;
                            }
                        }

                        // Nếu an toàn -> Thêm vào danh sách hiển thị
                        if (isSafe) {
                            foodList.add(food);
                        } else {
                            soMonBiLoai++;
                        }
                    }

                    android.util.Log.d("LOC_MON_AN", "✅ ĐÃ LỌC XONG! Tổng số món an toàn: " + foodList.size() + " | Bị giấu đi: " + soMonBiLoai);
                    setupAdapter(foodList);
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {
                Toast.makeText(MealSearchActivity.this, "Lỗi tải món ăn!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchFoodsLocally(String query) {
        List<Food> filteredList = new ArrayList<>();
        for (Food food : foodList) {
            if (food.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(food);
            }
        }
        setupAdapter(filteredList);
    }

    private void setupAdapter(List<Food> displayList) {
        adapter = new FoodVerticalAdapter(displayList, selectedCount -> {
            btnSaveMeal.setText("LƯU VÀO BỮA ĂN (" + selectedCount + ")");
        });
        rvFoodSearch.setAdapter(adapter);
    }

    private void saveSelectedMealsToDatabase() {
        if (adapter == null) return;

        // 1. Lấy dữ liệu dạng Map (Món ăn -> Số phần)
        Map<Food, Double> selectedFoodsMap = adapter.getSelectedFoodsMap();

        if (selectedFoodsMap.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 món ăn!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "LỖI NẶNG: Không tìm thấy User ID. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
            return;
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        int totalFoods = selectedFoodsMap.size();
        int[] completedCount = {0};

        // 2. Lặp qua Map để lấy Món ăn và Số lượng phần ăn
        for (Map.Entry<Food, Double> entry : selectedFoodsMap.entrySet()) {
            Food selectedFood = entry.getKey();
            Double quantity = entry.getValue();

            // 3. Tạo Object truyền lên Supabase với số lượng (quantity) chuẩn
            UserDailyMeal newMeal = new UserDailyMeal(
                    userId,
                    targetDate,
                    targetMealType,
                    selectedFood.getId(),
                    quantity // Truyền số lượng thực tế user đã chọn (0.5, 1.0, 1.5...)
            );

            // 4. Bắn API lưu
            apiService.addDailyMeal(newMeal).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    completedCount[0]++;
                    if (!response.isSuccessful()) {
                        try {
                            String errorDetails = response.errorBody().string();
                            android.util.Log.e("SUPABASE_ERROR", "Lỗi chi tiết: " + errorDetails);
                        } catch (Exception e) {}
                    }
                    checkCompletionAndFinish(completedCount[0], totalFoods);
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    completedCount[0]++;
                    checkCompletionAndFinish(completedCount[0], totalFoods);
                }
            });
        }
    }

    private void checkCompletionAndFinish(int currentCount, int totalFoods) {
        if (currentCount == totalFoods) {
            Toast.makeText(MealSearchActivity.this, "Đã xử lý xong " + totalFoods + " món!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
    }
}