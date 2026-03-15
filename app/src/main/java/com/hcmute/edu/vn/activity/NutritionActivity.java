package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.FoodAdapter;
import com.hcmute.edu.vn.model.Food;

import java.util.ArrayList;
import java.util.List;

public class NutritionActivity extends AppCompatActivity {

    RecyclerView rvBreakfastMeat, rvBreakfastVeggie, rvBreakfastCarb;
    RecyclerView rvLunchMeat, rvLunchVeggie, rvLunchCarb;
    RecyclerView rvDinnerMeat, rvDinnerVeggie, rvDinnerCarb;

    // Các thành phần của Dashboard
    CircularProgressIndicator progressCalories;
    LinearProgressIndicator progressCarb, progressProtein, progressFat;
    TextView tvTotalCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;

    // Biến lưu trữ món ăn ĐANG ĐƯỢC CHỌN của 9 danh sách
    Food selBfMeat, selBfVeggie, selBfCarb;
    Food selLunchMeat, selLunchVeggie, selLunchCarb;
    Food selDinnerMeat, selDinnerVeggie, selDinnerCarb;

    // Mục tiêu (Target) trong ngày
    final double TARGET_CALORIES = 2000.0;
    final double TARGET_CARB = 250.0;
    final double TARGET_PROTEIN = 120.0;
    final double TARGET_FAT = 65.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nutrition);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();
        setupBreakfastData();
        setupLunchData();
        setupDinnerData();
        setupBottomNavigation();

        // Tính toán tổng số ngay lần đầu tiên mở màn hình
        calculateAndDisplayTotals();
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

        // Ánh xạ Dashboard
        progressCalories = findViewById(R.id.progressCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);

        progressCarb = findViewById(R.id.progressCarb);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);

        progressProtein = findViewById(R.id.progressProtein);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);

        progressFat = findViewById(R.id.progressFat);
        tvTotalFat = findViewById(R.id.tvTotalFat);
    }

    // ===============================================
    // HÀM TÍNH TỔNG VÀ CẬP NHẬT DASHBOARD
    // ===============================================
    private void calculateAndDisplayTotals() {
        double totalCal = 0, totalCarb = 0, totalProtein = 0, totalFat = 0;

        // Gom tất cả món đang chọn vào mảng để duyệt cho lẹ
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

        // Cập nhật chữ (TextView)
        tvTotalCalories.setText(String.valueOf(Math.round(totalCal)));
        tvTotalCarb.setText(Math.round(totalCarb) + "g/" + Math.round(TARGET_CARB) + "g");
        tvTotalProtein.setText(Math.round(totalProtein) + "g/" + Math.round(TARGET_PROTEIN) + "g");
        tvTotalFat.setText(Math.round(totalFat) + "g/" + Math.round(TARGET_FAT) + "g");

        // Cập nhật thanh chạy (ProgressBar) với hiệu ứng chạy mượt mà (Animation)
        int progCal = (int) ((totalCal / TARGET_CALORIES) * 100);
        int progCarb = (int) ((totalCarb / TARGET_CARB) * 100);
        int progPro = (int) ((totalProtein / TARGET_PROTEIN) * 100);
        int progFat = (int) ((totalFat / TARGET_FAT) * 100);

        animateProgress(progressCalories, progCal);
        animateProgress(progressCarb, progCarb);
        animateProgress(progressProtein, progPro);
        animateProgress(progressFat, progFat);
    }

    // Hàm giúp thanh bar chạy êm ái thay vì giật cục
    private void animateProgress(android.widget.ProgressBar progressBar, int progress) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.getProgress(), progress);
        animation.setDuration(500); // Nửa giây
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    // ===============================================
    // HÀM HELPER KHỞI TẠO RECYCLERVIEW & LẮNG NGHE CHỌN MÓN
    // ===============================================
    public interface OnFoodUpdate {
        void onUpdate(Food food);
    }

    private void setupRecyclerView(RecyclerView recyclerView, List<Food> foodList, OnFoodUpdate callback) {
        FoodAdapter adapter = new FoodAdapter(this, foodList, food -> {
            // Khi user click chọn món khác -> Lưu lại và Tính lại ngay lập tức
            callback.onUpdate(food);
            calculateAndDisplayTotals();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Khởi tạo: Mặc định chọn món đầu tiên (vị trí 0)
        if (!foodList.isEmpty()) {
            callback.onUpdate(foodList.get(0));
        }
    }

    // ===============================================
    // DATA MẪU
    // ===============================================
    private void setupBreakfastData() {
        List<Food> bfMeat = new ArrayList<>();
        bfMeat.add(new Food("1", "Trứng Luộc", 1, "2 quả", 155.0, 13.0, 1.0, 0.0, 11.0));
        bfMeat.add(new Food("2", "Sữa Tươi", 1, "1 ly", 120.0, 8.0, 12.0, 0.0, 4.0));
        bfMeat.add(new Food("3", "Xúc Xích", 1, "1 cây", 250.0, 10.0, 2.0, 0.0, 20.0));
        bfMeat.add(new Food("4", "Bơ Đậu Phộng", 1, "2 thìa", 190.0, 7.0, 6.0, 2.0, 16.0));
        setupRecyclerView(rvBreakfastMeat, bfMeat, food -> selBfMeat = food);

        List<Food> bfVeggie = new ArrayList<>();
        bfVeggie.add(new Food("5", "Súp Lơ Xanh", 2, "100g", 34.0, 2.8, 7.0, 2.6, 0.4));
        bfVeggie.add(new Food("6", "Cà Rốt Luộc", 2, "1 củ", 41.0, 0.9, 10.0, 2.8, 0.2));
        bfVeggie.add(new Food("7", "Salad Trộn", 2, "1 bát", 50.0, 1.0, 5.0, 2.0, 3.0));
        bfVeggie.add(new Food("8", "Rau Bina", 2, "100g", 23.0, 2.9, 3.6, 2.2, 0.4));
        setupRecyclerView(rvBreakfastVeggie, bfVeggie, food -> selBfVeggie = food);

        List<Food> bfCarb = new ArrayList<>();
        bfCarb.add(new Food("9", "Bánh Phở", 3, "1 bát", 200.0, 4.0, 45.0, 1.0, 0.5));
        bfCarb.add(new Food("10", "Chuối", 3, "1 quả", 105.0, 1.3, 27.0, 3.1, 0.3));
        bfCarb.add(new Food("11", "Khoai Lang", 3, "1 củ", 112.0, 2.0, 26.0, 4.0, 0.1));
        bfCarb.add(new Food("12", "Yến Mạch", 3, "50g", 194.0, 7.0, 34.0, 5.0, 3.5));
        setupRecyclerView(rvBreakfastCarb, bfCarb, food -> selBfCarb = food);
    }

    private void setupLunchData() {
        List<Food> lunchMeat = new ArrayList<>();
        lunchMeat.add(new Food("13", "Ức Gà", 1, "200g", 330.0, 62.0, 0.0, 0.0, 7.2));
        lunchMeat.add(new Food("14", "Thịt Lợn", 1, "150g", 360.0, 30.0, 0.0, 0.0, 25.0));
        lunchMeat.add(new Food("15", "Tôm Hấp", 1, "150g", 150.0, 30.0, 0.0, 0.0, 2.0));
        lunchMeat.add(new Food("16", "Đậu Phụ", 1, "100g", 76.0, 8.0, 1.9, 0.3, 4.8));
        setupRecyclerView(rvLunchMeat, lunchMeat, food -> selLunchMeat = food);

        List<Food> lunchVeggie = new ArrayList<>();
        lunchVeggie.add(new Food("17", "Cà Chua Bi", 2, "10 quả", 30.0, 1.0, 6.0, 1.5, 0.2));
        lunchVeggie.add(new Food("18", "Bắp Cải", 2, "100g", 25.0, 1.3, 5.8, 2.5, 0.1));
        lunchVeggie.add(new Food("19", "Cải Ngọt", 2, "100g", 21.0, 2.0, 3.0, 2.0, 0.2));
        lunchVeggie.add(new Food("20", "Nấm Mỡ", 2, "100g", 22.0, 3.1, 3.3, 1.0, 0.3));
        setupRecyclerView(rvLunchVeggie, lunchVeggie, food -> selLunchVeggie = food);

        List<Food> lunchCarb = new ArrayList<>();
        lunchCarb.add(new Food("21", "Gạo Lứt", 3, "1 bát", 216.0, 5.0, 45.0, 3.5, 1.8));
        lunchCarb.add(new Food("22", "Khoai Tây", 3, "1 củ", 161.0, 4.3, 37.0, 3.8, 0.2));
        lunchCarb.add(new Food("23", "Bún Tươi", 3, "1 bát", 130.0, 1.5, 30.0, 0.5, 0.2));
        lunchCarb.add(new Food("24", "Cơm Trắng", 3, "1 bát", 205.0, 4.0, 45.0, 0.6, 0.4));
        setupRecyclerView(rvLunchCarb, lunchCarb, food -> selLunchCarb = food);
    }

    private void setupDinnerData() {
        List<Food> dinnerMeat = new ArrayList<>();
        dinnerMeat.add(new Food("25", "Cá Hồi", 1, "150g", 312.0, 30.0, 0.0, 0.0, 20.0));
        dinnerMeat.add(new Food("26", "Gà Nướng", 1, "150g", 250.0, 40.0, 0.0, 0.0, 9.0));
        dinnerMeat.add(new Food("27", "Thịt Bò", 1, "150g", 375.0, 39.0, 0.0, 0.0, 22.0));
        dinnerMeat.add(new Food("28", "Trứng Cút", 1, "10 quả", 158.0, 13.0, 0.0, 0.0, 11.0));
        setupRecyclerView(rvDinnerMeat, dinnerMeat, food -> selDinnerMeat = food);

        List<Food> dinnerVeggie = new ArrayList<>();
        dinnerVeggie.add(new Food("29", "Cần Tây", 2, "100g", 14.0, 0.7, 3.0, 1.6, 0.2));
        dinnerVeggie.add(new Food("30", "Dưa Chuột", 2, "1 quả", 16.0, 0.7, 4.0, 0.5, 0.1));
        dinnerVeggie.add(new Food("31", "Đậu Cô Ve", 2, "100g", 31.0, 1.8, 7.0, 3.4, 0.2));
        dinnerVeggie.add(new Food("32", "Rau Muống", 2, "100g", 19.0, 3.0, 3.0, 2.0, 0.2));
        setupRecyclerView(rvDinnerVeggie, dinnerVeggie, food -> selDinnerVeggie = food);

        List<Food> dinnerCarb = new ArrayList<>();
        dinnerCarb.add(new Food("33", "Táo", 3, "1 quả", 95.0, 0.5, 25.0, 4.4, 0.3));
        dinnerCarb.add(new Food("34", "Ngô Luộc", 3, "1 bắp", 86.0, 3.0, 19.0, 2.0, 1.0));
        dinnerCarb.add(new Food("35", "Cam", 3, "1 quả", 47.0, 0.9, 12.0, 2.4, 0.1));
        dinnerCarb.add(new Food("36", "Bánh Mì", 3, "1 ổ", 265.0, 9.0, 49.0, 2.0, 3.0));
        setupRecyclerView(rvDinnerCarb, dinnerCarb, food -> selDinnerCarb = food);
    }

    private void setupBottomNavigation() {
        // ... (Giữ nguyên code bottom nav cũ của bạn) ...
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