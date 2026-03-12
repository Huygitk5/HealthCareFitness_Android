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
import com.hcmute.edu.vn.database.DatabaseHelper;
// IMPORT ĐÚNG MODEL CHUẨN
import com.hcmute.edu.vn.model.User;

import java.util.UUID; // Dùng để tạo ID ngẫu nhiên

public class RegisterActivity extends AppCompatActivity {

    EditText edtUser, edtPass, edtConfirm;
    TextView tvPasswordMsg, tvConfirmMsg;
    Button btnRegister;
    TextView tvSignInLink;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        dbHelper = new DatabaseHelper(this);

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
                        tvPasswordMsg.setVisibility(View.VISIBLE);

                        if (passwordError != null) {
                            tvPasswordMsg.setText("❗ " + passwordError);
                            tvPasswordMsg.setTextColor(Color.parseColor("#D32F2F"));
                        } else {
                            tvPasswordMsg.setText("✅ Password is strong and valid!");
                            tvPasswordMsg.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }
                } else {
                    tvPasswordMsg.setVisibility(View.GONE);
                }
            }
        });

        edtConfirm.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String pass = edtPass.getText().toString();
                    String confirm = edtConfirm.getText().toString();

                    if (!confirm.isEmpty()) {
                        tvConfirmMsg.setVisibility(View.VISIBLE);

                        if (!pass.equals(confirm)) {
                            tvConfirmMsg.setText("❗ ERROR: Password Don't Match!");
                            tvConfirmMsg.setTextColor(Color.parseColor("#D32F2F"));
                        } else {
                            tvConfirmMsg.setText("✅ Passwords match!");
                            tvConfirmMsg.setTextColor(Color.parseColor("#4CAF50"));
                        }
                    }
                } else {
                    tvConfirmMsg.setVisibility(View.GONE);
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = edtUser.getText().toString().trim();
                String pass = edtPass.getText().toString().trim();
                String confirm = edtConfirm.getText().toString().trim();

                if (user.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                String passwordError = validatePassword(pass);
                if (passwordError != null) {
                    tvPasswordMsg.setVisibility(View.VISIBLE);
                    tvPasswordMsg.setText("❗ " + passwordError);
                    tvPasswordMsg.setTextColor(Color.parseColor("#D32F2F"));
                    return;
                }

                if (!pass.equals(confirm)) {
                    tvConfirmMsg.setVisibility(View.VISIBLE);
                    tvConfirmMsg.setText("❗ ERROR: Password Don't Match!");
                    tvConfirmMsg.setTextColor(Color.parseColor("#D32F2F"));
                    return;
                }

                // KHỞI TẠO USER MỚI: Dùng UUID cho id, truyền username.
                // Các thông tin profile khác truyền chuỗi rỗng ("") hoặc null vì sẽ được setup ở ProfileSetupActivity
                String newUserId = UUID.randomUUID().toString();
                User newUser = new User(newUserId, user, "", "", "", null, null, null, "", null);

                boolean success = dbHelper.addUser(newUser);
                if (success) {
                    Toast.makeText(RegisterActivity.this, "Create Success!", Toast.LENGTH_SHORT).show();
                    finish(); // Quay lại trang Login
                } else {
                    Toast.makeText(RegisterActivity.this, "Account already exists!", Toast.LENGTH_SHORT).show();
                }

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