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
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.model.SignUpRequest;
import com.hcmute.edu.vn.model.SignUpResponse;
import com.hcmute.edu.vn.model.User;

import java.util.List;

import retrofit2.Call;

public class RegisterActivity extends AppCompatActivity {

    EditText edtUser, edtPass, edtConfirm, edtEmail;
    TextView tvPasswordMsg, tvConfirmMsg, tvEmailMsg;
    Button btnRegister;
    TextView tvSignInLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        androidx.activity.EdgeToEdge.enable(this);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        edtUser = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPass = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirmPassword);
        tvPasswordMsg = findViewById(R.id.tvPasswordMsg);
        tvConfirmMsg = findViewById(R.id.tvConfirmMsg);
        tvEmailMsg = findViewById(R.id.tvEmailMsg);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignInLink = findViewById(R.id.tvSignInLink);

        edtEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String email = edtEmail.getText().toString().trim();
                    if (!email.isEmpty() && tvEmailMsg != null) {
                        tvEmailMsg.setVisibility(View.VISIBLE);

                        if (!validateEmail(email)) {
                            // EMAIL SAI -> Dấu ! và chữ Đỏ
                            tvEmailMsg.setText("❗ Invalid email format! (e.g. abc@gmail.com)");
                            tvEmailMsg.setTextColor(Color.parseColor("#D32F2F"));
                        } else {
                            // HỢP LỆ -> Dấu tick và chữ Xanh lá
                            tvEmailMsg.setText("✅ Valid email format!");
                            tvEmailMsg.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }
                } else {
                    if (tvEmailMsg != null) tvEmailMsg.setVisibility(View.GONE);
                }
            }
        });

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
                String email = edtEmail.getText().toString();
                String pass = edtPass.getText().toString();
                String confirm = edtConfirm.getText().toString();

                if (user.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                String passwordError = validatePassword(pass);
                if (passwordError != null) {
                    tvPasswordMsg.setText(passwordError);
                    return;
                }

                if (!pass.equals(confirm)) {
                    tvConfirmMsg.setText("Mật khẩu không khớp!");
                    return;
                }

                btnRegister.setEnabled(false);
                btnRegister.setText("Đang kiểm tra...");

                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                // KIỂM TRA XEM USERNAME ĐÃ TỒN TẠI TRONG DATABASE CHƯA
                apiService.getUserByUsername("eq." + user, "*").enqueue(new retrofit2.Callback<List<User>>() {
                    @Override
                    public void onResponse(Call<List<User>> call, retrofit2.Response<List<User>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            // Đã tìm thấy 1 user xài username này
                            btnRegister.setEnabled(true);
                            btnRegister.setText("REGISTER");
                            Toast.makeText(RegisterActivity.this, "Username này đã có người sử dụng!", Toast.LENGTH_SHORT).show();
                        } else {
                            // USERNAME CHƯA AI DÙNG -> TIẾN HÀNH ĐĂNG KÝ EMAIL + PASS VỚI AUTH
                            btnRegister.setText("Đang tạo tài khoản...");
                            SignUpRequest authRequest = new SignUpRequest(email, pass);

                            apiService.signUpAuth(authRequest).enqueue(new retrofit2.Callback<SignUpResponse>() {
                                @Override
                                public void onResponse(Call<SignUpResponse> call, retrofit2.Response<SignUpResponse> authResponse) {
                                    if (authResponse.isSuccessful() && authResponse.body() != null) {
                                        String authId = authResponse.body().getUser().getId();

                                        // BƯỚC 3: LƯU (ID, USERNAME, EMAIL) VÀO DATABASE
                                        User newUserProfile = new User(authId, user, email, "User Mới");
                                        apiService.registerUser(newUserProfile).enqueue(new retrofit2.Callback<Void>() {
                                            @Override
                                            public void onResponse(Call<Void> call, retrofit2.Response<Void> dbResponse) {
                                                if (dbResponse.isSuccessful()) {
                                                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                                    finish();
                                                } else {
                                                    try {
                                                        String errorDetails = dbResponse.errorBody().string();
                                                        android.util.Log.e("LỖI_DB_SUPABASE", "Chi tiết từ Supabase: " + errorDetails);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                    Toast.makeText(RegisterActivity.this, "Lỗi lưu hồ sơ!", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                            @Override
                                            public void onFailure(Call<Void> call, Throwable t) {
                                                btnRegister.setEnabled(true);
                                                btnRegister.setText("REGISTER");
                                                Toast.makeText(RegisterActivity.this, "Rớt mạng khi lưu hồ sơ!", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        btnRegister.setEnabled(true);
                                        btnRegister.setText("REGISTER");
                                        Toast.makeText(RegisterActivity.this, "Email này đã được sử dụng hoặc không hợp lệ!", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<SignUpResponse> call, Throwable t) {
                                    btnRegister.setEnabled(true);
                                    btnRegister.setText("REGISTER");

                                    // Thêm dòng này để in lỗi thật ra màn hình Logcat
                                    android.util.Log.e("LỖI_API", "Chi tiết lỗi: " + t.getMessage());

                                    Toast.makeText(RegisterActivity.this, "Lỗi mạng kết nối Auth!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    @Override
                    public void onFailure(Call<List<User>> call, Throwable t) {
                        // Sửa lại đoạn này
                        btnRegister.setEnabled(true);
                        btnRegister.setText("REGISTER"); // Trả lại tên cho nút bấm

                        // In log lỗi của bước 1 ra Logcat
                        android.util.Log.e("LỖI_API_BUOC_1", "Nguyên nhân rớt mạng: " + t.getMessage());

                        Toast.makeText(RegisterActivity.this, "Lỗi kết nối mạng ở bước kiểm tra!", Toast.LENGTH_SHORT).show();
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

    private boolean validateEmail(String email) {
        // Sử dụng pattern có sẵn của Android để check email chuẩn
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
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