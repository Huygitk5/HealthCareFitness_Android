package com.hcmute.edu.vn.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.DatabaseHelper;
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
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        dbHelper = new DatabaseHelper(this);

        // 1. Ánh xạ các View
        txtName = findViewById(R.id.txtName);
        txtLocation = findViewById(R.id.txtLocation);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        tvProfileWeight = findViewById(R.id.tvProfileWeight);
        tvProfileHeight = findViewById(R.id.tvProfileHeight);

        // 2. Lấy username từ Intent
        Intent intent = getIntent();
        String username = intent.getStringExtra("KEY_USER");

        // 3. Truy xuất DB và hiển thị
        if (username != null && !username.isEmpty()) {
            User currentUser = dbHelper.getUserDetails(username);
            if (currentUser != null) {
                txtName.setText(currentUser.getFullName());
                txtLocation.setText(currentUser.getAddress());

                // Hiển thị chiều cao, cân nặng
                tvProfileHeight.setText(currentUser.getHeight() + " cm");
                tvProfileWeight.setText(currentUser.getWeight() + " kg");

                // Tính toán và hiển thị tuổi
                int age = calculateAge(currentUser.getDob());
                tvProfileAge.setText(String.valueOf(age));
            }
        }

        // =========================================================
        // XỬ LÝ BOTTOM NAVIGATION
        // =========================================================
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, HomeActivity.class);
            i.putExtra("KEY_USER", username); // Truyền lại username
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, WorkoutActivity.class);
            i.putExtra("KEY_USER", username); // Truyền lại username
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
            i.putExtra("KEY_USER", username); // Truyền lại username
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });
    }

    // Hàm tính tuổi dựa trên chuỗi ngày sinh (định dạng dd/MM/yyyy)
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
            // Nếu chưa tới sinh nhật trong năm nay thì trừ đi 1 tuổi
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