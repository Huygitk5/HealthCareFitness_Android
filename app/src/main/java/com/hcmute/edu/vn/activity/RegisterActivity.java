package com.hcmute.edu.vn.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.User;

import retrofit2.Call;

public class RegisterActivity extends AppCompatActivity {

    EditText edtUser, edtPass, edtConfirm;
    TextView tvPasswordMsg, tvConfirmMsg;
    Button btnRegister;
    TextView tvSignInLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);


        edtUser = findViewById(R.id.edtUsername);
        edtPass = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirmPassword);
        tvPasswordMsg = findViewById(R.id.tvPasswordMsg);
        tvConfirmMsg = findViewById(R.id.tvConfirmMsg);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignInLink = findViewById(R.id.tvSignInLink);

        edtPass.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String pass = edtPass.getText().toString();
                    if (!pass.isEmpty()) {
                        String passwordError = validatePassword(pass);
                        tvPasswordMsg.setVisibility(View.VISIBLE); // Hiện thông báo lên

                        if (passwordError != null) {
                            // CÓ LỖI -> Dấu ! và chữ Đỏ
                            tvPasswordMsg.setText("❗ " + passwordError);
                            tvPasswordMsg.setTextColor(Color.parseColor("#D32F2F")); // Đỏ
                        } else {
                            // HỢP LỆ -> Dấu tick và chữ Xanh lá
                            tvPasswordMsg.setText("✅ Password is strong and valid!");
                            tvPasswordMsg.setTextColor(Color.parseColor("#4CAF50")); // Xanh lá
                        }
                    }
                } else {
                    // Khi đang nhập thì ẩn thông báo đi cho gọn
                    tvPasswordMsg.setVisibility(View.GONE);
                }
            }
        });

        // =========================================================
        // 2. SỰ KIỆN KHI CLICK RA KHỎI Ô CONFIRM PASSWORD
        // =========================================================
        edtConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String pass = edtPass.getText().toString();
                    String confirm = edtConfirm.getText().toString();

                    if (!confirm.isEmpty()) {
                        tvConfirmMsg.setVisibility(View.VISIBLE); // Hiện thông báo lên

                        if (!pass.equals(confirm)) {
                            // KHÔNG KHỚP -> Dấu ! và chữ Đỏ
                            tvConfirmMsg.setText("❗ ERROR: Password Don't Match!");
                            tvConfirmMsg.setTextColor(Color.parseColor("#D32F2F"));
                        } else {
                            // KHỚP -> Dấu tick và chữ Xanh lá
                            tvConfirmMsg.setText("✅ Passwords match!");
                            tvConfirmMsg.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }
                } else {
                    // Khi đang nhập thì ẩn đi
                    tvConfirmMsg.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = edtUser.getText().toString();
                String pass = edtPass.getText().toString();
                String confirm = edtConfirm.getText().toString();

                if (pass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }
                String passwordError = validatePassword(pass);
                if (passwordError != null) {
                    tvPasswordMsg.setText(passwordError);
                    return;
                }

                if (!pass.equals(confirm)) {
                    tvConfirmMsg.setText("Passwords do not match!");
                    return;
                }
                btnRegister.setEnabled(false);

                User newUser = new User(user, "User Mới");
                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                // Gọi API Đăng ký
                apiService.registerUser(newUser).enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                        btnRegister.setEnabled(true);

                        // Nếu HTTP CODE là 200~299 (Thành công)
                        if (response.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            // Supabase sẽ báo lỗi 409 Conflict nếu username (đặt UNIQUE) bị trùng
                            Toast.makeText(RegisterActivity.this, "Tài khoản đã tồn tại!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(RegisterActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        tvSignInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private String validatePassword(String password) {
        if (password.length() < 8) return "Password must be at least 8 characters long";
        if (!password.matches(".*[A-Z].*")) return "Password must contain at least one uppercase letter (A-Z)";
        if (!password.matches(".*[a-z].*")) return "Password must contain at least one lowercase letter (a-z)";
        if (!password.matches(".*\\d.*")) return "Password must contain at least one number (0-9)";
        if (!password.matches(".*[!@#$%^&*+=?-].*")) return "Password must contain at least one special character (!@#$%^&*...)";
        return null;
    }
}
