package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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

    // Biến lưu trữ món ăn ĐANG ĐƯỢC CHỌN
    Food selBfMeat, selBfVeggie, selBfCarb;
    Food selLunchMeat, selLunchVeggie, selLunchCarb;
    Food selDinnerMeat, selDinnerVeggie, selDinnerCarb;

    // ĐÃ THÊM: Lưu trữ danh sách để có thể chèn món mới vào
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

        initViews();
        setupBreakfastData();
        setupLunchData();
        setupDinnerData();
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

    // Thêm vào trong class NutritionActivity.java
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
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
        // 1. Reset màu tất cả các Tab về trạng thái chưa chọn (chữ xám, nền trong suốt)
        tabBreakfast.setBackgroundResource(android.R.color.transparent);
        tabBreakfast.setTextColor(android.graphics.Color.parseColor("#757575"));

        tabLunch.setBackgroundResource(android.R.color.transparent);
        tabLunch.setTextColor(android.graphics.Color.parseColor("#757575"));

        tabDinner.setBackgroundResource(android.R.color.transparent);
        tabDinner.setTextColor(android.graphics.Color.parseColor("#757575"));

        // 2. Ẩn tất cả các danh sách món ăn
        layoutBreakfast.setVisibility(android.view.View.GONE);
        layoutLunch.setVisibility(android.view.View.GONE);
        layoutDinner.setVisibility(android.view.View.GONE);

        // 3. Hiển thị danh sách tương ứng và tô màu Xanh cho Tab được bấm
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
        // Xóa món ăn trùng tên nếu đã tồn tại trong mảng
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getName().equals(newFood.getName())) {
                list.remove(i);
                break;
            }
        }
        // Chèn món mới vào vị trí index = 0 (Ngoài cùng bên trái)
        list.add(0, newFood);

        // Gọi lại hàm setup để Adapter tự động vẽ lại và highlight món đầu tiên
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
            calculateAndDisplayTotals();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        if (!foodList.isEmpty()) {
            callback.onUpdate(foodList.get(0));
        }
    }

    // ===============================================
    // DATA MẪU
    // ===============================================
    private void setupBreakfastData() {
        listBfMeat = new ArrayList<>();
        listBfMeat.add(new Food("1", "Trứng Luộc", 1, "2 quả", 155.0, 13.0, 1.0, 0.0, 11.0));
        listBfMeat.add(new Food("2", "Sữa Tươi", 1, "1 ly", 120.0, 8.0, 12.0, 0.0, 4.0));
        listBfMeat.add(new Food("3", "Xúc Xích", 1, "1 cây", 250.0, 10.0, 2.0, 0.0, 20.0));
        listBfMeat.add(new Food("4", "Bơ Đậu Phộng", 1, "2 thìa", 190.0, 7.0, 6.0, 2.0, 16.0));
        setupRecyclerView(rvBreakfastMeat, listBfMeat, food -> selBfMeat = food);

        listBfVeggie = new ArrayList<>();
        listBfVeggie.add(new Food("5", "Súp Lơ Xanh", 2, "100g", 34.0, 2.8, 7.0, 2.6, 0.4));
        listBfVeggie.add(new Food("6", "Cà Rốt Luộc", 2, "1 củ", 41.0, 0.9, 10.0, 2.8, 0.2));
        listBfVeggie.add(new Food("7", "Salad Trộn", 2, "1 bát", 50.0, 1.0, 5.0, 2.0, 3.0));
        listBfVeggie.add(new Food("8", "Rau Bina", 2, "100g", 23.0, 2.9, 3.6, 2.2, 0.4));
        setupRecyclerView(rvBreakfastVeggie, listBfVeggie, food -> selBfVeggie = food);

        listBfCarb = new ArrayList<>();
        listBfCarb.add(new Food("9", "Bánh Phở", 3, "1 bát", 200.0, 4.0, 45.0, 1.0, 0.5));
        listBfCarb.add(new Food("10", "Chuối", 3, "1 quả", 105.0, 1.3, 27.0, 3.1, 0.3));
        listBfCarb.add(new Food("11", "Khoai Lang", 3, "1 củ", 112.0, 2.0, 26.0, 4.0, 0.1));
        listBfCarb.add(new Food("12", "Yến Mạch", 3, "50g", 194.0, 7.0, 34.0, 5.0, 3.5));
        setupRecyclerView(rvBreakfastCarb, listBfCarb, food -> selBfCarb = food);
    }

    private void setupLunchData() {
        listLunchMeat = new ArrayList<>();
        listLunchMeat.add(new Food("13", "Ức Gà", 1, "200g", 330.0, 62.0, 0.0, 0.0, 7.2));
        listLunchMeat.add(new Food("14", "Thịt Lợn", 1, "150g", 360.0, 30.0, 0.0, 0.0, 25.0));
        listLunchMeat.add(new Food("15", "Tôm Hấp", 1, "150g", 150.0, 30.0, 0.0, 0.0, 2.0));
        listLunchMeat.add(new Food("16", "Đậu Phụ", 1, "100g", 76.0, 8.0, 1.9, 0.3, 4.8));
        setupRecyclerView(rvLunchMeat, listLunchMeat, food -> selLunchMeat = food);

        listLunchVeggie = new ArrayList<>();
        listLunchVeggie.add(new Food("17", "Cà Chua Bi", 2, "10 quả", 30.0, 1.0, 6.0, 1.5, 0.2));
        listLunchVeggie.add(new Food("18", "Bắp Cải", 2, "100g", 25.0, 1.3, 5.8, 2.5, 0.1));
        listLunchVeggie.add(new Food("19", "Cải Ngọt", 2, "100g", 21.0, 2.0, 3.0, 2.0, 0.2));
        listLunchVeggie.add(new Food("20", "Nấm Mỡ", 2, "100g", 22.0, 3.1, 3.3, 1.0, 0.3));
        setupRecyclerView(rvLunchVeggie, listLunchVeggie, food -> selLunchVeggie = food);

        listLunchCarb = new ArrayList<>();
        listLunchCarb.add(new Food("21", "Gạo Lứt", 3, "1 bát", 216.0, 5.0, 45.0, 3.5, 1.8));
        listLunchCarb.add(new Food("22", "Khoai Tây", 3, "1 củ", 161.0, 4.3, 37.0, 3.8, 0.2));
        listLunchCarb.add(new Food("23", "Bún Tươi", 3, "1 bát", 130.0, 1.5, 30.0, 0.5, 0.2));
        listLunchCarb.add(new Food("24", "Cơm Trắng", 3, "1 bát", 205.0, 4.0, 45.0, 0.6, 0.4));
        setupRecyclerView(rvLunchCarb, listLunchCarb, food -> selLunchCarb = food);
    }

    private void setupDinnerData() {
        listDinnerMeat = new ArrayList<>();
        listDinnerMeat.add(new Food("25", "Cá Hồi", 1, "150g", 312.0, 30.0, 0.0, 0.0, 20.0));
        listDinnerMeat.add(new Food("26", "Gà Nướng", 1, "150g", 250.0, 40.0, 0.0, 0.0, 9.0));
        listDinnerMeat.add(new Food("27", "Thịt Bò", 1, "150g", 375.0, 39.0, 0.0, 0.0, 22.0));
        listDinnerMeat.add(new Food("28", "Trứng Cút", 1, "10 quả", 158.0, 13.0, 0.0, 0.0, 11.0));
        setupRecyclerView(rvDinnerMeat, listDinnerMeat, food -> selDinnerMeat = food);

        listDinnerVeggie = new ArrayList<>();
        listDinnerVeggie.add(new Food("29", "Cần Tây", 2, "100g", 14.0, 0.7, 3.0, 1.6, 0.2));
        listDinnerVeggie.add(new Food("30", "Dưa Chuột", 2, "1 quả", 16.0, 0.7, 4.0, 0.5, 0.1));
        listDinnerVeggie.add(new Food("31", "Đậu Cô Ve", 2, "100g", 31.0, 1.8, 7.0, 3.4, 0.2));
        listDinnerVeggie.add(new Food("32", "Rau Muống", 2, "100g", 19.0, 3.0, 3.0, 2.0, 0.2));
        setupRecyclerView(rvDinnerVeggie, listDinnerVeggie, food -> selDinnerVeggie = food);

        listDinnerCarb = new ArrayList<>();
        listDinnerCarb.add(new Food("33", "Táo", 3, "1 quả", 95.0, 0.5, 25.0, 4.4, 0.3));
        listDinnerCarb.add(new Food("34", "Ngô Luộc", 3, "1 bắp", 86.0, 3.0, 19.0, 2.0, 1.0));
        listDinnerCarb.add(new Food("35", "Cam", 3, "1 quả", 47.0, 0.9, 12.0, 2.4, 0.1));
        listDinnerCarb.add(new Food("36", "Bánh Mì", 3, "1 ổ", 265.0, 9.0, 49.0, 2.0, 3.0));
        setupRecyclerView(rvDinnerCarb, listDinnerCarb, food -> selDinnerCarb = food);
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