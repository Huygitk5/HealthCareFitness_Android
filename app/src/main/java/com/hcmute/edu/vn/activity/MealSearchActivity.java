package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodVerticalAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.UserDailyMeal;

import java.util.ArrayList;
import java.util.HashMap;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_search);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        // Lấy ID của user đang đăng nhập
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = pref.getString("KEY_USER_ID", "");

        // Nhận dữ liệu Ngày và Bữa ăn từ NutritionActivity gửi sang
        targetDate = getIntent().getStringExtra("EXTRA_DATE");
        targetMealType = getIntent().getStringExtra("EXTRA_MEAL_TYPE");

        initViews();
        setupUI();
        loadAllFoods(); // Mở lên là load hết đồ ăn cho user chọn
    }

    private void initViews() {
        btnBackSearch = findViewById(R.id.btnBackSearch);
        tvSearchTitle = findViewById(R.id.tvSearchTitle);
        edtSearchFood = findViewById(R.id.edtSearchFood);
        rvFoodSearch = findViewById(R.id.rvFoodSearch);
        btnSaveMeal = findViewById(R.id.btnSaveMeal);

        rvFoodSearch.setLayoutManager(new LinearLayoutManager(this));

        // Nút Back
        btnBackSearch.setOnClickListener(v -> finish());
    }

    private void setupUI() {
        // Cập nhật tiêu đề theo bữa ăn
        String mealName = "Bữa ăn";
        if ("BREAKFAST".equals(targetMealType)) mealName = "Bữa Sáng";
        else if ("LUNCH".equals(targetMealType)) mealName = "Bữa Trưa";
        else if ("DINNER".equals(targetMealType)) mealName = "Bữa Tối";
        tvSearchTitle.setText("Thêm món " + mealName);

        // Tìm kiếm Realtime
        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                searchFoodsLocally(s.toString());
            }
        });

        // Nút Lưu lên Database
        btnSaveMeal.setOnClickListener(v -> saveSelectedMealsToDatabase());
    }

    private void loadAllFoods() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        // Gọi API lấy toàn bộ thức ăn (có thể giới hạn nếu list quá dài)
        apiService.searchFoods(new HashMap<>()).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    foodList = response.body();
                    setupAdapter(foodList);
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {}
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
            // Cập nhật số lượng món ăn đã chọn lên nút LƯU
            btnSaveMeal.setText("LƯU VÀO BỮA ĂN (" + selectedCount + ")");
        });
        rvFoodSearch.setAdapter(adapter);
    }

    private void saveSelectedMealsToDatabase() {
        if (adapter == null) return;
        List<Food> selectedFoods = adapter.getSelectedFoods();

        if (selectedFoods.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 món ăn!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. KIỂM TRA XEM USER ID CÓ BỊ RỖNG KHÔNG
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "LỖI NẶNG: Không tìm thấy User ID. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
            return;
        }

        Food selectedFood = selectedFoods.get(0);

        UserDailyMeal newMeal = new UserDailyMeal(
                userId,
                targetDate,
                targetMealType,
                selectedFood.getId(),
                1.0
        );

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.addDailyMeal(newMeal).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MealSearchActivity.this, "Đã thêm thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    // 2. IN RA LỖI CHI TIẾT TỪ SUPABASE
                    try {
                        String errorDetails = response.errorBody().string();
                        android.util.Log.e("SUPABASE_ERROR", "Lỗi chi tiết: " + errorDetails);
                        Toast.makeText(MealSearchActivity.this, "Lỗi Database: " + errorDetails, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(MealSearchActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}