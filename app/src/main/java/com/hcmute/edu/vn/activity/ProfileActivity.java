package com.hcmute.edu.vn.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.FitnessGoal;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserMedicalCondition;
import com.hcmute.edu.vn.model.UserMedicalConditionInsert;
import com.hcmute.edu.vn.util.FitnessCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileActivity extends AppCompatActivity {

    TextView txtName, txtEmail, tvProfileAge, tvProfileWeight, tvProfileHeight;
    TextView tvMedicalHistory, tvAllergies, btnUpdateMedical;
    MaterialCardView cardMedicalHistory, cardAllergies;
    MaterialButton btnLogout;

    String username;
    String currentUserId;
    List<Integer> currentConditionIds = new ArrayList<>();

    TextView tvProfileGoal, tvProfileTargetWeight, btnEditGoal;
    List<FitnessGoal> fitnessGoalList = new ArrayList<>();
    Integer currentGoalId = 1;
    Float currentTargetWeight = null;

    Double currentHeight = 0.0;
    Double currentWeight = 0.0;
    String currentGender = "Male";
    int currentAge = 20;

    private int currentActivityIndex = 2; // mặc định "Vận động vừa"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        username = pref.getString("KEY_USER", null);

        // Ánh xạ
        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        tvProfileAge = findViewById(R.id.tvProfileAge);
        tvProfileWeight = findViewById(R.id.tvProfileWeight);
        tvProfileHeight = findViewById(R.id.tvProfileHeight);
        tvMedicalHistory = findViewById(R.id.tvMedicalHistory);
        tvAllergies = findViewById(R.id.tvAllergies);
        btnUpdateMedical = findViewById(R.id.btnUpdateMedical);
        btnLogout = findViewById(R.id.btnLogout);

        cardMedicalHistory = findViewById(R.id.cardMedicalHistory);
        cardAllergies = findViewById(R.id.cardAllergies);

        tvProfileGoal = findViewById(R.id.tvProfileGoal);
        tvProfileTargetWeight = findViewById(R.id.tvProfileTargetWeight);
        btnEditGoal = findViewById(R.id.btnEditGoal);

        // Xử lý nút Switch Nhắc nhở uống nước
        SwitchCompat switchWater = findViewById(R.id.switchWaterReminder);
        boolean isWaterReminderOn = pref.getBoolean("WATER_REMINDER", false);
        switchWater.setChecked(isWaterReminderOn);

        switchWater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.edit().putBoolean("WATER_REMINDER", isChecked).apply();

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
                    }
                }
                setupWaterReminder(true);
                Toast.makeText(this, "Đã bật nhắc nhở uống nước!", Toast.LENGTH_SHORT).show();
            } else {
                setupWaterReminder(false);
                Toast.makeText(this, "Đã tắt nhắc nhở uống nước", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý nút Switch Nhắc nhở luyện tập
        SwitchCompat switchWorkout = findViewById(R.id.switchWorkoutReminder);
        boolean isWorkoutReminderOn = pref.getBoolean("WORKOUT_REMINDER", false);
        switchWorkout.setChecked(isWorkoutReminderOn);

        switchWorkout.setOnCheckedChangeListener((buttonView, isChecked) -> {
            pref.edit().putBoolean("WORKOUT_REMINDER", isChecked).apply();

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 102);
                    }
                }
                setupWorkoutReminder(true);
                Toast.makeText(this, "Đã bật nhắc nhở lúc 17:00 hằng ngày!", Toast.LENGTH_SHORT).show();
            } else {
                setupWorkoutReminder(false);
                Toast.makeText(this, "Đã tắt nhắc nhở luyện tập", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            Intent loginIntent = new Intent(ProfileActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnEditGoal.setOnClickListener(v -> showEditGoalDialog());
        btnUpdateMedical.setOnClickListener(v -> showMedicalConditionDialog());
        loadFitnessGoalsList();
        setupBottomNavigation();
    }

    // ==============================================================
    // HÀM HIỂN THỊ DIALOG ĐỔI MỤC TIÊU (ĐÃ TÍCH HỢP 3 RÀO CHẮN BẢO VỆ)
    // ==============================================================
    private void showEditGoalDialog() {
        if (fitnessGoalList.isEmpty()) {
            Toast.makeText(this, "Đang tải dữ liệu, vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_edit_goal, null);
        Spinner dialogSpinnerGoal = dialogView.findViewById(R.id.dialogSpinnerGoal);
        Spinner dialogSpinnerActivity = dialogView.findViewById(R.id.dialogSpinnerActivity);
        LinearLayout dialogLayoutTarget = dialogView.findViewById(R.id.dialogLayoutTarget);
        EditText dialogEdtTarget = dialogView.findViewById(R.id.dialogEdtTarget);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancelGoal);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnDialogSaveGoal);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // --- Setup Goal Spinner ---
        List<String> goalNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < fitnessGoalList.size(); i++) {
            goalNames.add(fitnessGoalList.get(i).getName());
            if (fitnessGoalList.get(i).getId() == currentGoalId) selectedIndex = i;
        }
        ArrayAdapter<String> goalAdapter  = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, goalNames);
        goalAdapter .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogSpinnerGoal.setAdapter(goalAdapter );
        dialogSpinnerGoal.setSelection(selectedIndex);

        // --- Setup Activity Level Spinner ---
        ArrayAdapter<String> actAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                Arrays.asList(FitnessCalculator.ACTIVITY_LEVEL_LABELS));
        actAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogSpinnerActivity.setAdapter(actAdapter);
        dialogSpinnerActivity.setSelection(currentActivityIndex);

        // --- Pre-fill target weight ---
        if (currentTargetWeight != null && currentTargetWeight > 0)
            dialogEdtTarget.setText(String.valueOf(currentTargetWeight));

        // --- Goal spinner listener (ẩn/hiện ô cân nặng mục tiêu + validate BMI) ---
        final int finalSelectedIndex = selectedIndex;
        dialogSpinnerGoal.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedName = goalNames.get(position).toLowerCase();
                boolean isMaintain = selectedName.contains("giữ");

                // BMI guard (chỉ check khi đổi sang goal khác)
                if (position != finalSelectedIndex && currentHeight != null && currentHeight > 0
                        && currentWeight != null && currentWeight > 0) {
                    double currentBmi = currentWeight / Math.pow(currentHeight / 100.0, 2);
                    if (currentBmi > 24.9 && (goalNames.contains("tăng") || isMaintain)) {
                        Toast.makeText(ProfileActivity.this,
                                "Bạn đang thừa cân (BMI > 24.9), chỉ nên chọn Giảm mỡ lúc này!", Toast.LENGTH_LONG).show();
                        dialogSpinnerGoal.setSelection(finalSelectedIndex);
                        return;
                    }
                    if (currentBmi < 18.5 && (goalNames.contains("giảm") || isMaintain)) {
                        Toast.makeText(ProfileActivity.this,
                                "Bạn đang thiếu cân (BMI < 18.5), chỉ nên chọn Tăng cơ lúc này!", Toast.LENGTH_LONG).show();
                        dialogSpinnerGoal.setSelection(finalSelectedIndex);
                        return;
                    }
                }

                dialogLayoutTarget.setVisibility(isMaintain ? View.GONE : View.VISIBLE);
                if (isMaintain) dialogEdtTarget.setText("");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int newGoalPosition  = dialogSpinnerGoal.getSelectedItemPosition();
            int newActivityIndex = dialogSpinnerActivity.getSelectedItemPosition();
            int newGoalId = fitnessGoalList.get(newGoalPosition).getId();
            String selectedGoalName = fitnessGoalList.get(newGoalPosition).getName();

            Float newTarget = null;
            if (dialogLayoutTarget.getVisibility() == View.VISIBLE) {
                String targetStr = dialogEdtTarget.getText().toString().trim();
                if (targetStr.isEmpty()) {
                    dialogEdtTarget.setError("Vui lòng nhập cân nặng mục tiêu!");
                    dialogEdtTarget.requestFocus();
                    return;
                }
                try { newTarget = Float.parseFloat(targetStr); }
                catch (NumberFormatException e) {
                    Toast.makeText(this, "Cân nặng phải là số!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // --- Validate target vs current weight ---
            boolean isLose = selectedGoalName.toLowerCase().contains("giảm");
            boolean isGain = selectedGoalName.toLowerCase().contains("tăng");
            if (newTarget != null && currentHeight != null && currentHeight > 0) {
                double targetBmi = newTarget / Math.pow(currentHeight / 100.0, 2);

                if (isLose) {
                    if (newTarget >= currentWeight) {
                        dialogEdtTarget.setError("Phải nhỏ hơn cân nặng hiện tại!");
                        dialogEdtTarget.requestFocus(); return;
                    }
                    if (targetBmi < 18.5) {
                        dialogEdtTarget.setError("Cấm! Mức này quá thấp (BMI < 18.5). Hãy chỉnh lại!");
                        dialogEdtTarget.requestFocus(); return;
                    }
                }
                if (isGain) {
                    if (newTarget <= currentWeight) {
                        dialogEdtTarget.setError("Phải lớn hơn cân nặng hiện tại!");
                        dialogEdtTarget.requestFocus(); return;
                    }
                    if (targetBmi > 23.0) {
                        dialogEdtTarget.setError("Cấm! Mức này quá cao (BMI > 23.0). Hãy chỉnh lại!");
                        dialogEdtTarget.requestFocus(); return;
                    }
                }
            }

            // === TÍNH TOÁN VỚI FitnessCalculator ===
            double bmr = FitnessCalculator.calcBMR(
                    currentWeight != null ? currentWeight : 60,
                    currentHeight != null ? currentHeight : 165,
                    currentAge, currentGender);

            double tdee = FitnessCalculator.calcTDEE(bmr, newActivityIndex);
            double targetW = (newTarget != null) ? newTarget : (currentWeight != null ? currentWeight : 60);
            boolean isUserBeginner = getSharedPreferences("UserPrefs", MODE_PRIVATE).getBoolean("IS_BEGINNER", true);
            FitnessCalculator.FitnessResult result =
                    FitnessCalculator.calculate(selectedGoalName, currentWeight != null ? currentWeight : 60,
                            targetW, tdee, currentGender, isUserBeginner);

            // Build update payload
            User updateData = new User();
            updateData.setFitnessGoalId(newGoalId);
            updateData.setTarget(newTarget);
            updateData.setCurrentDailyCalories(result.dailyCalories);
            updateData.setCurrentWorkoutPlanId(result.workoutPlanId);
            if (result.targetDate != null) updateData.setTargetDate(result.targetDate);

            // Lưu activity index vào SharedPrefs
            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                    .putInt("ACTIVITY_INDEX", newActivityIndex)
                    .putBoolean("TARGET_CHANGED", true)
                    .apply();
            currentActivityIndex = newActivityIndex;

            btnSave.setText("Đang lưu...");
            btnSave.setEnabled(false);

            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
            apiService.updateUserProfile("eq." + username, updateData).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Đã cập nhật mục tiêu!", Toast.LENGTH_SHORT).show();
                        loadUserProfile();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                        btnSave.setText("LƯU");
                        btnSave.setEnabled(true);
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                    btnSave.setText("LƯU");
                    btnSave.setEnabled(true);
                }
            });
        });

        dialog.show();
    }

    private void loadFitnessGoalsList() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllFitnessGoals("*").enqueue(new Callback<List<com.hcmute.edu.vn.model.FitnessGoal>>() {
            @Override
            public void onResponse(Call<List<com.hcmute.edu.vn.model.FitnessGoal>> call, Response<List<com.hcmute.edu.vn.model.FitnessGoal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    fitnessGoalList = response.body();
                    if (username != null && !username.isEmpty()) {
                        loadUserProfile();
                    }
                }
            }
            @Override
            public void onFailure(Call<List<com.hcmute.edu.vn.model.FitnessGoal>> call, Throwable t) {}
        });
    }

    private void setupWaterReminder(boolean isEnable) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, com.hcmute.edu.vn.receiver.WaterReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (!isEnable) {
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        int nextHour = currentHour + (currentHour % 2 == 0 ? 2 : 1);

        if (nextHour < 6) {
            nextHour = 6;
        } else if (nextHour > 22) {
            nextHour = 6;
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, nextHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long intervalMillis = 2 * 60 * 60 * 1000;

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    intervalMillis,
                    pendingIntent
            );
        }
    }

    private void setupWorkoutReminder(boolean isEnable) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, com.hcmute.edu.vn.receiver.DailyWorkoutReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 102, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (!isEnable) {
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            return;
        }

        // Cài đặt giờ là 17:00:00
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Nếu hiện tại đã qua 5h chiều, thì hẹn sang 5h chiều ngày mai
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Lặp lại mỗi ngày (INTERVAL_DAY)
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    android.app.AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (username != null && !username.isEmpty()) {
            loadUserProfile();
        }
    }

    private void loadUserProfile() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);
                    currentUserId = currentUser.getId();
                    currentGoalId = currentUser.getFitnessGoalId();
                    currentTargetWeight = currentUser.getTarget();

                    currentHeight = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                    currentWeight = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                    currentGender = currentUser.getGender() != null ? currentUser.getGender() : "Male";
                    currentAge = calculateAge(currentUser.getDateOfBirth());
                    if (currentAge <= 0) currentAge = 20;

                    txtName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty() ? currentUser.getName() : username);
                    txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật Email");

                    double heightCm = currentHeight;
                    double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                    tvProfileHeight.setText(heightCm > 0 ? heightCm + " cm" : "-- cm");
                    tvProfileWeight.setText(weightKg > 0 ? weightKg + " kg" : "-- kg");

                    int age = calculateAge(currentUser.getDateOfBirth());
                    tvProfileAge.setText(age > 0 ? String.valueOf(age) : "--");

                    currentActivityIndex = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                            .getInt("ACTIVITY_INDEX", 2);

                    String goalName = "Chưa thiết lập";
                    for (com.hcmute.edu.vn.model.FitnessGoal g : fitnessGoalList) {
                        if (g.getId() == currentGoalId) {
                            goalName = g.getName();
                            break;
                        }
                    }
                    tvProfileGoal.setText(goalName);

                    if (currentTargetWeight != null && currentTargetWeight > 0) {
                        tvProfileTargetWeight.setText(currentTargetWeight + " kg");
                    } else {
                        tvProfileTargetWeight.setText("Duy trì");
                    }

                    currentConditionIds.clear();
                    List<String> allergyList = new ArrayList<>();
                    List<String> historyList = new ArrayList<>();

                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                currentConditionIds.add(mc.getId());
                                String type = mc.getType();

                                if (type != null && (type.toLowerCase().contains("allergy") || type.toLowerCase().contains("dị ứng"))) {
                                    allergyList.add(mc.getName());
                                } else {
                                    historyList.add(mc.getName());
                                }
                            }
                        }
                    }

                    setupCardDisplay(allergyList, tvAllergies, cardAllergies, "Thực phẩm dị ứng");
                    setupCardDisplay(historyList, tvMedicalHistory, cardMedicalHistory, "Tiền sử bệnh");

                } else {
                    try {
                        String err = response.errorBody() != null ? response.errorBody().string() : "Rỗng";
                        Toast.makeText(ProfileActivity.this, "LỖI SUPABASE (TẢI): " + err, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {}
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {}
        });
    }

    private void setupCardDisplay(List<String> dataList, TextView textView, MaterialCardView cardView, String dialogTitle) {
        if (dataList.isEmpty()) {
            textView.setText("Không có");
            cardView.setOnClickListener(null);
            return;
        }

        StringBuilder displayStr = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            if (i < 3) {
                displayStr.append("• ").append(dataList.get(i)).append("\n");
            }
        }
        if (dataList.size() > 3) {
            displayStr.append("+ ").append(dataList.size() - 3).append(" mục khác...");
        }
        textView.setText(displayStr.toString().trim());

        boolean isAllergy = dialogTitle.toLowerCase().contains("dị ứng");
        cardView.setOnClickListener(v -> showCustomChipDialog(dialogTitle, dataList, isAllergy));
    }

    private void showCustomChipDialog(String title, List<String> items, boolean isAllergy) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_chips, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        com.google.android.material.chip.ChipGroup chipGroupItems = dialogView.findViewById(R.id.chipGroupItems);
        MaterialButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        tvDialogTitle.setText(title);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        for (String itemName : items) {
            TextView chip = new TextView(this);
            chip.setText(itemName);
            chip.setTextSize(14f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD);

            int padX = (int) (16 * getResources().getDisplayMetrics().density);
            int padY = (int) (8 * getResources().getDisplayMetrics().density);
            chip.setPadding(padX, padY, padX, padY);

            android.graphics.drawable.GradientDrawable chipGradient = new android.graphics.drawable.GradientDrawable();
            chipGradient.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
            chipGradient.setCornerRadius(100f);

            if (isAllergy) {
                chipGradient.setColors(new int[]{
                        android.graphics.Color.parseColor("#FFE0B2"),
                        android.graphics.Color.parseColor("#FFCCBC")
                });
                chip.setTextColor(android.graphics.Color.parseColor("#BF360C"));
            } else {
                chipGradient.setColors(new int[]{
                        android.graphics.Color.parseColor("#E0F2F1"),
                        android.graphics.Color.parseColor("#B2DFDB")
                });
                chip.setTextColor(android.graphics.Color.parseColor("#004D40"));
            }

            chip.setBackground(chipGradient);
            chipGroupItems.addView(chip);
        }

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showMedicalConditionDialog() {
        if (currentUserId == null) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getAllMedicalConditions("*").enqueue(new Callback<List<MedicalCondition>>() {
            @Override
            public void onResponse(Call<List<MedicalCondition>> call, Response<List<MedicalCondition>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MedicalCondition> allConditions = response.body();

                    android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_update_medical, null);

                    com.google.android.material.tabs.TabLayout tabLayoutMedical = dialogView.findViewById(R.id.tabLayoutMedical);
                    LinearLayout llAllergiesContainer = dialogView.findViewById(R.id.llAllergiesContainer);
                    LinearLayout llDiseasesContainer = dialogView.findViewById(R.id.llDiseasesContainer);
                    MaterialButton btnCancelUpdate = dialogView.findViewById(R.id.btnCancelUpdate);
                    MaterialButton btnSaveUpdate = dialogView.findViewById(R.id.btnSaveUpdate);

                    tabLayoutMedical.addTab(tabLayoutMedical.newTab().setText("Dị ứng"));
                    tabLayoutMedical.addTab(tabLayoutMedical.newTab().setText("Bệnh lý"));

                    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(ProfileActivity.this)
                            .setView(dialogView)
                            .create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    List<com.google.android.material.checkbox.MaterialCheckBox> checkBoxesList = new ArrayList<>();

                    for (MedicalCondition condition : allConditions) {
                        View itemView = getLayoutInflater().inflate(R.layout.item_medical_condition, llAllergiesContainer, false);

                        TextView tvName = itemView.findViewById(R.id.tvConditionName);
                        TextView tvType = itemView.findViewById(R.id.tvConditionType);
                        com.google.android.material.checkbox.MaterialCheckBox checkBox = itemView.findViewById(R.id.cbCondition);
                        MaterialCardView cardView = (MaterialCardView) itemView;

                        tvName.setText(condition.getName());
                        checkBox.setTag(condition.getId());

                        boolean isAllergy = "allergy".equals(condition.getType());
                        if (isAllergy) {
                            tvType.setText("DỊ ỨNG");
                            tvType.setTextColor(android.graphics.Color.parseColor("#FF7043"));
                            llAllergiesContainer.addView(itemView);
                        } else {
                            tvType.setText("BỆNH LÝ");
                            tvType.setTextColor(android.graphics.Color.parseColor("#26A69A"));
                            llDiseasesContainer.addView(itemView);
                        }

                        if (currentConditionIds.contains(condition.getId())) {
                            checkBox.setChecked(true);
                            cardView.setStrokeColor(android.graphics.Color.parseColor("#009688"));
                            cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F2F1"));
                        }

                        cardView.setOnClickListener(v -> {
                            boolean isChecked = !checkBox.isChecked();
                            checkBox.setChecked(isChecked);

                            if (isChecked) {
                                cardView.setStrokeColor(android.graphics.Color.parseColor("#009688"));
                                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F2F1"));
                            } else {
                                cardView.setStrokeColor(android.graphics.Color.parseColor("#E0E0E0"));
                                cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#F8FAFB"));
                            }
                        });

                        checkBoxesList.add(checkBox);
                    }

                    tabLayoutMedical.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                            if (tab.getPosition() == 0) {
                                llAllergiesContainer.setVisibility(View.VISIBLE);
                                llDiseasesContainer.setVisibility(View.GONE);
                            } else {
                                llAllergiesContainer.setVisibility(View.GONE);
                                llDiseasesContainer.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                        @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                    });

                    btnCancelUpdate.setOnClickListener(v -> dialog.dismiss());

                    btnSaveUpdate.setOnClickListener(v -> {
                        List<UserMedicalConditionInsert> insertList = new ArrayList<>();
                        for (com.google.android.material.checkbox.MaterialCheckBox cb : checkBoxesList) {
                            if (cb.isChecked()) {
                                Integer conditionId = (Integer) cb.getTag();
                                insertList.add(new UserMedicalConditionInsert(currentUserId, conditionId));
                            }
                        }
                        saveMedicalConditions(apiService, insertList);
                        dialog.dismiss();
                    });

                    dialog.show();
                }
            }
            @Override
            public void onFailure(Call<List<MedicalCondition>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi kết nối khi tải danh sách bệnh!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveMedicalConditions(SupabaseApiService apiService, List<UserMedicalConditionInsert> insertList) {
        apiService.deleteUserMedicalConditions("eq." + currentUserId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (insertList.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Đã xóa toàn bộ bệnh lý!", Toast.LENGTH_SHORT).show();
                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("ALLERGY_DIRTY", true).apply();
                    loadUserProfile();
                    return;
                }

                apiService.saveUserMedicalConditions(insertList).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(ProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                            getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putBoolean("ALLERGY_DIRTY", true).apply();
                            loadUserProfile();
                        } else {
                            try {
                                String err = response.errorBody() != null ? response.errorBody().string() : "Rỗng";
                                Toast.makeText(ProfileActivity.this, "LỖI SUPABASE (LƯU): " + err, Toast.LENGTH_LONG).show();
                            } catch (Exception e) {}
                        }
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);
            if (birthDate == null) return 0;

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();
            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }
            return age;
        } catch (Exception e) { return 0; }
    }

    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);

        navHome.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, HomeActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });
        navWorkout.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, WorkoutJourneyActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });
        navNutrition.setOnClickListener(v -> {
            Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            overridePendingTransition(0, 0);
        });
    }
}