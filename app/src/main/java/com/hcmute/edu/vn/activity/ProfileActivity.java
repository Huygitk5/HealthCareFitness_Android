package com.hcmute.edu.vn.activity;

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
import com.hcmute.edu.vn.activity.HomeActivity;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.activity.NutritionActivity;
import com.hcmute.edu.vn.activity.WorkoutActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    TextView txtName, txtEmail, tvProfileAge, tvProfileWeight, tvProfileHeight;
    MaterialButton btnLogout;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);


        // 1. Ánh xạ các View
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        tvProfileWeight = findViewById(R.id.tvProfileWeight);
        tvProfileHeight = findViewById(R.id.tvProfileHeight);
        btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            // Chuyển hướng về trang Login (Bạn nhớ đổi đúng tên Class LoginActivity của bạn)
            Intent loginIntent = new Intent(ProfileActivity.this, com.hcmute.edu.vn.activity.LoginActivity.class);

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
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // THÊM DÒNG NÀY
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, WorkoutActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // THÊM DÒNG NÀY
            startActivity(i);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // THÊM DÒNG NÀY
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

        if (username != null && !username.isEmpty()) {

            // Khởi tạo Retrofit gọi API
            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

            // Gọi API lấy thông tin Profile từ Supabase
            apiService.getUserByUsername("eq." + username, "*").enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {

                    // Nếu lấy dữ liệu thành công
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        User currentUser = response.body().get(0);

                        // 1. Hiển thị Tên (Đổi getFullName() thành getName())
                        txtName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty()
                                ? currentUser.getName() : username);

                        // 2. Hiển thị "Địa chỉ" (Do DB không còn address, ta dùng tạm ô này để hiện Email nhé)
                        txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật Email");

                        // 3. Hiển thị chiều cao, cân nặng (Xử lý an toàn vì Double có thể null)
                        double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                        double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;

                        if (heightCm > 0 && weightKg > 0) {
                            tvProfileHeight.setText(heightCm + " cm");
                            tvProfileWeight.setText(weightKg + " kg");
                        } else {
                            tvProfileHeight.setText("-- cm");
                            tvProfileWeight.setText("-- kg");
                        }

                        // 4. Tính toán và hiển thị tuổi (Đổi getDob() thành getDateOfBirth())
                        int age = calculateAge(currentUser.getDateOfBirth());
                        if(age > 0) {
                            tvProfileAge.setText(String.valueOf(age));
                        } else {
                            tvProfileAge.setText("--");
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Không tìm thấy thông tin người dùng trên Cloud", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Lỗi mạng: Không thể tải dữ liệu Profile!", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập trong máy", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm tính tuổi
    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) return 0;
        try {
            // SỬA ĐỊNH DẠNG TỪ dd/MM/yyyy THÀNH yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);

            if (birthDate == null) return 0;

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            // Kiểm tra xem đã đến sinh nhật năm nay chưa
            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}