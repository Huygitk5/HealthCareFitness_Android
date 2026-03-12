package com.hcmute.edu.vn.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.SupabaseApiService;
import com.hcmute.edu.vn.SupabaseClient;
import com.hcmute.edu.vn.home.activity.HomeActivity;
import com.hcmute.edu.vn.home.model.User;
import com.hcmute.edu.vn.profile.ProfileSetupActivity;

import java.util.List;

import retrofit2.Call;

public class LoginActivity extends AppCompatActivity {

    EditText edtUser, edtPass;
    TextInputLayout tilPassword;
    Button btnSignIn;
    TextView tvRegisterLink;
    TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        edtUser = findViewById(R.id.edtUsername);
        edtPass = findViewById(R.id.edtPassword);
        tilPassword = findViewById(R.id.tilPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvRegisterLink = findViewById(R.id.tvRegisterLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Xóa khai báo dbHelper, thêm đoạn mã sau vào sự kiện btnSignIn.setOnClickListener:

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUser.getText().toString().trim();
                String password = edtPass.getText().toString().trim();

                tilPassword.setError(null);

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Vô hiệu hóa nút để tránh bấm nhiều lần
                btnSignIn.setEnabled(false);
                btnSignIn.setText("Đang đăng nhập...");

                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                // Gọi API: Tìm username bằng và password bằng...
                apiService.loginUser("eq." + username, "eq." + password, "*").enqueue(new retrofit2.Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, retrofit2.Response<List<User>> response) {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");

                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            // CÓ DATA TRẢ VỀ -> ĐĂNG NHẬP THÀNH CÔNG
                            User currentUser = response.body().get(0);

                            android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            pref.edit().putString("KEY_USER", username).apply();

                            Intent intent;
                            // Kiểm tra xem đã cập nhật profile chưa (kiểm tra name)
                            if (currentUser.getName() == null || currentUser.getName().isEmpty()) {
                                intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
                                intent.putExtra("KEY_REGISTER_USER", username);
                                Toast.makeText(LoginActivity.this, "Hãy điền thông tin của bạn!", Toast.LENGTH_SHORT).show();
                            } else {
                                intent = new Intent(LoginActivity.this, HomeActivity.class);
                                intent.putExtra("KEY_USER", username);
                                Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                            }
                            startActivity(intent);
                            finish();
                        } else {
                            // TRẢ VỀ RỖNG -> SAI TÀI KHOẢN HOẶC MẬT KHẨU
                            tilPassword.setError("Sai tài khoản hoặc mật khẩu!");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        btnSignIn.setEnabled(true);
                        btnSignIn.setText("Sign In");
                        Toast.makeText(LoginActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo Intent: Từ Login -> sang Register
                Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(i);
            }
        });

        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }
}