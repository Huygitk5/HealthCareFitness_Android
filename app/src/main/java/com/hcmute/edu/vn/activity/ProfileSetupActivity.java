package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText edtFullName, edtDOB, edtAddress, edtHeight, edtWeight;
    RadioGroup rgGender;
    Button btnComplete;
    String receivedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);
        androidx.activity.EdgeToEdge.enable(this);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        edtFullName = findViewById(R.id.edtFullName);
        edtDOB = findViewById(R.id.edtDOB);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        rgGender = findViewById(R.id.rgGender);
        btnComplete = findViewById(R.id.btnComplete);

        Intent intent = getIntent();
        receivedUsername = intent.getStringExtra("KEY_REGISTER_USER");

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = edtFullName.getText().toString().trim();
                String dob = edtDOB.getText().toString().trim(); // Yêu cầu user nhập YYYY-MM-DD
                String heightStr = edtHeight.getText().toString().trim();
                String weightStr = edtWeight.getText().toString().trim();

                String gender = "Other";
                int selectedId = rgGender.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMale) gender = "Male";
                else if (selectedId == R.id.rbFemale) gender = "Female";

                if (fullName.isEmpty() || dob.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(ProfileSetupActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double height = Double.parseDouble(heightStr);
                    double weight = Double.parseDouble(weightStr);

                    // Tạo Object User CHỈ chứa các trường cần update
                    User updateData = new User();
                    updateData.setName(fullName);
                    updateData.setDateOfBirth(dob); // Lưu ý format "YYYY-MM-DD"
                    updateData.setGender(gender);
                    updateData.setHeight(height);
                    updateData.setWeight(weight);

                    btnComplete.setEnabled(false);
                    btnComplete.setText("Đang lưu...");

                    SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                    // ===============================================================
                    // BƯỚC 1: TÌM USER ĐỂ LẤY ID (UUID)
                    // ===============================================================
                    apiService.getUserByUsername("eq." + receivedUsername, "*").enqueue(new Callback<List<User>>() {
                        @Override
                        public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                            if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                                String userId = response.body().get(0).getId();

                                // ===============================================================
                                // BƯỚC 2: CẬP NHẬT PROFILE
                                // ===============================================================
                                apiService.updateUserProfile("eq." + receivedUsername, updateData).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> profileResponse) {
                                        if (profileResponse.isSuccessful()) {

                                            // ===============================================================
                                            // BƯỚC 3: TÍNH VÀ LƯU BMI VÀO DATABASE CHO BIỂU ĐỒ
                                            // ===============================================================
                                            double heightM = height / 100.0;
                                            double newBmi = weight / (heightM * heightM);
                                            String currentDateFull = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

                                            BmiLog newLog = new BmiLog(UUID.randomUUID().toString(), userId, weight, height, newBmi, currentDateFull);

                                            apiService.saveBmiLog(newLog).enqueue(new Callback<Void>() {
                                                @Override
                                                public void onResponse(Call<Void> call, Response<Void> logResponse) {
                                                    goToHome(); // Lưu log thành công -> Xong xuôi!
                                                }

                                                @Override
                                                public void onFailure(Call<Void> call, Throwable t) {
                                                    goToHome(); // Rớt mạng khúc lưu log thì vẫn cho vào trang Home
                                                }
                                            });

                                        } else {
                                            showError("Lỗi cập nhật thông tin!");
                                        }
                                    }

                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        showError("Lỗi kết nối mạng khi cập nhật!");
                                    }
                                });

                            } else {
                                showError("Không tìm thấy tài khoản để cập nhật!");
                            }
                        }

                        @Override
                        public void onFailure(Call<List<User>> call, Throwable t) {
                            showError("Lỗi mạng khi tìm thông tin tài khoản!");
                        }
                    });

                } catch (NumberFormatException e) {
                    Toast.makeText(ProfileSetupActivity.this, "Chiều cao, cân nặng phải là số!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Hàm phụ trợ để báo lỗi và mở lại nút bấm
    private void showError(String message) {
        btnComplete.setEnabled(true);
        btnComplete.setText("Hoàn Tất");
        Toast.makeText(ProfileSetupActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    // Hàm phụ trợ để chuyển sang Home
    private void goToHome() {
        Toast.makeText(ProfileSetupActivity.this, "Thiết lập thành công!", Toast.LENGTH_SHORT).show();

        // Ép lưu username vào SharedPreferences BẰNG LỆNH COMMIT() để Home tải được data ngay lập tức
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().putString("KEY_USER", receivedUsername).commit();

        Intent i = new Intent(ProfileSetupActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
    }
}