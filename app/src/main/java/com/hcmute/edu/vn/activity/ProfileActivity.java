package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.database.DatabaseHelper;
import com.hcmute.edu.vn.R;
// 1. IMPORT ĐÚNG MODEL MỚI
import com.hcmute.edu.vn.model.User;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    TextView txtName, txtLocation, tvProfileAge, tvProfileWeight, tvProfileHeight;
    MaterialButton btnLogout;
    DatabaseHelper dbHelper;
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

        dbHelper = new DatabaseHelper(this);

        // 1. Ánh xạ các View
        txtName = findViewById(R.id.txtName);
        txtLocation = findViewById(R.id.txtLocation);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        tvProfileWeight = findViewById(R.id.tvProfileWeight);
        tvProfileHeight = findViewById(R.id.tvProfileHeight);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // 2. XOÁ PHIÊN ĐĂNG NHẬP TRONG SHAREDPREFS
            android.content.SharedPreferences preferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = preferences.edit();
            editor.clear(); // Xoá sạch dữ liệu user đã lưu
            editor.apply();

            // Chuyển hướng về trang Login
            Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
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
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Giữ cờ này để chuyển mượt
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, WorkoutActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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

    @Override
    protected void onResume() {
        super.onResume();

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        if (username != null && !username.isEmpty()) {
            User currentUser = dbHelper.getUserDetails(username);

            if (currentUser != null) {
                // 3. ĐỔI THÀNH getName() VÀ BỎ LOGIC getAddress()
                txtName.setText(currentUser.getName() != null ? currentUser.getName() : username);
                txtLocation.setText("Chưa cập nhật địa chỉ");

                // 4. DÙNG DOUBLE (OBJECT) ĐỂ CHỐNG LỖI NULL
                Double heightCm = currentUser.getHeight();
                Double weightKg = currentUser.getWeight();

                if (heightCm != null && weightKg != null && heightCm > 0 && weightKg > 0) {
                    tvProfileHeight.setText(heightCm + " cm");
                    tvProfileWeight.setText(weightKg + " kg");
                } else {
                    tvProfileHeight.setText("-- cm");
                    tvProfileWeight.setText("-- kg");
                }

                // 5. ĐỔI THÀNH getDateOfBirth()
                int age = calculateAge(currentUser.getDateOfBirth());
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