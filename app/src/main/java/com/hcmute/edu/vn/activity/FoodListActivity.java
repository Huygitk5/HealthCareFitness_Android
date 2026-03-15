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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodVerticalAdapter;
import com.hcmute.edu.vn.model.Food;

import java.util.ArrayList;
import java.util.List;

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

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        // 1. Nhận Tiêu đề và CategoryID từ NutritionActivity truyền sang
        String title = getIntent().getStringExtra("CATEGORY_TITLE");
        int categoryId = getIntent().getIntExtra("CATEGORY_ID", 1);

        if (title != null) {
            tvFoodListTitle.setText(title);
        }

        // 2. Tải dữ liệu tương ứng với CategoryID
        loadData(categoryId);

        // 3. Khởi tạo RecyclerView
        setupRecyclerView();

        // 4. Xử lý các sự kiện (Click back, Gõ tìm kiếm...)
        setupListeners();
    }

    private void initViews() {
        btnBackFromList = findViewById(R.id.btnBackFromList);
        tvFoodListTitle = findViewById(R.id.tvFoodListTitle);
        edtSearchFood = findViewById(R.id.edtSearchFood);
        rvFullFoodList = findViewById(R.id.rvFullFoodList);
    }

    private void loadData(int categoryId) {
        allFoods = new ArrayList<>();

        // GIẢ LẬP DỮ LIỆU: Nếu sau này có Database, bạn sẽ query dựa vào categoryId ở đây
        if (categoryId == 1) {
            // Danh sách Thịt & Đạm
            allFoods.add(new Food("1", "Trứng Luộc", 1, "2 quả", 155.0, 13.0, 1.0, 0.0, 11.0));
            allFoods.add(new Food("2", "Sữa Tươi", 1, "1 ly", 120.0, 8.0, 12.0, 0.0, 4.0));
            allFoods.add(new Food("3", "Xúc Xích", 1, "1 cây", 250.0, 10.0, 2.0, 0.0, 20.0));
            allFoods.add(new Food("4", "Bơ Đậu Phộng", 1, "2 thìa", 190.0, 7.0, 6.0, 2.0, 16.0));
            allFoods.add(new Food("13", "Ức Gà", 1, "200g", 330.0, 62.0, 0.0, 0.0, 7.2));
            allFoods.add(new Food("14", "Thịt Lợn Luộc", 1, "150g", 360.0, 30.0, 0.0, 0.0, 25.0));
            allFoods.add(new Food("15", "Tôm Hấp", 1, "150g", 150.0, 30.0, 0.0, 0.0, 2.0));
            allFoods.add(new Food("25", "Cá Hồi", 1, "150g", 312.0, 30.0, 0.0, 0.0, 20.0));
        } else if (categoryId == 2) {
            // Danh sách Rau củ
            allFoods.add(new Food("5", "Súp Lơ Xanh", 2, "100g", 34.0, 2.8, 7.0, 2.6, 0.4));
            allFoods.add(new Food("6", "Cà Rốt Luộc", 2, "1 củ", 41.0, 0.9, 10.0, 2.8, 0.2));
            allFoods.add(new Food("17", "Cà Chua Bi", 2, "10 quả", 30.0, 1.0, 6.0, 1.5, 0.2));
            allFoods.add(new Food("18", "Bắp Cải Luộc", 2, "100g", 25.0, 1.3, 5.8, 2.5, 0.1));
            allFoods.add(new Food("29", "Cần Tây", 2, "100g", 14.0, 0.7, 3.0, 1.6, 0.2));
        } else {
            // Danh sách Tinh bột
            allFoods.add(new Food("9", "Bánh Phở", 3, "1 bát", 200.0, 4.0, 45.0, 1.0, 0.5));
            allFoods.add(new Food("10", "Chuối", 3, "1 quả", 105.0, 1.3, 27.0, 3.1, 0.3));
            allFoods.add(new Food("21", "Gạo Lứt", 3, "1 bát", 216.0, 5.0, 45.0, 3.5, 1.8));
            allFoods.add(new Food("22", "Khoai Tây", 3, "1 củ", 161.0, 4.3, 37.0, 3.8, 0.2));
            allFoods.add(new Food("33", "Táo", 3, "1 quả", 95.0, 0.5, 25.0, 4.4, 0.3));
        }

        // Khởi tạo danh sách hiển thị bằng danh sách gốc
        displayFoods = new ArrayList<>(allFoods);
    }

    private void setupRecyclerView() {
        // Truyền displayFoods vào Adapter. Mỗi khi user chọn 1 món, sẽ Toast số lượng lên
        adapter = new FoodVerticalAdapter(displayFoods, selectedCount -> {
            // Khi làm thật, bạn có thể lưu List đồ ăn này lại để mang về màn hình trước
            // Toast.makeText(this, "Đã chọn: " + selectedCount + " món", Toast.LENGTH_SHORT).show();
        });

        rvFullFoodList.setLayoutManager(new LinearLayoutManager(this));
        rvFullFoodList.setAdapter(adapter);
    }

    private void setupListeners() {
        // Nút Back
        btnBackFromList.setOnClickListener(v -> {
            // Kiểm tra xem user có chọn món nào không
            if (!adapter.getSelectedFoods().isEmpty()) {
                Food selected = adapter.getSelectedFoods().get(0);

                // Gói dữ liệu gửi về màn hình trước
                Intent resultIntent = new Intent();
                resultIntent.putExtra("CATEGORY_TITLE", getIntent().getStringExtra("CATEGORY_TITLE"));
                resultIntent.putExtra("FOOD_NAME", selected.getName());
                resultIntent.putExtra("FOOD_CAL", selected.getCalories());
                resultIntent.putExtra("FOOD_P", selected.getProteinG());
                resultIntent.putExtra("FOOD_C", selected.getCarbG());
                resultIntent.putExtra("FOOD_F", selected.getFatG());

                setResult(RESULT_OK, resultIntent); // Xác nhận gửi thành công
            }
            finish(); // Đóng màn hình
        });

        // Thanh Tìm Kiếm (Real-time Filter)
        edtSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Mỗi khi gõ 1 chữ, lập tức lọc danh sách
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
                if (food.getName().toLowerCase().contains(lowerCaseQuery)) {
                    displayFoods.add(food);
                }
            }
        }

        // ĐÃ SỬA LỖI Ở ĐÂY: Dùng notifyDataSetChanged()
        adapter.notifyDataSetChanged();
    }
}