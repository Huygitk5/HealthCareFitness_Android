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
import com.hcmute.edu.vn.home.DatabaseHelper;
import com.hcmute.edu.vn.home.activity.HomeActivity;
import com.hcmute.edu.vn.home.model.User;
import com.hcmute.edu.vn.profile.ProfileSetupActivity;

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
                String username = edtUser.getText().toString();
                String password = edtPass.getText().toString();

                tilPassword.setError(null);

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter all the information!", Toast.LENGTH_SHORT).show();
                } else {
                    if (dbHelper.checkLogin(username, password)) {
                        User currentUser = dbHelper.getUserDetails(username);
                        Intent intent;
                        if (currentUser.getFullName().isEmpty()) {
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
                        tilPassword.setError("Incorrect username or password!");
                    }
                }
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