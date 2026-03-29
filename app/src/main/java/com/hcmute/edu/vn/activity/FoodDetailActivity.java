package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.IngredientAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FoodDetailActivity extends AppCompatActivity {

    private ImageView imgFoodDetail;
    private ImageButton btnBackDetail;
    private TextView tvDetailName, tvDetailCalo, tvDetailPro, tvDetailCarb, tvDetailFat, tvDetailInstructions;
    private RecyclerView rvIngredients;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        btnBackDetail.setOnClickListener(v -> finish());

        // Nhận ID món ăn từ Adapter truyền sang
        String foodId = getIntent().getStringExtra("FOOD_ID");
        if (foodId != null && !foodId.isEmpty()) {
            loadFoodDetailFromApi(foodId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy món ăn!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        imgFoodDetail = findViewById(R.id.imgFoodDetail);
        btnBackDetail = findViewById(R.id.btnBackDetail);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailCalo = findViewById(R.id.tvDetailCalo);
        tvDetailPro = findViewById(R.id.tvDetailPro);
        tvDetailCarb = findViewById(R.id.tvDetailCarb);
        tvDetailFat = findViewById(R.id.tvDetailFat);
        tvDetailInstructions = findViewById(R.id.tvDetailInstructions);
        rvIngredients = findViewById(R.id.rvIngredients);

        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadFoodDetailFromApi(String foodId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // Chuẩn bị Filter: Tìm theo ID và bắt buộc Select "Món ăn" kèm theo "Bảng công thức"
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("id", "eq." + foodId);
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Food food = response.body().get(0);
                    displayFoodDetail(food);
                } else {
                    Toast.makeText(FoodDetailActivity.this, "Không tải được thông tin chi tiết!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Food>> call, Throwable t) {
                Toast.makeText(FoodDetailActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFoodDetail(Food food) {
        // 1. Tải ảnh và Thông tin cơ bản
        Glide.with(this).load(food.getImageUrl()).placeholder(R.mipmap.ic_launcher).into(imgFoodDetail);
        tvDetailName.setText(food.getName());
        tvDetailCalo.setText(String.valueOf(Math.round(food.getCalories() != null ? food.getCalories() : 0)));
        tvDetailPro.setText(Math.round(food.getProteinG() != null ? food.getProteinG() : 0) + "g");
        tvDetailCarb.setText(Math.round(food.getCarbG() != null ? food.getCarbG() : 0) + "g");
        tvDetailFat.setText(Math.round(food.getFatG() != null ? food.getFatG() : 0) + "g");

        // 2. Hiển thị Cách nấu
        if (food.getInstructions() != null && !food.getInstructions().isEmpty()) {
            tvDetailInstructions.setText(food.getInstructions());
        } else {
            tvDetailInstructions.setText("Chưa có hướng dẫn chế biến cho món ăn này.");
        }

        // 3. Hiển thị Danh sách Nguyên liệu vào RecyclerView
        if (food.getFoodIngredients() != null && !food.getFoodIngredients().isEmpty()) {
            IngredientAdapter adapter = new IngredientAdapter(food.getFoodIngredients());
            rvIngredients.setAdapter(adapter);
        }
    }
}