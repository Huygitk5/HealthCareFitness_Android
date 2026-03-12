package com.hcmute.edu.vn.home.activity;

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
import com.hcmute.edu.vn.DatabaseHelper;
import com.hcmute.edu.vn.home.adapter.ActivityAdapter;
import com.hcmute.edu.vn.home.model.ActivityItem;
import com.hcmute.edu.vn.home.model.News;
import com.hcmute.edu.vn.home.adapter.NewsAdapter;
import com.hcmute.edu.vn.home.model.User;
import com.hcmute.edu.vn.nutrition.activity.NutritionActivity;
import com.hcmute.edu.vn.profile.ProfileActivity;
import com.hcmute.edu.vn.workout.activity.WorkoutActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class  HomeActivity extends AppCompatActivity {

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
        ArrayList<ActivityItem> activityList = new ArrayList<>();
        activityList.add(new ActivityItem("Giảm Mỡ Thừa ⚡⚡", R.drawable.workout_1));
        activityList.add(new ActivityItem("Tăng Cơ 💪", R.drawable.workout_2));
        activityList.add(new ActivityItem("Yoga Buổi Sáng 🧘", R.drawable.workout_3));

        // DÙNG ActivityAdapter MỚI ĐỂ GIAO DIỆN TO RA
        ActivityAdapter activityAdapter = new ActivityAdapter(this, activityList);

        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvActivities.setLayoutManager(activityLayoutManager);
        rvActivities.setAdapter(activityAdapter);

        // =========================================================
        // SETUP RECYCLER VIEW CHO NEWS (Tin tức)
        // =========================================================
        ArrayList<News> newsList = new ArrayList<>();

        newsList.add(new News(
                "3 Tạng người trong tập gym, bạn thuộc tạng người nào?",
                "Đã đăng 3 phút trước  •  Thích Gym 24h",
                R.drawable.gym_tmp));

        newsList.add(new News(
                "Thực đơn ăn kiêng 7 ngày đánh bay mỡ thừa an toàn",
                "Đã đăng 2 giờ trước  •  HLV Quang Lâm",
                R.drawable.gym_tmp));

        newsList.add(new News(
                "Cách hít thở chuẩn khi tập tạ nặng tránh chấn thương",
                "Đã đăng 1 ngày trước  •  Fitness Center",
                R.drawable.gym_tmp));

        // Gọi NewsAdapter mới tạo
        NewsAdapter newsAdapter = new NewsAdapter(this, newsList);

        // Setup vuốt ngang
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
                Intent intent = new Intent(HomeActivity.this, WorkoutActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        LinearLayout navProfile = findViewById(R.id.nav_profile);
        navProfile.setOnClickListener(new View.OnClickListener() {@Override
        public void onClick(View v) {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
        });
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        navNutrition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NutritionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LẤY USERNAME TỪ SHAREDPREFS
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // NẾU CÓ USERNAME THÌ LOAD DATA
        if (username != null && !username.isEmpty()) {
            loadUserData();
        }
    }

    private void loadUserData() {
        User currentUser = dbHelper.getUserDetails(username);
        if (currentUser == null) return;

        tvGreeting.setText(currentUser.getFullName() != null ? currentUser.getFullName() : username);

        double heightCm = currentUser.getHeight();
        double weightKg = currentUser.getWeight();
        int age = calculateAge(currentUser.getDob());

        if (heightCm > 0 && weightKg > 0) {
            tvCurrentHeight.setText(heightCm + " cm");
            tvCurrentWeight.setText(weightKg + " kg");

            double heightM = heightCm / 100.0;
            double bmi = weightKg / (heightM * heightM);
            tvBMIValue.setText(String.format("%.1f", bmi));

            // Code set màu sắc BMI Status... (copy từ code cũ của bạn)
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
            // Hiển thị mặc định khi chưa có data...
            tvCurrentHeight.setText("-- cm");
            tvCurrentWeight.setText("-- kg");
            tvBMIValue.setText("--");
            tvBMIStatus.setText("Chưa có");
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