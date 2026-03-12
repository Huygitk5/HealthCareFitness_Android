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

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edtUser.getText().toString().trim();
                String password = edtPass.getText().toString().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnSignIn.setEnabled(false);
                btnSignIn.setText("Đang đăng nhập...");

                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                // LẤY EMAIL BẰNG USERNAME
                apiService.getUserByUsername("eq." + username, "*").enqueue(new retrofit2.Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, retrofit2.Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                            // Lấy email thành công
                            String userEmail = response.body().get(0).getEmail();

                            // TIẾN HÀNH ĐĂNG NHẬP AUTH BẰNG EMAIL + PASSWORD
                            SignInRequest loginRequest = new SignInRequest(userEmail, password);
                            apiService.signInAuth(loginRequest).enqueue(new retrofit2.Callback<SignInResponse>() {
                                @Override
                                public void onResponse(Call<SignInResponse> call, retrofit2.Response<SignInResponse> loginResponse) {
                                    btnSignIn.setEnabled(true);
                                    btnSignIn.setText("Sign In");

                                    if (loginResponse.isSuccessful() && loginResponse.body() != null) {
                                        // Đăng nhập thành công!
                                        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                        pref.edit().putString("KEY_USER", username).apply();

                                        Toast.makeText(LoginActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        tilPassword.setError("Sai tài khoản hoặc mật khẩu!");
                                    }
                                }
                                @Override
                                public void onFailure(Call<SignInResponse> call, Throwable t) {
                                    btnSignIn.setEnabled(true);
                                    btnSignIn.setText("Sign In");
                                }
                            });

                        } else {
                            // Không tìm thấy username trong database
                            btnSignIn.setEnabled(true);
                            btnSignIn.setText("Sign In");
                            tilPassword.setError("Tài khoản không tồn tại!");
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