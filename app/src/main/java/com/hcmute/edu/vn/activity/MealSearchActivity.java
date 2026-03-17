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
import com.hcmute.edu.vn.model.ConditionRestrictedIngredient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.FoodIngredient;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyMeal;
import com.hcmute.edu.vn.model.UserMedicalCondition;

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
    private String username;

    // ĐÃ ĐỔI: Chứa danh sách các ID của Nguyên liệu cần tránh (Chính xác tuyệt đối)
    private List<String> restrictedIngredientIds = new ArrayList<>();
    private List<String> userAllergiesList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_search);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = pref.getString("KEY_USER_ID", "");
        username = pref.getString("KEY_USER", "");

        targetDate = getIntent().getStringExtra("EXTRA_DATE");
        targetMealType = getIntent().getStringExtra("EXTRA_MEAL_TYPE");

        initViews();
        setupUI();
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

    // ==============================================================
    // BƯỚC 1: LẤY DANH SÁCH ID NGUYÊN LIỆU BỊ CẤM TỪ DATABASE
    // ==============================================================
    // ==============================================================
    // BƯỚC 1: TẢI DANH SÁCH DỊ ỨNG (CÓ GẮN LOG THEO DÕI)
    // ==============================================================
    private void loadRestrictedIngredientsThenFoods() {
        if (username == null || username.isEmpty()) {
            loadAllFoods();
            return;
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*, condition_restricted_ingredients(*)))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                restrictedIngredientIds.clear();
                userAllergiesList.clear(); // Danh sách tên để dự phòng

                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);

                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                // 1. Lưu TÊN dị ứng (Để lọc dự phòng)
                                String type = mc.getType();
                                if (type != null && (type.toLowerCase().contains("allergy") || type.toLowerCase().contains("dị ứng"))) {
                                    userAllergiesList.add(mc.getName().toLowerCase());
                                }

                                // 2. Lưu ID nguyên liệu cấm
                                if (mc.getRestrictedIngredients() != null) {
                                    for (com.hcmute.edu.vn.model.ConditionRestrictedIngredient cri : mc.getRestrictedIngredients()) {
                                        if (cri.getIngredientId() != null) {
                                            restrictedIngredientIds.add(cri.getIngredientId());
                                        }
                                    }
                                } else {
                                    android.util.Log.e("LOC_MON_AN", "CẢNH BÁO: Bệnh '" + mc.getName() + "' trả về restrictedIngredients = NULL (Xem lại Model MedicalCondition)");
                                }
                            }
                        }
                    }
                }

                android.util.Log.d("LOC_MON_AN", "TỔNG SỐ ID BỊ CẤM TÌM THẤY: " + restrictedIngredientIds.size());
                android.util.Log.d("LOC_MON_AN", "TỔNG SỐ TÊN BỊ CẤM TÌM THẤY: " + userAllergiesList.size());

                loadAllFoods(); // Xong xuôi thì qua tải Đồ ăn
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                loadAllFoods();
            }
        });
    }

    // ==============================================================
    // BƯỚC 2: TẢI ĐỒ ĂN VÀ LỌC BẰNG CẢ ID LẪN TÊN (KHÔNG THỂ LỌT LƯỚI)
    // ==============================================================
    private void loadAllFoods() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        java.util.Map<String, String> queryMap = new java.util.HashMap<>();
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))"); // Kéo theo cả tên nguyên liệu để dự phòng

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Food> allFoodsFetched = response.body();
                    foodList.clear();

                    int soMonBiLoai = 0;

                    for (Food food : allFoodsFetched) {
                        boolean isSafe = true;

                        // LỌC 1: KIỂM TRA TÊN MÓN ĂN (Dự phòng)
                        if (food.getName() != null) {
                            String foodName = food.getName().toLowerCase();
                            for (String allergyName : userAllergiesList) {
                                if (foodName.contains(allergyName) || allergyName.contains(foodName)) {
                                    isSafe = false;
                                    android.util.Log.d("LOC_MON_AN", "❌ LOẠI: [" + food.getName() + "] vì tên trùng dị ứng: " + allergyName);
                                    break;
                                }
                            }
                        }

                        // LỌC 2: KIỂM TRA BẰNG BẢNG ID VÀ CÔNG THỨC MÓN
                        if (isSafe && food.getFoodIngredients() != null) {
                            for (com.hcmute.edu.vn.model.FoodIngredient fi : food.getFoodIngredients()) {

                                // Quét bằng ID nguyên liệu (Chính xác tuyệt đối)
                                if (fi.getIngredientId() != null && restrictedIngredientIds.contains(fi.getIngredientId())) {
                                    isSafe = false;
                                    android.util.Log.d("LOC_MON_AN", "❌ LOẠI: [" + food.getName() + "] vì chứa ID nguyên liệu cấm: " + fi.getIngredientId());
                                    break;
                                }

                                // Quét dự phòng bằng TÊN nguyên liệu (Phòng khi Database ID chưa nối)
                                if (fi.getIngredient() != null && fi.getIngredient().getName() != null) {
                                    String ingName = fi.getIngredient().getName().toLowerCase();
                                    for (String allergyName : userAllergiesList) {
                                        if (ingName.contains(allergyName) || allergyName.contains(ingName)) {
                                            isSafe = false;
                                            android.util.Log.d("LOC_MON_AN", "❌ LOẠI: [" + food.getName() + "] vì tên nguyên liệu '" + ingName + "' trùng dị ứng: " + allergyName);
                                            break;
                                        }
                                    }
                                }
                                if (!isSafe) break;
                            }
                        } else if (food.getFoodIngredients() == null) {
                            android.util.Log.e("LOC_MON_AN", "CẢNH BÁO: Món [" + food.getName() + "] trả về foodIngredients = NULL (Xem lại Model Food)");
                        }

                        // Nếu an toàn -> Thêm vào danh sách hiển thị
                        if (isSafe) {
                            foodList.add(food);
                        } else {
                            soMonBiLoai++;
                        }
                    }

                    android.util.Log.d("LOC_MON_AN", "ĐÃ LỌC XONG! Tổng số món hiển thị: " + foodList.size() + " | Số món bị giấu đi: " + soMonBiLoai);
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

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "LỖI NẶNG: Không tìm thấy User ID. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
            return;
        }

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        int totalFoods = selectedFoods.size();
        int[] completedCount = {0};

        for (Food selectedFood : selectedFoods) {
            UserDailyMeal newMeal = new UserDailyMeal(
                    userId,
                    targetDate,
                    targetMealType,
                    selectedFood.getId(),
                    1.0
            );

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