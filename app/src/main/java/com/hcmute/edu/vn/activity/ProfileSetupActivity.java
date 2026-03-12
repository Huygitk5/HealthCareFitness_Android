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
import com.hcmute.edu.vn.database.DatabaseHelper;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText edtFullName, edtDOB, edtHeight, edtWeight;
    RadioGroup rgGender;
    Button btnComplete;
    DatabaseHelper dbHelper;
    String receivedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);

        dbHelper = new DatabaseHelper(this);

        edtFullName = findViewById(R.id.edtFullName);
        edtDOB = findViewById(R.id.edtDOB);
        // ĐÃ XÓA: edtAddress = findViewById(R.id.edtAddress);
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
                String dob = edtDOB.getText().toString().trim();
                String heightStr = edtHeight.getText().toString().trim();
                String weightStr = edtWeight.getText().toString().trim();

                String gender = "Other";
                int selectedId = rgGender.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMale) gender = "Male";
                else if (selectedId == R.id.rbFemale) gender = "Female";

                // ĐÃ XÓA address khỏi điều kiện check rỗng
                if (fullName.isEmpty() || dob.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(ProfileSetupActivity.this, "Please fill in all the information", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        // Ép kiểu chuỗi thành số thực an toàn
                        double height = Double.parseDouble(heightStr);
                        double weight = Double.parseDouble(weightStr);

                        boolean checkUpdate = dbHelper.updateUserProfile(receivedUsername, fullName, dob, gender, height, weight, null);

                        if(checkUpdate) {
                            Toast.makeText(ProfileSetupActivity.this, "Setup Success!", Toast.LENGTH_LONG).show();

                            // Lưu lại phiên đăng nhập để các màn hình khác nhận diện được user
                            android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            android.content.SharedPreferences.Editor editor = pref.edit();
                            editor.putString("KEY_USER", receivedUsername);
                            editor.apply();

                            Intent i = new Intent(ProfileSetupActivity.this, HomeActivity.class);
                            i.putExtra("KEY_USER", receivedUsername);
                            startActivity(i);
                            finish();
                        } else {
                            Toast.makeText(ProfileSetupActivity.this, "Error updating profile!", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(ProfileSetupActivity.this, "Invalid Height or Weight format!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}