package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodVerticalAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodListActivity extends AppCompatActivity {

    private ImageView btnBackFromList;
    private TextView tvFoodListTitle;
    private EditText edtSearchFood;
    private RecyclerView rvFullFoodList;

    private FoodVerticalAdapter adapter;
    private List<Food> allFoods; // Danh sách gốc chứa toàn bộ món ăn
    private List<Food> displayFoods; // Danh sách dùng để hiển thị (thay đổi khi Search)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        // 1. Nhận Tiêu đề và CategoryID từ NutritionActivity truyền sang
        String title = getIntent().getStringExtra("CATEGORY_TITLE");
        int categoryId = getIntent().getIntExtra("CATEGORY_ID", 1);

        if (title != null) {
            tvFoodListTitle.setText(title);
        }

        // 2. Khởi tạo RecyclerView TRƯỚC VỚI DANH SÁCH RỖNG để tránh lỗi giao diện
        setupRecyclerView();

        // 3. Tải dữ liệu thật từ API tương ứng với CategoryID
        loadDataFromApi(categoryId);

        // 4. Xử lý các sự kiện (Click back, Gõ tìm kiếm...)
        setupListeners();
    }

    private void initViews() {
        btnBackFromList = findViewById(R.id.btnBackFromList);
        tvFoodListTitle = findViewById(R.id.tvFoodListTitle);
        edtSearchFood = findViewById(R.id.edtSearchFood);
        rvFullFoodList = findViewById(R.id.rvFullFoodList);

        allFoods = new ArrayList<>();
        displayFoods = new ArrayList<>();
    }

    // =======================================================
    // HÀM LẤY DỮ LIỆU TỪ SUPABASE BẰNG API THẬT
    // =======================================================
    private void loadDataFromApi(int categoryId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // Định dạng eqCategoryId: "eq.1", "eq.2",...
        String eqCategoryId = "eq." + categoryId;

        apiService.getFoodsByCategory(eqCategoryId, "*").enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allFoods.clear();
                    allFoods.addAll(response.body());

                    displayFoods.clear();
                    displayFoods.addAll(allFoods);
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(FoodListActivity.this, "Không tìm thấy món ăn nào!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Food>> call, Throwable t) {
                Toast.makeText(FoodListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new FoodVerticalAdapter(displayFoods, selectedCount -> {});

        rvFullFoodList.setLayoutManager(new LinearLayoutManager(this));
        rvFullFoodList.setAdapter(adapter);
    }

    private void setupListeners() {
        // Nút Back
        btnBackFromList.setOnClickListener(v -> {

            java.util.Map<Food, Double> selectedMap = adapter.getSelectedFoodsMap();

            // Kiểm tra xem user có chọn món nào không
            if (selectedMap != null && !selectedMap.isEmpty()) {

                // Lấy món ăn đầu tiên mà user đã chọn trong Map ra
                Food selected = selectedMap.keySet().iterator().next();

                // Gói dữ liệu gửi về màn hình trước
                Intent resultIntent = new Intent();
                resultIntent.putExtra("CATEGORY_TITLE", getIntent().getStringExtra("CATEGORY_TITLE"));
                resultIntent.putExtra("FOOD_NAME", selected.getName());

                // Lấy thông số Macro
                resultIntent.putExtra("FOOD_CAL", selected.getCalories() != null ? selected.getCalories() : 0.0);
                resultIntent.putExtra("FOOD_P", selected.getProteinG() != null ? selected.getProteinG() : 0.0);
                resultIntent.putExtra("FOOD_C", selected.getCarbG() != null ? selected.getCarbG() : 0.0);
                resultIntent.putExtra("FOOD_F", selected.getFatG() != null ? selected.getFatG() : 0.0);
                resultIntent.putExtra("FOOD_IMAGE", selected.getImageUrl());

                setResult(RESULT_OK, resultIntent); // Xác nhận gửi thành công
            }
            finish();
        });

        // Thanh Tìm Kiếm (Real-time Filter)
        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFood(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Hàm Lọc Món Ăn
    private void filterFood(String query) {
        displayFoods.clear();
        if (query.isEmpty()) {
            displayFoods.addAll(allFoods); // Nếu xóa hết chữ, hiển thị lại toàn bộ
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Food food : allFoods) {
                // Nếu tên món ăn có chứa từ khóa đang gõ thì thêm vào danh sách hiển thị
                if (food.getName() != null && food.getName().toLowerCase().contains(lowerCaseQuery)) {
                    displayFoods.add(food);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }
}