package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.DatabaseHelper;
import com.hcmute.edu.vn.adapter.ActivityAdapter;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.News;
import com.hcmute.edu.vn.adapter.NewsAdapter;
import com.hcmute.edu.vn.model.User; // Nhớ import đúng package User

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID; // Dùng để tạo ID ngẫu nhiên cho Bài tập

public class HomeActivity extends AppCompatActivity {

    TextView tvGreeting, tvCurrentWeight, tvCurrentHeight, tvCurrentAge, tvBMIValue, tvBMIStatus;
    ImageView btnNotification;
    RecyclerView rvActivities, rvNews;
    DatabaseHelper dbHelper;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        dbHelper = new DatabaseHelper(this);

        // 2. Ánh xạ các View từ XML
        tvGreeting = findViewById(R.id.tvGreeting);
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvCurrentHeight = findViewById(R.id.tvCurrentHeight);
        tvCurrentAge = findViewById(R.id.tvCurrentAge);
        tvBMIValue = findViewById(R.id.tvBMIValue);
        tvBMIStatus = findViewById(R.id.tvBMIStatus);
        btnNotification = findViewById(R.id.btnNotification);
        rvActivities = findViewById(R.id.rvActivities);
        rvNews = findViewById(R.id.rvNews);

        // 4. Sự kiện Click Chuông Thông báo
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Bạn có 0 thông báo mới", Toast.LENGTH_SHORT).show();
            }
        });

        // =========================================================
        // SETUP RECYCLER VIEW CHO ACTIVITIES (Bài tập)
        // =========================================================
        ArrayList<Exercise> activityList = new ArrayList<>();

        // Cập nhật lại khởi tạo Exercise với đầy đủ tham số
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Giảm Mỡ Thừa ⚡⚡", "Bài tập giúp đốt mỡ", 1, 1, 3, "20 mins", "", String.valueOf(R.drawable.workout_1), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Tăng Cơ 💪", "Xây dựng sức mạnh", 2, 2, 4, "30 mins", "", String.valueOf(R.drawable.workout_2), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Yoga Buổi Sáng 🧘", "Thư giãn tinh thần", 3, 1, 1, "15 mins", "", String.valueOf(R.drawable.workout_3), null));

        // Nếu file ActivityAdapter.java báo lỗi, bạn cần sửa nó lại để nhận List<Exercise> giống như ExerciseAdapter nhé!
        ActivityAdapter activityAdapter = new ActivityAdapter(this, activityList);

        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvActivities.setLayoutManager(activityLayoutManager);
        rvActivities.setAdapter(activityAdapter);

        // =========================================================
        // SETUP RECYCLER VIEW CHO NEWS (Tin tức)
        // =========================================================
        ArrayList<News> newsList = new ArrayList<>();
        newsList.add(new News("3 Tạng người trong tập gym, bạn thuộc tạng người nào?", "Đã đăng 3 phút trước  •  Thích Gym 24h", R.drawable.gym_tmp));
        newsList.add(new News("Thực đơn ăn kiêng 7 ngày đánh bay mỡ thừa an toàn", "Đã đăng 2 giờ trước  •  HLV Quang Lâm", R.drawable.gym_tmp));
        newsList.add(new News("Cách hít thở chuẩn khi tập tạ nặng tránh chấn thương", "Đã đăng 1 ngày trước  •  Fitness Center", R.drawable.gym_tmp));

        NewsAdapter newsAdapter = new NewsAdapter(this, newsList);
        LinearLayoutManager newsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvNews.setLayoutManager(newsLayoutManager);
        rvNews.setAdapter(newsAdapter);

        // =========================================================
        // XỬ LÝ CLICK BOTTOM NAVIGATION
        // =========================================================
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        navWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, WorkoutActivity.class); // Thay đổi theo đúng thư mục của bạn
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        LinearLayout navProfile = findViewById(R.id.nav_profile);
        navProfile.setOnClickListener(new View.OnClickListener() {@Override
        public void onClick(View v) {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class); // Thay đổi theo đúng thư mục của bạn
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        });

        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        navNutrition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NutritionActivity.class); // Thay đổi theo đúng thư mục của bạn
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        if (username != null && !username.isEmpty()) {
            loadUserData();
        }
    }

    private void loadUserData() {
        User currentUser = dbHelper.getUserDetails(username);
        if (currentUser == null) return;

        // CẬP NHẬT 1: Gọi getName() thay vì getFullName()
        tvGreeting.setText(currentUser.getName() != null ? currentUser.getName() : username);

        // CẬP NHẬT 2: Sử dụng Double (Object) để tránh lỗi NullPointerException
        Double heightCm = currentUser.getHeight();
        Double weightKg = currentUser.getWeight();

        // CẬP NHẬT 3: Gọi getDateOfBirth() thay vì getDob()
        int age = calculateAge(currentUser.getDateOfBirth());

        // Đảm bảo chữ luôn là màu trắng (do background có màu)
        tvBMIStatus.setTextColor(android.graphics.Color.WHITE);

        // Kiểm tra an toàn trước khi tính toán
        if (heightCm != null && weightKg != null && heightCm > 0 && weightKg > 0) {
            tvCurrentHeight.setText(heightCm + " cm");
            tvCurrentWeight.setText(weightKg + " kg");

            double heightM = heightCm / 100.0;
            double bmi = weightKg / (heightM * heightM);
            tvBMIValue.setText(String.format("%.1f", bmi));

            if (bmi < 18.5) {
                tvBMIStatus.setText("Thiếu cân");
                tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
            } else if (bmi >= 18.5 && bmi < 23) {
                tvBMIStatus.setText("Bình thường");
                tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
            } else {
                tvBMIStatus.setText("Béo phì");
                tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
            }
        } else {
            tvCurrentHeight.setText("-- cm");
            tvCurrentWeight.setText("-- kg");
            tvBMIValue.setText("--");
            tvBMIStatus.setText("Chưa có");
            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E"))); // Nền Xám
        }

        if (age > 0) tvCurrentAge.setText(String.valueOf(age));
        else tvCurrentAge.setText("--");
    }

    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);
            if (birthDate == null) return 0;

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}