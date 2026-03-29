package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.FitnessGoal;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserExperience;
import com.hcmute.edu.vn.util.FitnessCalculator;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
    Spinner spinnerActivityLevel;
    LinearLayout layoutTargetWeight;
    EditText edtTargetWeight;
    RadioGroup rgExperience;

    private List<FitnessGoal> fitnessGoalList = new ArrayList<>();
    private List<UserExperience> experienceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_setup);
        EdgeToEdge.enable(this);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(),
                getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        edtFullName = findViewById(R.id.edtFullName);
        edtDOB = findViewById(R.id.edtDOB);
        edtHeight = findViewById(R.id.edtHeight);
        edtWeight = findViewById(R.id.edtWeight);
        rgGender = findViewById(R.id.rgGender);
        btnComplete = findViewById(R.id.btnComplete);
        spinnerFitnessGoal = findViewById(R.id.spinnerFitnessGoal);
        spinnerActivityLevel = findViewById(R.id.spinnerActivityLevel);
        layoutTargetWeight = findViewById(R.id.layoutTargetWeight);
        edtTargetWeight = findViewById(R.id.edtTargetWeight);
        rgExperience = findViewById(R.id.rgExperience);

        // --- Setup Activity Level Spinner ---
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                Arrays.asList(FitnessCalculator.ACTIVITY_LEVEL_LABELS));
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivityLevel.setAdapter(actAdapter);
        spinnerActivityLevel.setSelection(2); // mặc định "Vận động vừa"

        // --- Auto-format ngày sinh ---
        edtDOB.addTextChangedListener(new android.text.TextWatcher() {
            private boolean isFormatting = false;
            private boolean isDeleting = false;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                isDeleting = after == 0;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (isFormatting) return;
                isFormatting = true;
                String digits = s.toString().replaceAll("[^\\d]", "");
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    formatted.append(digits.charAt(i));
                    if ((i == 1 || i == 3) && i < digits.length() - 1) formatted.append("/");
                }
                if (!isDeleting && (digits.length() == 2 || digits.length() == 4))
                    formatted.append("/");
                if (formatted.length() > 10) formatted.delete(10, formatted.length());
                edtDOB.setText(formatted.toString());
                edtDOB.setSelection(formatted.length());
                isFormatting = false;
            }
        });

        receivedUsername = getIntent().getStringExtra("KEY_REGISTER_USER");

        // --- Spinner Fitness Goal ---
        spinnerFitnessGoal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!fitnessGoalList.isEmpty()) {
                    String selectedName = fitnessGoalList.get(position).getName().toLowerCase();
                    boolean isMaintain = selectedName.contains("giữ");

                    // Validation BMI vs goal
                    String hStr = edtHeight.getText().toString().trim();
                    String wStr = edtWeight.getText().toString().trim();
                    if (!hStr.isEmpty() && !wStr.isEmpty()) {
                        try {
                            double currentBmi = calculateBMI(Double.parseDouble(wStr), Double.parseDouble(hStr));
                            if (currentBmi > 24.9 && (isMaintain || selectedName.contains("tăng"))) {
                                Toast.makeText(ProfileSetupActivity.this,
                                        "BMI của bạn đang ở mức Thừa cân. Bạn chỉ nên chọn Giảm mỡ lúc này!",
                                        Toast.LENGTH_LONG).show();
                                selectFirstGoalContaining("giảm");
                                return;
                            }
                            if (currentBmi < 18.5 && (isMaintain || selectedName.contains("giảm"))) {
                                Toast.makeText(ProfileSetupActivity.this,
                                        "BMI của bạn đang ở mức Thiếu cân. Bạn chỉ nên chọn Tăng cơ lúc này!",
                                        Toast.LENGTH_LONG).show();
                                selectFirstGoalContaining("tăng");
                                return;
                            }
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }

                    layoutTargetWeight.setVisibility(isMaintain ? View.GONE : View.VISIBLE);
                    if (isMaintain) edtTargetWeight.setText("");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        btnComplete.setOnClickListener(v -> saveProfileData());
        loadFitnessGoals();
        loadExperiences();
    }

    // =========================================================
    // LOAD FITNESS GOALS
    // =========================================================

    private void loadFitnessGoals() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllFitnessGoals("*").enqueue(new Callback<List<FitnessGoal>>() {
            @Override
            public void onResponse(Call<List<FitnessGoal>> call, Response<List<FitnessGoal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fitnessGoalList = response.body();
                    List<String> goalNames = new ArrayList<>();
                    for (FitnessGoal goal : fitnessGoalList) goalNames.add(goal.getName());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            ProfileSetupActivity.this, android.R.layout.simple_spinner_item, goalNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFitnessGoal.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<FitnessGoal>> call, Throwable t) {
            }
        });
    }

    private void loadExperiences() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllUserExperiences("*").enqueue(new Callback<List<UserExperience>>() {
            @Override
            public void onResponse(Call<List<UserExperience>> call, Response<List<UserExperience>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    experienceList = response.body();
                    rgExperience.removeAllViews();

                    for (int i = 0; i < experienceList.size(); i++) {
                        android.widget.RadioButton rb = new android.widget.RadioButton(ProfileSetupActivity.this);

                        String expName = experienceList.get(i).getUserType();
                        if (expName != null) {
                            if (expName.equalsIgnoreCase("Beginner"))
                                expName = "Người mới";
                            else if (expName.equalsIgnoreCase("Intermediate"))
                                expName = "Đã có kinh nghiệm";
                        }
                        rb.setText(expName);

                        rb.setId(View.generateViewId()); // Tạo ID động
                        rb.setTag(experienceList.get(i).getId()); // Lưu ID database vào Tag

                        // Đảm bảo UI giống file XML
                        android.widget.RadioGroup.LayoutParams params = new android.widget.RadioGroup.LayoutParams(
                                0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                        rb.setLayoutParams(params);
                        rb.setTextSize(15);
                        try {
                            rb.setButtonTintList(android.content.res.ColorStateList
                                    .valueOf(android.graphics.Color.parseColor("#2196F3")));
                        } catch (Exception e) {
                        }

                        rgExperience.addView(rb);
                        if (i == 0)
                            rgExperience.check(rb.getId()); // Check mặc định cái đầu
                    }
                }
            }

            @Override
            public void onFailure(Call<List<UserExperience>> call, Throwable t) {
            }
        });
    }

    // =========================================================
    // SAVE DATA
    // =========================================================

    private void saveProfileData() {
        String fullName = edtFullName.getText().toString().trim();
        String dobInput = edtDOB.getText().toString().trim();
        String heightStr = edtHeight.getText().toString().trim();
        String weightStr = edtWeight.getText().toString().trim();

        if (fullName.isEmpty() || dobInput.isEmpty() || heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(ProfileSetupActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // LẤY EXPERIENCE ID TỪ RADIOGROUP THEO CÁCH CHẮC CHẮN NHẤT
        int extractedExperienceId = 1; // Mặc định

        for (int i = 0; i < rgExperience.getChildCount(); i++) {
            View child = rgExperience.getChildAt(i);
            if (child instanceof android.widget.RadioButton) {
                if (((android.widget.RadioButton) child).isChecked()) {
                    if (child.getTag() != null) {
                        try {
                            extractedExperienceId = Integer.parseInt(child.getTag().toString());
                        } catch (Exception e) {
                        }
                    } else {
                        // Fallback an toàn nếu API lag chưa kịp load tag (tránh hardcode R.id)
                        String btnText = ((android.widget.RadioButton) child).getText().toString().toLowerCase();
                        if (btnText.contains("kin") || btnText.contains("intermediate")) {
                            extractedExperienceId = 2;
                        }
                    }
                    break;
                }
            }
        }

        android.util.Log.d("ProfileSetup", "EXPERIENCE ID ĐÃ CHỌN: " + extractedExperienceId);
        final int finalExperienceId = extractedExperienceId;
        boolean isUserBeginner = (finalExperienceId == 1);

        // Convert DOB
        String dobFormatted = dobInput;
        try {
            if ((dobInput.contains("/") || dobInput.contains("-"))
                    && !dobInput.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String formatPattern = dobInput.contains("/") ? "dd/MM/yyyy" : "dd-MM-yyyy";
                Date date = new SimpleDateFormat(formatPattern, Locale.getDefault()).parse(dobInput);
                if (date != null)
                    dobFormatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ngày sinh không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double height = Double.parseDouble(heightStr);
            double weight = Double.parseDouble(weightStr);

            // Goal
            final int selectedGoalId;
            String selectedGoalName = "";
            if (!fitnessGoalList.isEmpty() && spinnerFitnessGoal.getSelectedItemPosition() >= 0) {
                FitnessGoal g = fitnessGoalList.get(spinnerFitnessGoal.getSelectedItemPosition());
                selectedGoalId = g.getId();
                selectedGoalName = g.getName();
            } else {
                selectedGoalId = 1;
            }

            // Target weight
            Float targetWeightValue = null;
            if (layoutTargetWeight.getVisibility() == View.VISIBLE) {
                String tw = edtTargetWeight.getText().toString().trim();
                if (!tw.isEmpty()) {
                    try {
                        targetWeightValue = Float.parseFloat(tw);
                    } catch (NumberFormatException e) {
                    }
                }
            }

            String gender = (rgGender.getCheckedRadioButtonId() == R.id.rbMale) ? "Male" : "Female";

            // Age
            int age = 20;
            try {
                Calendar dobCal = Calendar.getInstance();
                dobCal.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dobFormatted));
                age = Calendar.getInstance().get(Calendar.YEAR) - dobCal.get(Calendar.YEAR);
            } catch (Exception e) {
            }

            // Activity level
            int activityIndex = spinnerActivityLevel.getSelectedItemPosition();

            double bmi = calculateBMI(weight, height);
            double bmr = FitnessCalculator.calcBMR(weight, height, age, gender);
            double tdee = FitnessCalculator.calcTDEE(bmr, activityIndex);
            double targetW = (targetWeightValue != null && targetWeightValue > 0) ? targetWeightValue : weight;

            boolean isLose = selectedGoalName.toLowerCase().contains("giảm");
            boolean isGain = selectedGoalName.toLowerCase().contains("tăng");
            boolean isMaintain = selectedGoalName.toLowerCase().contains("giữ");

            if (targetWeightValue != null && !isMaintain) {
                double heightM = height / 100.0;
                double targetBmi = targetWeightValue / (heightM * heightM);

                if (isLose) {
                    if (targetWeightValue >= weight) {
                        showError("Cân nặng mục tiêu phải nhỏ hơn hiện tại!");
                        return;
                    }
                    if (targetBmi < 18.5) {
                        showError("Cấm! Mức này quá thấp để giảm. Hãy điều chỉnh lại!");
                        return;
                    }
                } else if (isGain) {
                    if (targetWeightValue <= weight) {
                        showError("Cân nặng mục tiêu phải lớn hơn hiện tại!");
                        return;
                    }
                    if (targetBmi > 23.0) {
                        showError("Cấm! Mức này quá cao để tăng. Hãy điều chỉnh lại!");
                        return;
                    }
                }
            }

            if (!isMaintain && targetWeightValue == null) {
                showError("Vui lòng nhập cân nặng mục tiêu!");
                return;
            }

            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                    .putBoolean("IS_BEGINNER", isUserBeginner)
                    .putInt("ACTIVITY_INDEX", activityIndex)
                    .apply();

            // === CALCULATE ===
            FitnessCalculator.FitnessResult result = FitnessCalculator.calculate(
                    selectedGoalName, weight, targetW, tdee, gender, isUserBeginner);

            // Build User object
            User updateData = new User();
            updateData.setName(fullName);
            updateData.setDateOfBirth(dobFormatted);
            updateData.setGender(gender);
            updateData.setHeight(height);
            updateData.setWeight(weight);
            updateData.setFitnessGoalId(selectedGoalId);
            updateData.setUserExperienceId(finalExperienceId);
            updateData.setTarget(targetWeightValue);
            updateData.setCurrentDailyCalories(result.dailyCalories);
            if (result.targetDate != null)
                updateData.setTargetDate(result.targetDate);

            btnComplete.setEnabled(false);
            btnComplete.setText("Đang lưu...");

            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
            final double finalWeight = weight;
            final double finalHeight = height;

            apiService.getUserByUsername("eq." + receivedUsername, "*").enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        String userId = response.body().get(0).getId();

                        // TÌM GÓI TẬP BẰNG CẢ 2 KHÓA NGOẠI: GOAL VÀ EXPERIENCE
                        apiService.getWorkoutPlanByGoalAndExperience("eq." + selectedGoalId, "eq." + finalExperienceId,
                                "*").enqueue(new Callback<List<WorkoutPlan>>() {
                                    @Override
                                    public void onResponse(Call<List<WorkoutPlan>> planCall,
                                            Response<List<WorkoutPlan>> planResponse) {
                                        if (planResponse.isSuccessful() && planResponse.body() != null
                                                && !planResponse.body().isEmpty()) {
                                            updateData.setCurrentWorkoutPlanId(planResponse.body().get(0).getId());
                                        }

                                        apiService.updateUserProfile("eq." + receivedUsername, updateData)
                                                .enqueue(new Callback<Void>() {
                                                    @Override
                                                    public void onResponse(Call<Void> call,
                                                            Response<Void> profileResponse) {
                                                        if (profileResponse.isSuccessful()) {

                                                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                                                    .putInt("USER_FITNESS_GOAL_ID", selectedGoalId)
                                                                    .putInt("USER_EXPERIENCE_ID", finalExperienceId)
                                                                    .apply();

                                                            double bmiVal = finalWeight
                                                                    / Math.pow(finalHeight / 100.0, 2);
                                                            String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
                                                                    Locale.getDefault()).format(new Date());
                                                            BmiLog log = new BmiLog(UUID.randomUUID().toString(),
                                                                    userId, finalWeight, finalHeight, bmiVal, now);
                                                            apiService.saveBmiLog(log).enqueue(new Callback<Void>() {
                                                                @Override
                                                                public void onResponse(Call<Void> call,
                                                                        Response<Void> logResponse) {
                                                                    goToHome(userId);
                                                                }

                                                                @Override
                                                                public void onFailure(Call<Void> call, Throwable t) {
                                                                    goToHome(userId);
                                                                }
                                                            });
                                                        } else {
                                                            showError("Lỗi cập nhật!");
                                                        }
                                                    }

                                                    @Override
                                                    public void onFailure(Call<Void> call, Throwable t) {
                                                        showError("Lỗi mạng!");
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onFailure(Call<List<WorkoutPlan>> planCall, Throwable t) {
                                        showError("Lỗi lấy kế hoạch tập!");
                                    }
                                });
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) { showError("Lỗi mạng!"); }
            });
        } catch (NumberFormatException e) { showError("Chiều cao, cân nặng phải là số!"); }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void selectFirstGoalContaining(String keyword) {
        for (int i = 0; i < fitnessGoalList.size(); i++) {
            if (fitnessGoalList.get(i).getName().toLowerCase().contains(keyword)) {
                spinnerFitnessGoal.setSelection(i);
                return;
            }
        }
    }

    private double calculateBMI(double weightKg, double heightCm) {
        if (heightCm <= 0) return 0;
        return weightKg / Math.pow(heightCm / 100.0, 2);
    }

    private void showError(String message) {
        btnComplete.setEnabled(true);
        btnComplete.setText("Hoàn Tất");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void goToHome(String userId) {
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        pref.edit().putString("KEY_USER", receivedUsername).putString("KEY_USER_ID", userId).commit();
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}