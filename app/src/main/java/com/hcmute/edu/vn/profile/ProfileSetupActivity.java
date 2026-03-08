package com.hcmute.edu.vn.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.DatabaseHelper;
import com.hcmute.edu.vn.home.activity.HomeActivity;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText edtFullName, edtDOB, edtAddress, edtHeight, edtWeight;
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
        edtAddress = findViewById(R.id.edtAddress);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        rgGender = findViewById(R.id.rgGender);
        btnComplete = findViewById(R.id.btnComplete);

        Intent intent = getIntent();
        receivedUsername = intent.getStringExtra("KEY_REGISTER_USER");

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullName = edtFullName.getText().toString();
                String dob = edtDOB.getText().toString();
                String address = edtAddress.getText().toString();
                String heightStr = edtHeight.getText().toString().trim();
                String weightStr = edtWeight.getText().toString().trim();

                String gender = "Other";
                int selectedId = rgGender.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMale) gender = "Male";
                else if (selectedId == R.id.rbFemale) gender = "Female";

                if (fullName.isEmpty() || dob.isEmpty() || address.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(ProfileSetupActivity.this, "Please fill in all the information", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        // Ép kiểu chuỗi thành số thực an toàn
                        double height = Double.parseDouble(heightStr);
                        double weight = Double.parseDouble(weightStr);

                        boolean checkUpdate = dbHelper.updateUserProfile(receivedUsername, fullName, dob, gender, address, height, weight);

                        if(checkUpdate) {
                            Toast.makeText(ProfileSetupActivity.this, "Setup Success!", Toast.LENGTH_LONG).show();
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