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
    private TextView tvDetailName;
    private TextView tvDetailServing;
    private TextView tvDetailCalo;
    private TextView tvDetailPro;
    private TextView tvDetailCarb;
    private TextView tvDetailFat;
    private TextView tvDetailInstructions;
    private RecyclerView rvIngredients;
    private double displayQuantity = 1.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_detail);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        displayQuantity = getIntent().getDoubleExtra("EXTRA_QUANTITY", 1.0);

        initViews();
        btnBackDetail.setOnClickListener(v -> finish());

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
        tvDetailServing = findViewById(R.id.tvDetailServing);
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

        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("id", "eq." + foodId);
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    displayFoodDetail(response.body().get(0));
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
        Glide.with(this)
                .load(food.getImageUrl())
                .placeholder(R.mipmap.ic_launcher)
                .into(imgFoodDetail);

        tvDetailName.setText(food.getName());
        tvDetailServing.setText(getQuantityText(displayQuantity) + " phần");
        tvDetailCalo.setText(String.valueOf(Math.round(getValue(food.getCalories()) * displayQuantity)));
        tvDetailPro.setText(Math.round(getValue(food.getProteinG()) * displayQuantity) + "g");
        tvDetailCarb.setText(Math.round(getValue(food.getCarbG()) * displayQuantity) + "g");
        tvDetailFat.setText(Math.round(getValue(food.getFatG()) * displayQuantity) + "g");

        if (food.getInstructions() != null && !food.getInstructions().isEmpty()) {
            tvDetailInstructions.setText(food.getInstructions());
        } else {
            tvDetailInstructions.setText("Chưa có hướng dẫn chế biến cho món ăn này.");
        }

        if (food.getFoodIngredients() != null && !food.getFoodIngredients().isEmpty()) {
            IngredientAdapter adapter = new IngredientAdapter(food.getFoodIngredients(), displayQuantity);
            rvIngredients.setAdapter(adapter);
        }
    }

    private double getValue(Double value) {
        return value != null ? value : 0.0;
    }

    private String getQuantityText(double quantity) {
        return quantity == Math.floor(quantity) ? String.valueOf((int) quantity) : String.valueOf(quantity);
    }
}
