package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

        // ĐÃ THÊM: Ánh xạ 2 cái thẻ CardView để lát bắt sự kiện click
        cardMedicalHistory = findViewById(R.id.cardMedicalHistory);
        cardAllergies = findViewById(R.id.cardAllergies);

        btnLogout.setOnClickListener(v -> {
            Intent loginIntent = new Intent(ProfileActivity.this, com.hcmute.edu.vn.activity.LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            Toast.makeText(ProfileActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Nút Cập nhật y tế
        btnUpdateMedical.setOnClickListener(v -> showMedicalConditionDialog());

        setupBottomNavigation();
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

                    // Hiển thị thông tin cá nhân
                    txtName.setText(currentUser.getName() != null && !currentUser.getName().isEmpty() ? currentUser.getName() : username);
                    txtEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "Chưa cập nhật Email");

                    double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                    double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                    tvProfileHeight.setText(heightCm > 0 ? heightCm + " cm" : "-- cm");
                    tvProfileWeight.setText(weightKg > 0 ? weightKg + " kg" : "-- kg");

                    int age = calculateAge(currentUser.getDateOfBirth());
                    tvProfileAge.setText(age > 0 ? String.valueOf(age) : "--");

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
    // HÀM HELPER: XỬ LÝ RÚT GỌN CHUỖI VÀ TẠO POP-UP
    // ==============================================================
    private void setupCardDisplay(List<String> dataList, TextView textView, MaterialCardView cardView, String dialogTitle) {
        if (dataList.isEmpty()) {
            textView.setText("Không có");
            cardView.setOnClickListener(null); // Không có gì thì không cho click
            return;
        }

        StringBuilder displayStr = new StringBuilder(); // Chuỗi ngắn (Max 3)
        StringBuilder fullStr = new StringBuilder();    // Chuỗi dài (Full cho Dialog)

        for (int i = 0; i < dataList.size(); i++) {
            String item = "• " + dataList.get(i) + "\n";
            fullStr.append(item);

            if (i < 3) {
                displayStr.append(item);
            }
        }

        if (dataList.size() > 3) {
            int extra = dataList.size() - 3;
            displayStr.append("+ ").append(extra).append(" mục khác...");
        }

        textView.setText(displayStr.toString().trim());

        // Bắt sự kiện click vào Thẻ -> Mở Pop-up
        String finalFullContent = fullStr.toString().trim();
        cardView.setOnClickListener(v -> showDetailDialog(dialogTitle, finalFullContent));
    }

    // HÀM HIỂN THỊ POP-UP CHI TIẾT
    private void showDetailDialog(String title, String fullContent) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(fullContent)
                .setPositiveButton("ĐÓNG", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showMedicalConditionDialog() {
        if (currentUserId == null) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getAllMedicalConditions("*").enqueue(new Callback<List<MedicalCondition>>() {
            @Override
            public void onResponse(Call<List<MedicalCondition>> call, Response<List<MedicalCondition>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MedicalCondition> allConditions = response.body();
                    String[] conditionNames = new String[allConditions.size()];
                    boolean[] checkedItems = new boolean[allConditions.size()];

                    for (int i = 0; i < allConditions.size(); i++) {
                        conditionNames[i] = allConditions.get(i).getName() + ("allergy".equals(allConditions.get(i).getType()) ? " (Dị ứng)" : " (Bệnh lý)");
                        checkedItems[i] = currentConditionIds.contains(allConditions.get(i).getId());
                    }

                    new android.app.AlertDialog.Builder(ProfileActivity.this)
                            .setTitle("Cập nhật tình trạng sức khỏe")
                            .setMultiChoiceItems(conditionNames, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                            .setPositiveButton("Lưu", (dialog, which) -> {
                                List<UserMedicalConditionInsert> insertList = new ArrayList<>();
                                for (int i = 0; i < allConditions.size(); i++) {
                                    if (checkedItems[i]) {
                                        insertList.add(new UserMedicalConditionInsert(currentUserId, allConditions.get(i).getId()));
                                    }
                                }
                                saveMedicalConditions(apiService, insertList);
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                }
            }
            @Override
            public void onFailure(Call<List<MedicalCondition>> call, Throwable t) {
                Toast.makeText(ProfileActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
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