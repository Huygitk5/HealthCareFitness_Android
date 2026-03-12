package com.hcmute.edu.vn.activity;

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
import com.hcmute.edu.vn.database.DatabaseHelper;
import com.hcmute.edu.vn.model.User;

public class LoginActivity extends AppCompatActivity {

    EditText edtUser, edtPass;
    TextInputLayout tilPassword;
    Button btnSignIn;
    TextView tvRegisterLink;
    TextView tvForgotPassword;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        dbHelper = new DatabaseHelper(this);

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

                tilPassword.setError(null);

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter all the information!", Toast.LENGTH_SHORT).show();
                } else {
                    if (dbHelper.checkUserExists(username)) {

                        User currentUser = dbHelper.getUserDetails(username);

                        // Lưu phiên đăng nhập
                        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        android.content.SharedPreferences.Editor editor = pref.edit();
                        editor.putString("KEY_USER", username);
                        editor.apply();

                        Intent intent;
                        if (currentUser.getName() == null || currentUser.getName().isEmpty()) {
                            intent = new Intent(LoginActivity.this, ProfileSetupActivity.class);
                            intent.putExtra("KEY_REGISTER_USER", username);
                            Toast.makeText(LoginActivity.this, "Fill in your information!", Toast.LENGTH_SHORT).show();
                        } else {
                            intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.putExtra("KEY_USER", username);
                            Toast.makeText(LoginActivity.this, "Login success!", Toast.LENGTH_SHORT).show();
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        tilPassword.setError("Incorrect username or account does not exist!");
                    }
                }
            }
        });

        tvRegisterLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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