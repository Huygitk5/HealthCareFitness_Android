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
    MaterialCardView cardMedicalHistory, cardAllergies; // ĐÃ THÊM: Ánh xạ 2 thẻ
    MaterialButton btnLogout;

    String username;
    String currentUserId;
    List<Integer> currentConditionIds = new ArrayList<>(); // Lưu ID bệnh đang mắc để tick sẵn

    TextView tvProfileGoal, tvProfileTargetWeight, btnEditGoal;
    List<com.hcmute.edu.vn.model.FitnessGoal> fitnessGoalList = new ArrayList<>();
    Integer currentGoalId = 1;
    Float currentTargetWeight = null;

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

        // Ánh xạ 2 cái thẻ CardView
        cardMedicalHistory = findViewById(R.id.cardMedicalHistory);
        cardAllergies = findViewById(R.id.cardAllergies);

        tvProfileGoal = findViewById(R.id.tvProfileGoal);
        tvProfileTargetWeight = findViewById(R.id.tvProfileTargetWeight);
        btnEditGoal = findViewById(R.id.btnEditGoal);

        // ==============================================================
        // ĐÃ THÊM MỚI: Xử lý nút Switch Nhắc nhở uống nước
        // ==============================================================
        androidx.appcompat.widget.SwitchCompat switchWater = findViewById(R.id.switchWaterReminder);

        // Khôi phục trạng thái nút Switch từ lần mở app trước
        boolean isWaterReminderOn = pref.getBoolean("WATER_REMINDER", false);
        switchWater.setChecked(isWaterReminderOn);

        // Bắt sự kiện khi người dùng gạt nút
        switchWater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Lưu trạng thái mới vào SharedPreferences
            pref.edit().putBoolean("WATER_REMINDER", isChecked).apply();

            if (isChecked) {
                // Nếu là Android 13 trở lên, phải xin quyền gửi thông báo
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
        // ==============================================================

        btnLogout.setOnClickListener(v -> {
            Intent loginIntent = new Intent(ProfileActivity.this, com.hcmute.edu.vn.activity.LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Bắt sự kiện bấm nút Thay đổi mục tiêu luyện tập
        btnEditGoal.setOnClickListener(v -> showEditGoalDialog());
        // Nút Cập nhật y tế
        btnUpdateMedical.setOnClickListener(v -> showMedicalConditionDialog());
        loadFitnessGoalsList();
        setupBottomNavigation();
    }

    // ==============================================================
    // HÀM HIỂN THỊ DIALOG ĐỔI MỤC TIÊU
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

        // Đổ dữ liệu vào Spinner
        List<String> goalNames = new ArrayList<>();
        int selectedIndex = 0;
        for (int i = 0; i < fitnessGoalList.size(); i++) {
            goalNames.add(fitnessGoalList.get(i).getName());
            if (fitnessGoalList.get(i).getId() == currentGoalId) {
                selectedIndex = i; // Tìm vị trí mục tiêu hiện tại để set default
            }
        }

        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, goalNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogSpinnerGoal.setAdapter(adapter);
        dialogSpinnerGoal.setSelection(selectedIndex);

        // Hiển thị cân nặng hiện tại (nếu có)
        if (currentTargetWeight != null && currentTargetWeight > 0) {
            dialogEdtTarget.setText(String.valueOf(currentTargetWeight));
        }

        // Bắt sự kiện chọn Spinner để ẩn/hiện ô nhập cân nặng
        dialogSpinnerGoal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedName = goalNames.get(position).toLowerCase();
                if (selectedName.contains("duy trì") || selectedName.contains("maintain")) {
                    dialogLayoutTarget.setVisibility(View.GONE);
                    dialogEdtTarget.setText("");
                } else {
                    dialogLayoutTarget.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Bắt sự kiện Nút bấm
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            int newGoalId = fitnessGoalList.get(dialogSpinnerGoal.getSelectedItemPosition()).getId();
            Float newTarget = null;

            if (dialogLayoutTarget.getVisibility() == View.VISIBLE) {
                String targetStr = dialogEdtTarget.getText().toString().trim();
                if (!targetStr.isEmpty()) {
                    try {
                        newTarget = Float.parseFloat(targetStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Cân nặng phải là số!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
            }

            // Gọi API Update (Chỉ update 2 trường này)
            User updateData = new User();
            updateData.setFitnessGoalId(newGoalId);
            updateData.setTarget(newTarget);

            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
            apiService.updateUserProfile("eq." + username, updateData).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProfileActivity.this, "Đã cập nhật mục tiêu!", Toast.LENGTH_SHORT).show();
                        loadUserProfile(); // Tải lại thông tin để màn hình cập nhật UI
                        dialog.dismiss();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(ProfileActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
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
                        loadUserProfile(); // Tải xong Goal thì mới tải User để dịch được tên
                    }
                }
            }
            @Override
            public void onFailure(Call<List<com.hcmute.edu.vn.model.FitnessGoal>> call, Throwable t) {}
        });
    }

    // ==============================================================
    // HÀM CÀI ĐẶT NHẮC NHỞ UỐNG NƯỚC (CÁCH 2 TIẾNG TỪ 6H SÁNG)
    // ==============================================================
    private void setupWaterReminder(boolean isEnable) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, com.hcmute.edu.vn.receiver.WaterReminderReceiver.class); // Đảm bảo đúng package
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (!isEnable) {
            // Nếu TẮT -> Hủy báo thức
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
            return;
        }

        // BẬT -> Tính toán thời gian của mốc chẵn tiếp theo (6, 8, 10, 12... 22)
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // Thuật toán tìm giờ chẵn tiếp theo
        int nextHour = currentHour + (currentHour % 2 == 0 ? 2 : 1);

        if (nextHour < 6) {
            nextHour = 6; // Đêm khuya thì dời sang 6h sáng hôm nay
        } else if (nextHour > 22) {
            nextHour = 6; // Đã qua 22h thì dời sang 6h sáng hôm sau
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, nextHour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Cài đặt lặp lại mỗi 2 tiếng (1000ms * 60s * 60m * 2h)
        long intervalMillis = 2 * 60 * 60 * 1000;

        if (alarmManager != null) {
            // Dùng setRepeating để máy tự động lặp lại báo thức
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

        // Bỏ Alias, gọi thẳng tên bảng để GSON không bị lú
        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    User currentUser = response.body().get(0);
                    currentUserId = currentUser.getId();
                    currentGoalId = currentUser.getFitnessGoalId();
                    currentTargetWeight = currentUser.getTarget();

                    // Hiển thị thông tin cá nhân
                    txtName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty() ? currentUser.getName() : username);
                    txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật Email");

                    double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
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

                    // HIỂN THỊ Y TẾ (SỬ DỤNG LOGIC POP-UP MỚI)
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

                    // Gọi hàm Helper để thiết lập giao diện và Pop-up
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

    // ==============================================================
    // HÀM HELPER: XỬ LÝ RÚT GỌN CHUỖI VÀ GỌI POP-UP CHIP
    // ==============================================================
    private void setupCardDisplay(List<String> dataList, TextView textView, MaterialCardView cardView, String dialogTitle) {
        if (dataList.isEmpty()) {
            textView.setText("Không có");
            cardView.setOnClickListener(null);
            return;
        }

        // Vẫn giữ logic tạo chuỗi rút gọn cho mặt ngoài của Thẻ
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

        // BẮT SỰ KIỆN MỞ DIALOG MỚI (Truyền luôn cái List vào)
        boolean isAllergy = dialogTitle.toLowerCase().contains("dị ứng");
        cardView.setOnClickListener(v -> showCustomChipDialog(dialogTitle, dataList, isAllergy));
    }

    // ==============================================================
    // HÀM VẼ DIALOG CUSTOM CHIP (ĐÃ CÓ CHIP GRADIENT)
    // ==============================================================
    private void showCustomChipDialog(String title, List<String> items, boolean isAllergy) {
        // 1. Gắn file layout XML vừa tạo
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_chips, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        com.google.android.material.chip.ChipGroup chipGroupItems = dialogView.findViewById(R.id.chipGroupItems);
        MaterialButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

        tvDialogTitle.setText(title);

        // 2. Tạo Dialog
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Xóa phông đen để lộ lớp CardView bo góc phía sau (trong XML)
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 3. Vòng lặp vẽ từng cục "Chip" nhét vào ChipGroup
        for (String itemName : items) {
            // DÙNG TEXTVIEW THAY VÌ CHIP ĐỂ KHÔNG BỊ ÉP MÀU TÍM MẶC ĐỊNH
            TextView chip = new TextView(this);
            chip.setText(itemName);
            chip.setTextSize(14f);
            chip.setTypeface(null, android.graphics.Typeface.BOLD); // In đậm chữ cho giống Chip

            // Tính toán kích thước bo viền (Padding) cho TextView to ra thành viên thuốc
            int padX = (int) (16 * getResources().getDisplayMetrics().density);
            int padY = (int) (8 * getResources().getDisplayMetrics().density);
            chip.setPadding(padX, padY, padX, padY);

            // TẠO NỀN GRADIENT
            android.graphics.drawable.GradientDrawable chipGradient = new android.graphics.drawable.GradientDrawable();
            chipGradient.setOrientation(android.graphics.drawable.GradientDrawable.Orientation.TL_BR);
            chipGradient.setCornerRadius(100f); // Bo tròn lẳn 2 đầu

            // Phối màu Gradient
            if (isAllergy) {
                // Gradient Dị ứng: Cam nhạt -> Cam đậm
                chipGradient.setColors(new int[]{
                        android.graphics.Color.parseColor("#FFE0B2"),
                        android.graphics.Color.parseColor("#FFCCBC")
                });
                chip.setTextColor(android.graphics.Color.parseColor("#BF360C"));
            } else {
                // Gradient Bệnh lý: Xanh lơ nhạt -> Xanh ngọc bích
                chipGradient.setColors(new int[]{
                        android.graphics.Color.parseColor("#E0F2F1"),
                        android.graphics.Color.parseColor("#B2DFDB")
                });
                chip.setTextColor(android.graphics.Color.parseColor("#004D40"));
            }

            // Gắn nền Gradient cho TextView
            chip.setBackground(chipGradient);

            // Nhét nó vào ChipGroup
            chipGroupItems.addView(chip);
        }

        // 4. Bấm nút Đóng
        btnDialogClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // ==============================================================
    // HÀM HIỂN THỊ DIALOG CẬP NHẬT Y TẾ (CÓ CHIA TAB)
    // ==============================================================
    private void showMedicalConditionDialog() {
        if (currentUserId == null) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getAllMedicalConditions("*").enqueue(new Callback<List<MedicalCondition>>() {
            @Override
            public void onResponse(Call<List<MedicalCondition>> call, Response<List<MedicalCondition>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MedicalCondition> allConditions = response.body();

                    // 1. Lấy giao diện XML Dialog lớn
                    android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_update_medical, null);

                    com.google.android.material.tabs.TabLayout tabLayoutMedical = dialogView.findViewById(R.id.tabLayoutMedical);
                    LinearLayout llAllergiesContainer = dialogView.findViewById(R.id.llAllergiesContainer);
                    LinearLayout llDiseasesContainer = dialogView.findViewById(R.id.llDiseasesContainer);
                    MaterialButton btnCancelUpdate = dialogView.findViewById(R.id.btnCancelUpdate);
                    MaterialButton btnSaveUpdate = dialogView.findViewById(R.id.btnSaveUpdate);

                    // THÊM 2 TAB VÀO THANH ĐIỀU HƯỚNG
                    tabLayoutMedical.addTab(tabLayoutMedical.newTab().setText("Dị ứng"));
                    tabLayoutMedical.addTab(tabLayoutMedical.newTab().setText("Bệnh lý"));

                    // 2. Tạo Dialog
                    android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(ProfileActivity.this)
                            .setView(dialogView)
                            .create();

                    if (dialog.getWindow() != null) {
                        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
                    }

                    // 3. Vòng lặp vẽ các Thẻ (Card) bệnh lý và chia hộp
                    List<com.google.android.material.checkbox.MaterialCheckBox> checkBoxesList = new ArrayList<>();

                    for (MedicalCondition condition : allConditions) {
                        // Tránh truyền 'null' vào ViewParent để layout không bị vỡ kích thước
                        View itemView = getLayoutInflater().inflate(R.layout.item_medical_condition, llAllergiesContainer, false);

                        TextView tvName = itemView.findViewById(R.id.tvConditionName);
                        TextView tvType = itemView.findViewById(R.id.tvConditionType);
                        com.google.android.material.checkbox.MaterialCheckBox checkBox = itemView.findViewById(R.id.cbCondition);
                        MaterialCardView cardView = (MaterialCardView) itemView;

                        tvName.setText(condition.getName());
                        checkBox.setTag(condition.getId());

                        // Phân loại và nhét thẻ vào đúng Hộp chứa
                        boolean isAllergy = "allergy".equals(condition.getType());
                        if (isAllergy) {
                            tvType.setText("DỊ ỨNG");
                            tvType.setTextColor(android.graphics.Color.parseColor("#FF7043"));
                            llAllergiesContainer.addView(itemView); // Nhét vào hộp Dị ứng
                        } else {
                            tvType.setText("BỆNH LÝ");
                            tvType.setTextColor(android.graphics.Color.parseColor("#26A69A"));
                            llDiseasesContainer.addView(itemView); // Nhét vào hộp Bệnh lý
                        }

                        // Set trạng thái tick sẵn và màu sắc
                        if (currentConditionIds.contains(condition.getId())) {
                            checkBox.setChecked(true);
                            cardView.setStrokeColor(android.graphics.Color.parseColor("#009688"));
                            cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#E0F2F1"));
                        }

                        // Sự kiện click sáng thẻ
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

                    // 4. BẮT SỰ KIỆN CHUYỂN TAB ĐỂ ẨN/HIỆN HỘP CHỨA
                    tabLayoutMedical.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                            if (tab.getPosition() == 0) { // Bấm Tab 1 (Dị ứng)
                                llAllergiesContainer.setVisibility(View.VISIBLE);
                                llDiseasesContainer.setVisibility(View.GONE);
                            } else { // Bấm Tab 2 (Bệnh lý)
                                llAllergiesContainer.setVisibility(View.GONE);
                                llDiseasesContainer.setVisibility(View.VISIBLE);
                            }
                        }
                        @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                        @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                    });

                    // 5. Sự kiện Lưu và Hủy
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