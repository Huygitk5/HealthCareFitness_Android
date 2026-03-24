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
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
        edtDOB.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            private boolean isDeleting = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { isDeleting = after == 0; }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    formatted.append(digits.charAt(i));
                    if ((i == 1 || i == 3) && i < digits.length() - 1) formatted.append("/");
                }
                if (!isDeleting && (digits.length() == 2 || digits.length() == 4)) formatted.append("/");
                if (formatted.length() > 10) formatted.delete(10, formatted.length());
                edtDOB.setText(formatted.toString());
                edtDOB.setSelection(formatted.length());
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

        loadFitnessGoals();

        spinnerFitnessGoal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!fitnessGoalList.isEmpty()) {
                    String selectedName = fitnessGoalList.get(position).getName().toLowerCase();
                    boolean isLose = selectedName.contains("giảm");
                    boolean isGain = selectedName.contains("tăng");
                    boolean isMaintain = selectedName.contains("giữ");

                    String hStr = edtHeight.getText().toString().trim();
                    String wStr = edtWeight.getText().toString().trim();

                    if (!hStr.isEmpty() && !wStr.isEmpty()) {
                        try {
                            double heightCm = Double.parseDouble(hStr);
                            double weightKg = Double.parseDouble(wStr);
                            double currentBmi = calculateBMI(weightKg, heightCm);

                            if (currentBmi > 24.9 && (isMaintain || isGain)) {
                                Toast.makeText(ProfileSetupActivity.this, "BMI của bạn đang ở mức Thừa cân. Bạn chỉ nên chọn Giảm mỡ lúc này!", Toast.LENGTH_LONG).show();
                                for (int i = 0; i < fitnessGoalList.size(); i++) {
                                    if (fitnessGoalList.get(i).getName().toLowerCase().contains("giảm")) {
                                        spinnerFitnessGoal.setSelection(i); return;
                                    }
                                }
                            }
                            if (currentBmi < 18.5 && (isMaintain || isLose)) {
                                Toast.makeText(ProfileSetupActivity.this, "BMI của bạn đang ở mức Thiếu cân. Bạn chỉ nên chọn Tăng cơ lúc này!", Toast.LENGTH_LONG).show();
                                for (int i = 0; i < fitnessGoalList.size(); i++) {
                                    if (fitnessGoalList.get(i).getName().toLowerCase().contains("tăng")) {
                                        spinnerFitnessGoal.setSelection(i); return;
                                    }
                                }
                            }
                        } catch (NumberFormatException e) { e.printStackTrace(); }
                    }
                    if (isMaintain) {
                        layoutTargetWeight.setVisibility(View.GONE);
                        edtTargetWeight.setText("");
                    } else {
                        layoutTargetWeight.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnComplete.setOnClickListener(v -> saveProfileData());
    }

    private void loadFitnessGoals() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllFitnessGoals("*").enqueue(new Callback<List<FitnessGoal>>() {
            @Override
            public void onResponse(Call<List<FitnessGoal>> call, Response<List<FitnessGoal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fitnessGoalList = response.body();
                    List<String> goalNames = new ArrayList<>();
                    for (FitnessGoal goal : fitnessGoalList) goalNames.add(goal.getName());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(ProfileSetupActivity.this, android.R.layout.simple_spinner_item, goalNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFitnessGoal.setAdapter(adapter);
                }
            }
            @Override public void onFailure(Call<List<FitnessGoal>> call, Throwable t) {}
        });
    }

    private void saveProfileData() {
        String fullName = edtFullName.getText().toString().trim();
        String dobInput = edtDOB.getText().toString().trim();
        String heightStr = edtHeight.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();
        String dobFormatted = dobInput;

        if (!dobInput.isEmpty()) {
            try {
                if ((dobInput.contains("/") || dobInput.contains("-")) && !dobInput.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    String formatPattern = dobInput.contains("/") ? "dd/MM/yyyy" : "dd-MM-yyyy";
                    SimpleDateFormat inputFormat = new SimpleDateFormat(formatPattern, Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = inputFormat.parse(dobInput);
                    if (date != null) dobFormatted = outputFormat.format(date);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Ngày sinh không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        final int selectedGoalId;
        if (!fitnessGoalList.isEmpty() && spinnerFitnessGoal.getSelectedItemPosition() >= 0) {
            selectedGoalId = fitnessGoalList.get(spinnerFitnessGoal.getSelectedItemPosition()).getId();
        } else {
            selectedGoalId = 1;
        }

        Float targetWeightValue = null;
        if (layoutTargetWeight.getVisibility() == View.VISIBLE) {
            String targetWeightStr = edtTargetWeight.getText().toString().trim();
            if (!targetWeightStr.isEmpty()) {
                try { targetWeightValue = Float.parseFloat(targetWeightStr); } catch (NumberFormatException e) {}
            }
        }

        String gender = rgGender.getCheckedRadioButtonId() == R.id.rbMale ? "Male" : "Female";

        if (fullName.isEmpty() || dobFormatted.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(ProfileSetupActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);
            String selectedGoalName = "";
            int age = 20;

            if (!fitnessGoalList.isEmpty() && spinnerFitnessGoal.getSelectedItemPosition() >= 0) {
                selectedGoalName = fitnessGoalList.get(spinnerFitnessGoal.getSelectedItemPosition()).getName().toLowerCase();
                double currentBmi = weight / ((height / 100.0) * (height / 100.0));
                boolean isLose = selectedGoalName.contains("giảm"), isGain = selectedGoalName.contains("tăng"), isMaintain = selectedGoalName.contains("giữ");

                if (isLose && currentBmi < 18.5) { showError("BMI quá thấp, không thể giảm cân!"); return; }
                if (isGain && currentBmi > 23.0) { showError("BMI cao, không nên tăng cân!"); return; }
                if (targetWeightValue != null && !isMaintain) {
                    double targetWeight = targetWeightValue;
                    if (isLose && targetWeight >= weight) { showError("Cân nặng mục tiêu phải nhỏ hơn hiện tại!"); return; }
                    if (isGain && targetWeight <= weight) { showError("Cân nặng mục tiêu phải lớn hơn hiện tại!"); return; }
                }
            }

            User updateData = new User();
            updateData.setName(fullName);
            updateData.setDateOfBirth(dobFormatted);
            updateData.setGender(gender);
            updateData.setHeight(height);
            updateData.setWeight(weight);
            updateData.setFitnessGoalId(selectedGoalId);
            updateData.setTarget(targetWeightValue);

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Calendar calDOB = Calendar.getInstance(); calDOB.setTime(sdf.parse(dobFormatted));
                age = Calendar.getInstance().get(Calendar.YEAR) - calDOB.get(Calendar.YEAR);
            } catch (Exception e) {}

            calculateFitnessMetrics(updateData, weight, height, age, gender, selectedGoalName, targetWeightValue != null ? targetWeightValue.doubleValue() : weight);

            btnComplete.setEnabled(false); btnComplete.setText("Đang lưu...");
            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

            apiService.getUserByUsername("eq." + receivedUsername, "*").enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        String userId = response.body().get(0).getId();
                        
                        // LẤY GÓI TẬP ĐÚNG THEO MỤC TIÊU VÀ LƯU VÀO USER NGAY LÚC SETUP PROFILE
                        apiService.getWorkoutPlanByGoalId("eq." + selectedGoalId, "*").enqueue(new Callback<List<WorkoutPlan>>() {
                            @Override
                            public void onResponse(Call<List<WorkoutPlan>> planCall, Response<List<WorkoutPlan>> planResponse) {
                                if (planResponse.isSuccessful() && planResponse.body() != null && !planResponse.body().isEmpty()) {
                                    updateData.setCurrentWorkoutPlanId(planResponse.body().get(0).getId());
                                }
                                
                                apiService.updateUserProfile("eq." + receivedUsername, updateData).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> profileResponse) {
                                if (profileResponse.isSuccessful()) {
                                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                            .putInt("USER_FITNESS_GOAL_ID", selectedGoalId)
                                            .apply();

                                    String currentDateFull = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                                    double currentBmi = weight / ((height / 100.0) * (height / 100.0));
                                    BmiLog newLog = new BmiLog(UUID.randomUUID().toString(), userId, weight, height, currentBmi, currentDateFull);

                                    apiService.saveBmiLog(newLog).enqueue(new Callback<Void>() {
                                        @Override public void onResponse(Call<Void> call, Response<Void> logResponse) { goToHome(userId); }
                                        @Override public void onFailure(Call<Void> call, Throwable t) { goToHome(userId); }
                                    });
                                } else { showError("Lỗi cập nhật!"); }
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) { showError("Lỗi mạng!"); }
                        });
                        
                            }
                            @Override
                            public void onFailure(Call<List<WorkoutPlan>> planCall, Throwable t) {
                                showError("Lỗi lấy kế hoạch tập!");
                            }
                        }); // Kết thúc block getWorkoutPlanByGoalId
                    }
                }
                @Override public void onFailure(Call<List<User>> call, Throwable t) { showError("Lỗi mạng!"); }
            });
        } catch (NumberFormatException e) { showError("Chiều cao, cân nặng phải là số!"); }
    }

    private void showError(String message) {
        btnComplete.setEnabled(true); btnComplete.setText("Hoàn Tất");
        Toast.makeText(ProfileSetupActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void goToHome(String userId) {
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().putString("KEY_USER", receivedUsername).putString("KEY_USER_ID", userId).commit();
        startActivity(new Intent(ProfileSetupActivity.this, HomeActivity.class));
        finish();
    }

    private void calculateFitnessMetrics(User updateData, double weight, double height, int age, String gender, String goalName, Double targetWeight) {
        double bmr = ("Male".equalsIgnoreCase(gender)) ? (10 * weight) + (6.25 * height) - (5 * age) + 5 : (10 * weight) + (6.25 * height) - (5 * age) - 161;
        double tdee = bmr * 1.55;
        double dailyCalories = tdee;
        int weeksToTarget = 0;

        if (goalName.contains("giảm")) {
            weeksToTarget = (int) Math.ceil((weight - targetWeight) / (weight * 0.0075));
            dailyCalories = tdee - 500;
        } else if (goalName.contains("tăng")) {
            weeksToTarget = (int) Math.ceil((targetWeight - weight) / (weight * 0.005));
            dailyCalories = tdee + 300;
        }

        if (weeksToTarget > 0) {
            Calendar c = Calendar.getInstance(); c.add(Calendar.WEEK_OF_YEAR, weeksToTarget);
            updateData.setTargetDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.getTime()));
        }
        updateData.setCurrentDailyCalories(dailyCalories);
    }

    private double calculateBMI(double weightKg, double heightCm) { return (heightCm <= 0) ? 0 : weightKg / ((heightCm / 100.0) * (heightCm / 100.0)); }
}