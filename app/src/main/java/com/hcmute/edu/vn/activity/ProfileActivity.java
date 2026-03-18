    package com.hcmute.edu.vn.activity;
    
    import android.app.PendingIntent;
    import android.content.Intent;
    import android.os.Bundle;
    import android.view.View;
    import android.widget.EditText;
    import android.widget.LinearLayout;
    import android.widget.Spinner;
    import android.widget.TextView;
    import android.widget.Toast;
    
    import androidx.appcompat.app.AppCompatActivity;
    
    import com.google.android.material.button.MaterialButton;
    import com.google.android.material.card.MaterialCardView;
    import com.google.android.material.dialog.MaterialAlertDialogBuilder;
    import com.hcmute.edu.vn.R;
    import com.hcmute.edu.vn.database.SupabaseApiService;
    import com.hcmute.edu.vn.database.SupabaseClient;
    import com.hcmute.edu.vn.model.MedicalCondition;
    import com.hcmute.edu.vn.model.User;
    import com.hcmute.edu.vn.model.UserMedicalCondition;
    import com.hcmute.edu.vn.model.UserMedicalConditionInsert;
    
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.List;
    import java.util.Locale;
    
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
        List<com.hcmute.edu.vn.model.FitnessGoal> fitnessGoalList = new ArrayList<>();
        Integer currentGoalId = 1;
        Float currentTargetWeight = null;
    
        Double currentHeight = 0.0;
        Double currentWeight = 0.0;
    
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.profile);
    
            androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.setAppearanceLightStatusBars(true);
    
            android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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
            androidx.appcompat.widget.SwitchCompat switchWater = findViewById(R.id.switchWaterReminder);
            boolean isWaterReminderOn = pref.getBoolean("WATER_REMINDER", false);
            switchWater.setChecked(isWaterReminderOn);
    
            switchWater.setOnCheckedChangeListener((buttonView, isChecked) -> {
                pref.edit().putBoolean("WATER_REMINDER", isChecked).apply();
    
                if (isChecked) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                        }
                    }
                    setupWaterReminder(true);
                    Toast.makeText(this, "Đã bật nhắc nhở uống nước!", Toast.LENGTH_SHORT).show();
                } else {
                    setupWaterReminder(false);
                    Toast.makeText(this, "Đã tắt nhắc nhở uống nước", Toast.LENGTH_SHORT).show();
                }
            });
    
            btnLogout.setOnClickListener(v -> {
                Intent loginIntent = new Intent(ProfileActivity.this, com.hcmute.edu.vn.activity.LoginActivity.class);
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
    
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_edit_goal, null);
            Spinner dialogSpinnerGoal = dialogView.findViewById(R.id.dialogSpinnerGoal);
            LinearLayout dialogLayoutTarget = dialogView.findViewById(R.id.dialogLayoutTarget);
            EditText dialogEdtTarget = dialogView.findViewById(R.id.dialogEdtTarget);
            com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnDialogCancelGoal);
            com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnDialogSaveGoal);
    
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setView(dialogView).create();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
    
            List<String> goalNames = new ArrayList<>();
            int selectedIndex = 0;
            for (int i = 0; i < fitnessGoalList.size(); i++) {
                goalNames.add(fitnessGoalList.get(i).getName());
                if (fitnessGoalList.get(i).getId() == currentGoalId) {
                    selectedIndex = i;
                }
            }
    
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, goalNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            dialogSpinnerGoal.setAdapter(adapter);
            dialogSpinnerGoal.setSelection(selectedIndex);

            final int finalSelectedIndex = selectedIndex;
    
            if (currentTargetWeight != null && currentTargetWeight > 0) {
                dialogEdtTarget.setText(String.valueOf(currentTargetWeight));
            }
    
            dialogSpinnerGoal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    String selectedName = goalNames.get(position).toLowerCase();
    
                    // ===============================================================
                    // KIỂM TRA BMI NGAY LẬP TỨC KHI NGƯỜI DÙNG VỪA CHỌN SPINNER
                    // (Chỉ kiểm tra nếu họ chọn một mục tiêu MỚI khác với cái ban đầu)
                    // ===============================================================
                    if (position != finalSelectedIndex && currentHeight != null && currentHeight > 0 && currentWeight != null && currentWeight > 0) {
                        double heightM = currentHeight / 100.0;
                        double currentBmi = currentWeight / (heightM * heightM);
    
                        boolean isLose = selectedName.contains("giảm") || selectedName.contains("lose");
                        boolean isGain = selectedName.contains("tăng") || selectedName.contains("gain") || selectedName.contains("build");
                        boolean isMaintain = selectedName.contains("duy trì") || selectedName.contains("maintain");
    
                        // Rào chắn 1: Béo phì không được Tăng cơ
                        if (isGain && currentBmi > 23.0) {
                            Toast.makeText(ProfileActivity.this, "Bạn đang thừa cân (BMI > 23.0), không nên Tăng cơ lúc này. Hãy chọn Giảm mỡ nhé!", Toast.LENGTH_LONG).show();
                            dialogSpinnerGoal.setSelection(finalSelectedIndex); // Ép trả về mục tiêu ban đầu
                            return; // Dừng ngay, không chạy tiếp code bên dưới
                        }
    
                        // Rào chắn 2: Thiếu cân không được Giảm mỡ
                        if (isLose && currentBmi < 18.5) {
                            Toast.makeText(ProfileActivity.this, "Bạn đang thiếu cân (BMI < 18.5), không thể chọn Giảm mỡ. Hãy chọn Tăng cơ nhé!", Toast.LENGTH_LONG).show();
                            dialogSpinnerGoal.setSelection(finalSelectedIndex); // Ép trả về mục tiêu ban đầu
                            return; // Dừng ngay
                        }
    
                        // Rào chắn 3: Không chuẩn thì không được Duy trì
                        if (isMaintain && (currentBmi < 18.5 || currentBmi > 23.0)) {
                            Toast.makeText(ProfileActivity.this, "BMI hiện tại chưa đạt chuẩn (18.5 - 23.0), không thể chọn Duy trì vóc dáng!", Toast.LENGTH_LONG).show();
                            dialogSpinnerGoal.setSelection(finalSelectedIndex); // Ép trả về mục tiêu ban đầu
                            return; // Dừng ngay
                        }
                    }
    
                    // ===============================================================
                    // NẾU VƯỢT QUA BÀI KIỂM TRA -> CẬP NHẬT GIAO DIỆN ẨN/HIỆN Ô CÂN NẶNG
                    // ===============================================================
                    if (selectedName.contains("duy trì") || selectedName.contains("maintain")) {
                        dialogLayoutTarget.setVisibility(View.GONE);
                        dialogEdtTarget.setText("");
                    } else {
                        dialogLayoutTarget.setVisibility(View.VISIBLE);
                    }
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
    
            btnCancel.setOnClickListener(v -> dialog.dismiss());
    
            btnSave.setOnClickListener(v -> {
                int newGoalId = fitnessGoalList.get(dialogSpinnerGoal.getSelectedItemPosition()).getId();
                Float newTarget = null;
                String selectedGoalName = fitnessGoalList.get(dialogSpinnerGoal.getSelectedItemPosition()).getName().toLowerCase();
    
                if (dialogLayoutTarget.getVisibility() == View.VISIBLE) {
                    String targetStr = dialogEdtTarget.getText().toString().trim();
                    if (!targetStr.isEmpty()) {
                        try {
                            newTarget = Float.parseFloat(targetStr);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Cân nặng phải là số!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } else {
                        dialogEdtTarget.setError("Vui lòng nhập cân nặng mục tiêu!");
                        dialogEdtTarget.requestFocus();
                        return;
                    }
                }
    
                // ===============================================================
                // LOGIC 3 RÀO CHẮN BMI (Sử dụng từ khóa an toàn: giảm, tăng, duy trì)
                // ===============================================================
                if (currentHeight != null && currentHeight > 0 && currentWeight != null && currentWeight > 0) {
                    double heightM = currentHeight / 100.0;
                    double currentBmi = currentWeight / (heightM * heightM);
    
                    boolean isLose = selectedGoalName.contains("giảm") || selectedGoalName.contains("lose");
                    boolean isGain = selectedGoalName.contains("tăng") || selectedGoalName.contains("gain") || selectedGoalName.contains("build");
                    boolean isMaintain = selectedGoalName.contains("duy trì") || selectedGoalName.contains("maintain");
    
                    // --- RÀO CHẮN 1: TÍNH HỢP LÝ CỦA MỤC TIÊU VỚI THỂ TRẠNG HIỆN TẠI ---
                    if (isLose && currentBmi < 18.5) {
                        Toast.makeText(this, "Bạn đang thiếu cân (BMI < 18.5), không thể chọn Giảm mỡ. Hãy chọn Tăng cơ nhé!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (isGain && currentBmi > 23.0) {
                        Toast.makeText(this, "Bạn đang thừa cân (BMI > 23.0), không nên Tăng cơ lúc này. Hãy chọn Giảm mỡ nhé!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (isMaintain && (currentBmi < 18.5 || currentBmi > 23.0)) {
                        Toast.makeText(this, "BMI hiện tại không nằm trong mức chuẩn (18.5 - 23.0). Không nên chọn Duy trì vóc dáng lúc này!", Toast.LENGTH_LONG).show();
                        return;
                    }
    
                    // --- RÀO CHẮN 2 & 3: KIỂM TRA MỨC CÂN NẶNG ĐÍCH HỢP LÝ ---
                    if (newTarget != null && !isMaintain) {
                        double targetBmi = newTarget / (heightM * heightM);
    
                        if (isLose) {
                            if (newTarget >= currentWeight) {
                                dialogEdtTarget.setError("Để giảm mỡ, cân nặng mục tiêu phải < cân nặng hiện tại!");
                                dialogEdtTarget.requestFocus();
                                return;
                            }
                            if (targetBmi < 18.5) {
                                dialogEdtTarget.setError("Cấm! Mức này quá thấp (BMI < 18.5). Hãy điều chỉnh lại mục tiêu an toàn hơn (Target BMI: 18.5 - 23.0).");
                                dialogEdtTarget.requestFocus();
                                return;
                            }
                        }
                        else if (isGain) {
                            if (newTarget <= currentWeight) {
                                dialogEdtTarget.setError("Để tăng cơ, cân nặng mục tiêu phải > cân nặng hiện tại!");
                                dialogEdtTarget.requestFocus();
                                return;
                            }
                            if (targetBmi > 23.0) {
                                dialogEdtTarget.setError("Cấm! Mức này quá cao (BMI > 23.0). Hãy điều chỉnh lại mục tiêu an toàn hơn (Target BMI: 18.5 - 23.0).");
                                dialogEdtTarget.requestFocus();
                                return;
                            }
                        }
                    }
                }
                // ===============================================================
    
                // Vượt qua hết các rào chắn thì gọi API Lưu dữ liệu
                User updateData = new User();
                updateData.setFitnessGoalId(newGoalId);
                updateData.setTarget(newTarget);
    
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
    
                        txtName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty() ? currentUser.getName() : username);
                        txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật Email");
    
                        double heightCm = currentHeight;
                        double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                        tvProfileHeight.setText(heightCm > 0 ? heightCm + " cm" : "-- cm");
                        tvProfileWeight.setText(weightKg > 0 ? weightKg + " kg" : "-- kg");
    
                        int age = calculateAge(currentUser.getDateOfBirth());
                        tvProfileAge.setText(age > 0 ? String.valueOf(age) : "--");
    
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
    
                                if(isChecked) {
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
                startActivity(i); overridePendingTransition(0, 0);
            });
            navWorkout.setOnClickListener(v -> {
                Intent i = new Intent(ProfileActivity.this, WorkoutActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); overridePendingTransition(0, 0);
            });
            navNutrition.setOnClickListener(v -> {
                Intent i = new Intent(ProfileActivity.this, NutritionActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i); overridePendingTransition(0, 0);
            });
        }
    }