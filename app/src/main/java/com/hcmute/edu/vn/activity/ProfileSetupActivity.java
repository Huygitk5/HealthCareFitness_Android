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
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.activity.HomeActivity;
import com.hcmute.edu.vn.model.User;

import retrofit2.Call;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText edtFullName, edtDOB, edtAddress, edtHeight, edtWeight;
    RadioGroup rgGender;
    Button btnComplete;
    String receivedUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);

        edtFullName = findViewById(R.id.edtFullName);
        edtDOB = findViewById(R.id.edtDOB);
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
                String dob = edtDOB.getText().toString().trim(); // Yêu cầu user nhập YYYY-MM-DD
                String heightStr = edtHeight.getText().toString().trim();
                String weightStr = edtWeight.getText().toString().trim();

                String gender = "Other";
                int selectedId = rgGender.getCheckedRadioButtonId();
                if (selectedId == R.id.rbMale) gender = "Male";
                else if (selectedId == R.id.rbFemale) gender = "Female";

                if (fullName.isEmpty() || dob.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(ProfileSetupActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    double height = Double.parseDouble(heightStr);
                    double weight = Double.parseDouble(weightStr);

                    // Tạo Object User CHỈ chứa các trường cần update
                    User updateData = new User();
                    updateData.setName(fullName);
                    updateData.setDateOfBirth(dob); // Lưu ý format "YYYY-MM-DD"
                    updateData.setGender(gender);
                    updateData.setHeight(height);
                    updateData.setWeight(weight);

                    btnComplete.setEnabled(false);
                    btnComplete.setText("Đang lưu...");

                    // Gọi API PATCH của Supabase (như hướng dẫn ở các tin nhắn trước)
                    SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
                    apiService.updateUserProfile("eq." + receivedUsername, updateData).enqueue(new retrofit2.Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, retrofit2.Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(ProfileSetupActivity.this, "Cập nhật thành công!", Toast.LENGTH_LONG).show();
                                Intent i = new Intent(ProfileSetupActivity.this, HomeActivity.class);
                                i.putExtra("KEY_USER", receivedUsername);
                                startActivity(i);
                                finish();
                            } else {
                                btnComplete.setEnabled(true);
                                btnComplete.setText("Hoàn Tất");
                                Toast.makeText(ProfileSetupActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            btnComplete.setEnabled(true);
                            btnComplete.setText("Hoàn Tất");
                            Toast.makeText(ProfileSetupActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (NumberFormatException e) {
                    Toast.makeText(ProfileSetupActivity.this, "Chiều cao, cân nặng phải là số!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}