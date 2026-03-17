package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.FitnessGoal;
import com.hcmute.edu.vn.model.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileSetupActivity extends AppCompatActivity {

    EditText edtFullName, edtDOB, edtHeight, edtWeight;
    RadioGroup rgGender;
    Button btnComplete;
    String receivedUsername;

    Spinner spinnerFitnessGoal;
    LinearLayout layoutTargetWeight;
    EditText edtTargetWeight;

    private List<FitnessGoal> fitnessGoalList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);
        androidx.activity.EdgeToEdge.enable(this);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        edtFullName = findViewById(R.id.edtFullName);
        edtDOB = findViewById(R.id.edtDOB);
        // --- TEXTWATCHER TỰ ĐỘNG ĐIỀN DẤU "/" NGAY LẬP TỨC ---
        edtDOB.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            private boolean isDeleting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nếu 'after' == 0 nghĩa là người dùng đang bấm nút xóa (Backspace)
                isDeleting = after == 0;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                // Xóa hết các ký tự không phải số
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();

                for (int i = 0; i < digits.length(); i++) {
                    formatted.append(digits.charAt(i));
                    // Chèn dấu "/" NẾU đã duyệt qua số thứ 2 hoặc thứ 4 (và chưa phải là số cuối cùng)
                    if ((i == 1 || i == 3) && i < digits.length() - 1) {
                        formatted.append("/");
                    }
                }

                // LOGIC QUAN TRỌNG: Tự động chèn "/" ngay khi vừa gõ đủ 2 hoặc 4 số (VÀ không phải đang xóa)
                if (!isDeleting && (digits.length() == 2 || digits.length() == 4)) {
                    formatted.append("/");
                }

                // Cắt bỏ phần dư nếu gõ quá 10 ký tự
                if (formatted.length() > 10) {
                    formatted.delete(10, formatted.length());
                }

                edtDOB.setText(formatted.toString());
                edtDOB.setSelection(formatted.length()); // Đẩy con trỏ nháy về cuối dòng

                isFormatting = false;
            }
        });
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        rgGender = findViewById(R.id.rgGender);
        btnComplete = findViewById(R.id.btnComplete);

        spinnerFitnessGoal = findViewById(R.id.spinnerFitnessGoal);
        layoutTargetWeight = findViewById(R.id.layoutTargetWeight);
        edtTargetWeight = findViewById(R.id.edtTargetWeight);

        Intent intent = getIntent();
        receivedUsername = intent.getStringExtra("KEY_REGISTER_USER");

        // Gọi hàm tải Fitness Goals từ Supabase
        loadFitnessGoals();

        // Lắng nghe sự kiện chọn Goal
        spinnerFitnessGoal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!fitnessGoalList.isEmpty()) {
                    // Lấy tên của mục tiêu được chọn
                    String selectedName = fitnessGoalList.get(position).getName().toLowerCase();

                    // Nếu mục tiêu là "Duy trì" thì ẩn ô nhập cân nặng mục tiêu
                    if (selectedName.contains("duy trì") || selectedName.contains("maintain")) {
                        layoutTargetWeight.setVisibility(View.GONE);
                        edtTargetWeight.setText(""); // Xóa dữ liệu cũ nếu có
                    } else {
                        // Nếu là Giảm cân hoặc Tăng cân -> Hiện ô nhập cân nặng
                        layoutTargetWeight.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveProfileData();
            }
        });
    }

    // Hàm gọi API lấy danh sách Fitness Goals
    private void loadFitnessGoals() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllFitnessGoals("*").enqueue(new Callback<List<FitnessGoal>>() {
            @Override
            public void onResponse(Call<List<FitnessGoal>> call, Response<List<FitnessGoal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fitnessGoalList = response.body();

                    // Chuyển danh sách Object thành danh sách String (Tên goal) để đưa vào Spinner
                    List<String> goalNames = new ArrayList<>();
                    for (FitnessGoal goal : fitnessGoalList) {
                        goalNames.add(goal.getName());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ProfileSetupActivity.this, android.R.layout.simple_spinner_item, goalNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFitnessGoal.setAdapter(adapter);
                } else {
                    Toast.makeText(ProfileSetupActivity.this, "Không thể tải danh sách mục tiêu!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<FitnessGoal>> call, Throwable t) {
                Toast.makeText(ProfileSetupActivity.this, "Lỗi kết nối khi tải mục tiêu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfileData() {
        String fullName = edtFullName.getText().toString().trim();
        String dobInput = edtDOB.getText().toString().trim();
        String heightStr = edtHeight.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();
        String dobFormatted = dobInput;

        // --- TỰ ĐỘNG CHUYỂN ĐỔI NGÀY SINH ---
        if (!dobInput.isEmpty()) {
            try {
                // Nếu user nhập chuẩn Việt Nam chứa "/" hoặc "-" (VD: 23-01-2003)
                if ((dobInput.contains("/") || dobInput.contains("-")) && !dobInput.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    String formatPattern = dobInput.contains("/") ? "dd/MM/yyyy" : "dd-MM-yyyy";
                    SimpleDateFormat inputFormat = new SimpleDateFormat(formatPattern, Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    Date date = inputFormat.parse(dobInput);
                    if (date != null) {
                        dobFormatted = outputFormat.format(date); // Chuyển sang YYYY-MM-DD
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ngày sinh không hợp lệ. Vui lòng nhập DD-MM-YYYY", Toast.LENGTH_SHORT).show();
                return; // Dừng lại không gọi API nếu nhập sai định dạng
            }
        }

        // Lấy ID thật sự của Goal từ Database thông qua danh sách đã tải
        int selectedGoalId = 1; // Giá trị mặc định
        if (!fitnessGoalList.isEmpty() && spinnerFitnessGoal.getSelectedItemPosition() >= 0) {
            selectedGoalId = fitnessGoalList.get(spinnerFitnessGoal.getSelectedItemPosition()).getId();
        }

        // Lấy Target Weight (nếu đang hiển thị)
        Float targetWeightValue = null;
        if (layoutTargetWeight.getVisibility() == View.VISIBLE) {
            String targetWeightStr = edtTargetWeight.getText().toString().trim();
            if (!targetWeightStr.isEmpty()) {
                try {
                    targetWeightValue = Float.parseFloat(targetWeightStr);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }

        String gender = "Other";
        int selectedId = rgGender.getCheckedRadioButtonId();
        if (selectedId == R.id.rbMale) gender = "Male";
        else if (selectedId == R.id.rbFemale) gender = "Female";

        if (fullName.isEmpty() || dobFormatted.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(ProfileSetupActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);
            if (!fitnessGoalList.isEmpty() && spinnerFitnessGoal.getSelectedItemPosition() >= 0) {
                String selectedGoalName = fitnessGoalList.get(spinnerFitnessGoal.getSelectedItemPosition()).getName().toLowerCase();

                double heightM = height / 100.0;
                double currentBmi = weight / (heightM * heightM);

                // --- RÀO CHẮN 1: KIỂM TRA TÍNH HỢP LÝ CỦA MỤC TIÊU DỰA TRÊN THỂ TRẠNG HIỆN TẠI ---
                if ((selectedGoalName.contains("giảm mỡ") || selectedGoalName.contains("lose fat")) && currentBmi < 18.5) {
                    Toast.makeText(this, "Bạn đang thiếu cân (BMI < 18.5), không thể chọn Giảm mỡ. Hãy chọn Tăng cơ nhé!", Toast.LENGTH_LONG).show();
                    spinnerFitnessGoal.requestFocus();
                    return;
                }

                if ((selectedGoalName.contains("tăng cơ") || selectedGoalName.contains("build muscle")) && currentBmi > 23.0) {
                    Toast.makeText(this, "Bạn đang thừa cân (BMI > 23.0), không nên Tăng cơ lúc này. Hãy chọn Giảm mỡ nhé!", Toast.LENGTH_LONG).show();
                    spinnerFitnessGoal.requestFocus();
                    return;
                }

                if ((selectedGoalName.contains("duy trì") || selectedGoalName.contains("maintain")) && (currentBmi < 18.5 || currentBmi > 23.0)) {
                    Toast.makeText(this, "BMI hiện tại không nằm trong mức chuẩn (18.5 - 23.0). Không nên chọn Duy trì vóc dáng lúc này!", Toast.LENGTH_LONG).show();
                    spinnerFitnessGoal.requestFocus();
                    return;
                }

                // --- RÀO CHẮN 2 & 3: KIỂM TRA CÂN NẶNG ĐÍCH (Chỉ áp dụng khi không phải là Giữ dáng) ---
                if (targetWeightValue != null && (!selectedGoalName.contains("duy trì") && !selectedGoalName.contains("maintain"))) {
                    double targetBmi = targetWeightValue / (heightM * heightM);

                    // Nếu chọn Giảm mỡ
                    if (selectedGoalName.contains("giảm mỡ") || selectedGoalName.contains("lose fat")) {
                        if (targetWeightValue >= weight) {
                            edtTargetWeight.setError("Để giảm mỡ, cân nặng mục tiêu phải < cân nặng hiện tại!");
                            edtTargetWeight.requestFocus();
                            return;
                        }
                        if (targetBmi < 18.5) {
                            edtTargetWeight.setError("Cấm! Mức này quá thấp (BMI < 18.5). Hãy điều chỉnh lại mục tiêu an toàn hơn (Target BMI: 18.5 - 23.0).");
                            edtTargetWeight.requestFocus();
                            return;
                        }
                    }
                    // Nếu chọn Tăng cơ
                    else if (selectedGoalName.contains("tăng cơ") || selectedGoalName.contains("build muscle")) {
                        if (targetWeightValue <= weight) {
                            edtTargetWeight.setError("Để tăng cơ, cân nặng mục tiêu phải > cân nặng hiện tại!");
                            edtTargetWeight.requestFocus();
                            return;
                        }
                        if (targetBmi > 23.0) {
                            edtTargetWeight.setError("Cấm! Mức này quá cao (BMI > 23.0). Hãy điều chỉnh lại mục tiêu an toàn hơn (Target BMI: 18.5 - 23.0).");
                            edtTargetWeight.requestFocus();
                            return;
                        }
                    }
                }
            }
            // ===============================================================

            User updateData = new User();
            updateData.setName(fullName);
            updateData.setDateOfBirth(dobFormatted);
            updateData.setGender(gender);
            updateData.setHeight(height);
            updateData.setWeight(weight);
            updateData.setFitnessGoalId(selectedGoalId);
            updateData.setTarget(targetWeightValue);

            btnComplete.setEnabled(false);
            btnComplete.setText("Đang lưu...");

            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

            // ===============================================================
            // BƯỚC 1: TÌM USER ĐỂ LẤY ID (UUID)
            // ===============================================================
            apiService.getUserByUsername("eq." + receivedUsername, "*").enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        String userId = response.body().get(0).getId();

                        // ===============================================================
                        // BƯỚC 2: CẬP NHẬT PROFILE
                        // ===============================================================
                        apiService.updateUserProfile("eq." + receivedUsername, updateData).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> profileResponse) {
                                if (profileResponse.isSuccessful()) {

                                    // ===============================================================
                                    // BƯỚC 3: TÍNH VÀ LƯU BMI VÀO DATABASE CHO BIỂU ĐỒ
                                    // ===============================================================
                                    String currentDateFull = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                                    double currentBmi = weight / ((height / 100.0) * (height / 100.0));
                                    BmiLog newLog = new BmiLog(UUID.randomUUID().toString(), userId, weight, height, currentBmi, currentDateFull);

                                    apiService.saveBmiLog(newLog).enqueue(new Callback<Void>() {
                                        @Override
                                        public void onResponse(Call<Void> call, Response<Void> logResponse) {
                                            goToHome(); // Lưu log thành công -> Xong xuôi!
                                        }

                                        @Override
                                        public void onFailure(Call<Void> call, Throwable t) {
                                            goToHome(); // Rớt mạng khúc lưu log thì vẫn cho vào trang Home
                                        }
                                    });

                                } else {
                                    try {
                                        // In thẳng lỗi ra màn hình nếu database chửi
                                        String errorDetail = profileResponse.errorBody().string();
                                        showError("Lỗi DB: " + errorDetail);
                                    } catch (Exception e) {
                                        showError("Lỗi cập nhật thông tin!");
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                showError("Lỗi kết nối mạng khi cập nhật!");
                            }
                        });

                    } else {
                        showError("Không tìm thấy tài khoản để cập nhật!");
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    showError("Lỗi mạng khi tìm thông tin tài khoản!");
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(ProfileSetupActivity.this, "Chiều cao, cân nặng phải là số!", Toast.LENGTH_SHORT).show();
        }
    }

    // Hàm phụ trợ để báo lỗi và mở lại nút bấm
    private void showError(String message) {
        btnComplete.setEnabled(true);
        btnComplete.setText("Hoàn Tất");
        Toast.makeText(ProfileSetupActivity.this, message, Toast.LENGTH_LONG).show();
    }

    // Hàm phụ trợ để chuyển sang Home
    private void goToHome() {
        Toast.makeText(ProfileSetupActivity.this, "Thiết lập thành công!", Toast.LENGTH_SHORT).show();

        // Ép lưu username vào SharedPreferences BẰNG LỆNH COMMIT() để Home tải được data ngay lập tức
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().putString("KEY_USER", receivedUsername).commit();

        Intent i = new Intent(ProfileSetupActivity.this, HomeActivity.class);
        startActivity(i);
        finish();
    }
}