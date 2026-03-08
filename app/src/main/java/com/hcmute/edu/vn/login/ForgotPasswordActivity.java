package com.hcmute.edu.vn.login;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    EditText edtEmail;
    Button btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgot_password);

        edtEmail = findViewById(R.id.edtEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);

        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmail.getText().toString().trim();

                if (email.isEmpty()) {
                    edtEmail.setError("Email cannot be empty");
                    edtEmail.requestFocus();
                    return;
                }

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    edtEmail.setError("Please enter a valid email address");
                    edtEmail.requestFocus();
                    return;
                }

                Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();

                finish();
            }
        });
    }
}