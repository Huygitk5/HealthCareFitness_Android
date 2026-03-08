package com.hcmute.edu.vn.home.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.home.DatabaseHelper;
import com.hcmute.edu.vn.home.adapter.ActivityAdapter;
import com.hcmute.edu.vn.home.model.ActivityItem;
import com.hcmute.edu.vn.home.model.News;
import com.hcmute.edu.vn.home.adapter.NewsAdapter;
import com.hcmute.edu.vn.home.model.User;

import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity {

    TextView tvGreeting;
    ImageView btnNotification;
    RecyclerView rvActivities, rvNews;
    TextView tvCurrentWeight, tvCurrentHeight, tvCurrentAge, tvBMIValue;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_view);

        dbHelper = new DatabaseHelper(this);

        // 2. Ánh xạ các View từ XML
        tvGreeting = findViewById(R.id.tvGreeting);
        btnNotification = findViewById(R.id.btnNotification);
        rvActivities = findViewById(R.id.rvActivities);
        rvNews = findViewById(R.id.rvNews);

        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvCurrentHeight = findViewById(R.id.tvCurrentHeight);
        tvCurrentAge = findViewById(R.id.tvCurrentAge);
        tvBMIValue = findViewById(R.id.tvBMIValue);

        // 3. Nhận dữ liệu User từ màn hình Login
        Intent intent = getIntent();
        String username = intent.getStringExtra("KEY_USER");
        String password = intent.getStringExtra("KEY_PASS");



        if (username != null && !username.isEmpty()) {
            User currentUser = dbHelper.getUserDetails(username);
            tvGreeting.setText(currentUser.getFullName() != null ? currentUser.getFullName() : username);

            // Lấy thông tin số thực
            double heightCm = currentUser.getHeight();
            double weightKg = currentUser.getWeight();

            if (heightCm > 0 && weightKg > 0) {
                tvCurrentHeight.setText(heightCm + " cm");
                tvCurrentWeight.setText(weightKg + " kg");

                // Tính toán trực tiếp không cần Parse
                double heightM = heightCm / 100.0;
                double bmi = weightKg / (heightM * heightM);
                tvBMIValue.setText(String.format("%.1f", bmi));
            } else {
                tvCurrentHeight.setText("-- cm");
                tvCurrentWeight.setText("-- kg");
                tvBMIValue.setText("--");
            }
        }

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
        activityList.add(new ActivityItem("Giảm Mỡ Thừa ⚡⚡", R.drawable.bt1));
        activityList.add(new ActivityItem("Tăng Cơ 💪", R.drawable.bt2));
        activityList.add(new ActivityItem("Yoga Buổi Sáng 🧘", R.drawable.bt3));

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
    }
}