package com.hcmute.edu.vn.profile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.home.activity.HomeActivity;
import com.hcmute.edu.vn.home.model.User;
import com.hcmute.edu.vn.nutrition.activity.NutritionActivity;
import com.hcmute.edu.vn.workout.activity.WorkoutActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView txtName, txtLocation, tvProfileAge, tvProfileWeight, tvProfileHeight;
    MaterialButton btnLogout;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);


        // 1. Ánh xạ các View
        txtName = findViewById(R.id.txtName);
        txtLocation = findViewById(R.id.txtLocation);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        tvProfileWeight = findViewById(R.id.tvProfileWeight);
        tvProfileHeight = findViewById(R.id.tvProfileHeight);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // Chuyển hướng về trang Login (Bạn nhớ đổi đúng tên Class LoginActivity của bạn)
            Intent loginIntent = new Intent(ProfileActivity.this, com.hcmute.edu.vn.login.LoginActivity.class);

            // Cờ này giúp xóa toàn bộ Activity cũ, ngăn người dùng bấm Back để vào lại
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(loginIntent);
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // =========================================================
        // XỬ LÝ BOTTOM NAVIGATION
        // =========================================================
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, HomeActivity.class);
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, WorkoutActivity.class);
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
            startActivity(i);
            overridePendingTransition(0, 0);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
    }

    // Đưa việc lấy dữ liệu vào onResume để luôn refresh thông tin mới nhất
    @Override
    protected void onResume() {
        super.onResume();

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // 3. Truy xuất DB và hiển thị (Giống hệt cách của HomeActivity)
        if (username != null && !username.isEmpty()) {
            User currentUser = dbHelper.getUserDetails(username);

            if (currentUser != null) {
                // Hiển thị Tên và Địa chỉ
                txtName.setText(currentUser.getFullName() != null ? currentUser.getFullName() : username);
                txtLocation.setText(currentUser.getAddress() != null ? currentUser.getAddress() : "Chưa cập nhật địa chỉ");

                // Hiển thị chiều cao, cân nặng
                double heightCm = currentUser.getHeight();
                double weightKg = currentUser.getWeight();

                if (heightCm > 0 && weightKg > 0) {
                    tvProfileHeight.setText(heightCm + " cm");
                    tvProfileWeight.setText(weightKg + " kg");
                } else {
                    tvProfileHeight.setText("-- cm");
                    tvProfileWeight.setText("-- kg");
                }

                // Tính toán và hiển thị tuổi
                int age = calculateAge(currentUser.getDob());
                if(age > 0) {
                    tvProfileAge.setText(String.valueOf(age));
                } else {
                    tvProfileAge.setText("--");
                }
            }
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm tính tuổi
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