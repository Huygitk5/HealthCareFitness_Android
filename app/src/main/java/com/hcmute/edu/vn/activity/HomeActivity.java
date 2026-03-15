package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.adapter.ActivityAdapter;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.News;
import com.hcmute.edu.vn.adapter.NewsAdapter;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.activity.NutritionActivity;
import com.hcmute.edu.vn.activity.ProfileActivity;
import com.hcmute.edu.vn.activity.WorkoutActivity;

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

public class HomeActivity extends AppCompatActivity {

    TextView tvGreeting, tvCurrentWeight, tvCurrentHeight, tvCurrentAge, tvBMIValue, tvBMIStatus;
    ImageView btnNotification;
    RecyclerView rvActivities, rvNews;
    String username;
    TextView btnChartDay, btnChartWeek, btnChartMonth;
    LineChart lineChartBMI;
    List<BmiLog> currentBmiLogs = new ArrayList<>();
    LinearLayout btnUpdateBMI;
    User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // 2. Ánh xạ các View từ XML
        tvGreeting = findViewById(R.id.tvGreeting);
        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvCurrentHeight = findViewById(R.id.tvCurrentHeight);
        tvCurrentAge = findViewById(R.id.tvCurrentAge);
        tvBMIValue = findViewById(R.id.tvBMIValue);
        tvBMIStatus = findViewById(R.id.tvBMIStatus);
        btnNotification = findViewById(R.id.btnNotification);
        rvActivities = findViewById(R.id.rvActivities);
        rvNews = findViewById(R.id.rvNews);
        btnChartDay = findViewById(R.id.btnChartDay);
        btnChartWeek = findViewById(R.id.btnChartWeek);
        btnChartMonth = findViewById(R.id.btnChartMonth);
        lineChartBMI = findViewById(R.id.lineChartBMI);

        setupChartAppearance();

        btnChartDay.setOnClickListener(v -> {
            setActiveTab(btnChartDay, btnChartWeek, btnChartMonth);
            updateChartData("DAY");
        });

        btnChartWeek.setOnClickListener(v -> {
            setActiveTab(btnChartWeek, btnChartDay, btnChartMonth);
            updateChartData("WEEK");
        });

        btnChartMonth.setOnClickListener(v -> {
            setActiveTab(btnChartMonth, btnChartDay, btnChartWeek);
            updateChartData("MONTH");
        });

        // 4. Sự kiện Click Chuông Thông báo
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Bạn có 0 thông báo mới", Toast.LENGTH_SHORT).show();
            }
        });

        // =========================================================
        // SETUP RECYCLER VIEW CHO ACTIVITIES (Bài tập)
        // =========================================================
        ArrayList<Exercise> activityList = new ArrayList<>();

        // Cập nhật lại khởi tạo Exercise với đầy đủ tham số
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Giảm Mỡ Thừa ⚡⚡", "Bài tập giúp đốt mỡ", 1, 1, 3, "20 mins", "", String.valueOf(R.drawable.workout_1), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Tăng Cơ 💪", "Xây dựng sức mạnh", 2, 2, 4, "30 mins", "", String.valueOf(R.drawable.workout_2), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Yoga Buổi Sáng 🧘", "Thư giãn tinh thần", 3, 1, 1, "15 mins", "", String.valueOf(R.drawable.workout_3), null));

        // DÙNG ActivityAdapter MỚI ĐỂ GIAO DIỆN TO RA
        ActivityAdapter activityAdapter = new ActivityAdapter(this, activityList);

        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvActivities.setLayoutManager(activityLayoutManager);
        rvActivities.setAdapter(activityAdapter);

        // =========================================================
        // SETUP RECYCLER VIEW CHO NEWS (Tin tức)
        // =========================================================
        ArrayList<News> newsList = new ArrayList<>();

        newsList.add(new News(
                "3 Tạng người trong tập gym, bạn thuộc tạng người nào?",
                "Đã đăng 3 phút trước  •  Thích Gym 24h",
                R.drawable.gym_tmp));

        newsList.add(new News(
                "Thực đơn ăn kiêng 7 ngày đánh bay mỡ thừa an toàn",
                "Đã đăng 2 giờ trước  •  HLV Quang Lâm",
                R.drawable.gym_tmp));

        newsList.add(new News(
                "Cách hít thở chuẩn khi tập tạ nặng tránh chấn thương",
                "Đã đăng 1 ngày trước  •  Fitness Center",
                R.drawable.gym_tmp));

        // Gọi NewsAdapter mới tạo
        NewsAdapter newsAdapter = new NewsAdapter(this, newsList);

        // Setup vuốt ngang
        LinearLayoutManager newsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvNews.setLayoutManager(newsLayoutManager);
        rvNews.setAdapter(newsAdapter);

        // =========================================================
        // XỬ LÝ CLICK BOTTOM NAVIGATION
        // =========================================================

        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        navWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, WorkoutActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        LinearLayout navProfile = findViewById(R.id.nav_profile);
        navProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        navNutrition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, NutritionActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        });

        btnUpdateBMI = findViewById(R.id.btnUpdateBMI);
        btnUpdateBMI.setOnClickListener(v -> showUpdateBMIDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LẤY USERNAME TỪ SHAREDPREFS
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        // NẾU CÓ USERNAME THÌ LOAD DATA
        if (username != null && !username.isEmpty()) {
            loadUserData();
        }
    }

    private void loadUserData() {
        if (username == null || username.isEmpty()) return;

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getUserByUsername("eq." + username, "*").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentUser = response.body().get(0);

                    // 1. HIỂN THỊ THÔNG TIN LÊN BẢNG TÌNH TRẠNG (Giữ nguyên code cũ của bạn)
                    tvGreeting.setText(currentUser.getName() != null ? currentUser.getName() : username);
                    double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                    double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                    int age = calculateAge(currentUser.getDateOfBirth());

                    if (heightCm > 0 && weightKg > 0) {
                        tvCurrentHeight.setText(heightCm + " cm");
                        tvCurrentWeight.setText(weightKg + " kg");

                        double heightM = heightCm / 100.0;
                        double bmi = weightKg / (heightM * heightM);
                        tvBMIValue.setText(String.format(Locale.getDefault(), "%.1f", bmi));

                        if (bmi < 18.5) {
                            tvBMIStatus.setText("Thiếu cân");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFCA28")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#FFCA28"));
                        } else if (bmi < 23) {
                            tvBMIStatus.setText("Bình thường");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                        } else if (bmi < 25) {
                            tvBMIStatus.setText("Thừa cân");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#FF9800"));
                        } else {
                            tvBMIStatus.setText("Béo phì");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#F44336"));
                        }
                    } else {
                        tvCurrentHeight.setText("-- cm");
                        tvCurrentWeight.setText("-- kg");
                        tvBMIValue.setText("--");
                        tvBMIStatus.setText("Chưa có");
                        tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
                    }
                    tvCurrentAge.setText(age > 0 ? String.valueOf(age) : "--");

                    // =======================================================
                    // 2. GỌI API LẤY LỊCH SỬ BMI ĐỂ VẼ BIỂU ĐỒ (DÒNG MỚI THÊM)
                    // =======================================================
                    fetchBmiHistory(currentUser.getId());

                } else {
                    Toast.makeText(HomeActivity.this, "Không tìm thấy dữ liệu người dùng!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(HomeActivity.this, "Lỗi mạng: Không thể tải dữ liệu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================================================
    // HÀM LẤY LỊCH SỬ BMI TỪ SUPABASE
    // =========================================================
    private void fetchBmiHistory(String userId) {
        if (userId == null || userId.isEmpty()) return;

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // order=recorded_at.asc để dữ liệu xếp từ quá khứ đến hiện tại
        apiService.getUserBmiLogs("eq." + userId, "*", "recorded_at.asc").enqueue(new Callback<List<BmiLog>>() {
            @Override
            public void onResponse(Call<List<BmiLog>> call, Response<List<BmiLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBmiLogs = response.body();

                    // Sau khi có dữ liệu, vẽ mặc định tab DAY (Ngày)
                    setActiveTab(btnChartDay, btnChartWeek, btnChartMonth);
                    updateChartData("DAY");
                }
            }

            @Override
            public void onFailure(Call<List<BmiLog>> call, Throwable t) {
                android.util.Log.e("BMI_CHART", "Lỗi tải biểu đồ: " + t.getMessage());
            }
        });
    }

    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) return 0;
        try {
            // SỬA ĐỊNH DẠNG TỪ dd/MM/yyyy THÀNH yyyy-MM-dd
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);

            if (birthDate == null) return 0;

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

            // Kiểm tra xem đã đến sinh nhật năm nay chưa
            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }
            return age;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void setActiveTab(TextView active, TextView inactive1, TextView inactive2) {
        // Nút được chọn -> Nền xanh, chữ trắng
        active.setBackgroundResource(R.drawable.bg_nav_active);
        active.setTextColor(Color.WHITE);

        // Nút không chọn -> Trong suốt, chữ xám
        inactive1.setBackgroundColor(Color.TRANSPARENT);
        inactive1.setTextColor(Color.parseColor("#757575"));

        inactive2.setBackgroundColor(Color.TRANSPARENT);
        inactive2.setTextColor(Color.parseColor("#757575"));
    }

    // =========================================================
    // HÀM SETUP GIAO DIỆN BIỂU ĐỒ BAN ĐẦU
    // =========================================================
//    private void setupChartAppearance() {
//        lineChartBMI.getDescription().setEnabled(false); // Ẩn chữ mô tả
//        lineChartBMI.getLegend().setEnabled(false); // Ẩn chú thích
//        lineChartBMI.setDrawGridBackground(false);
//        lineChartBMI.getAxisRight().setEnabled(false); // Ẩn cột số bên phải
//
//        XAxis xAxis = lineChartBMI.getXAxis();
//        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
//        xAxis.setDrawGridLines(false); // Ẩn đường kẻ dọc dọc
//        xAxis.setTextColor(Color.parseColor("#9E9E9E"));
//
//        lineChartBMI.getAxisLeft().setDrawGridLines(true); // Giữ đường kẻ ngang
//        lineChartBMI.getAxisLeft().setTextColor(Color.parseColor("#9E9E9E"));
//    }

    private void setupChartAppearance() {
        lineChartBMI.getDescription().setEnabled(false); // Ẩn chữ mô tả
        lineChartBMI.getLegend().setEnabled(false); // Ẩn chú thích
        lineChartBMI.setDrawGridBackground(false);
        lineChartBMI.getAxisRight().setEnabled(false); // Ẩn cột số bên phải

        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // Ẩn đường kẻ dọc dọc
        xAxis.setTextColor(Color.parseColor("#9E9E9E"));

        // ==========================================
        // CÁCH CHỮA BỆNH ĐÈ SỐ: Thêm khoảng đệm 2 đầu trục X
        // ==========================================
        xAxis.setSpaceMin(0.3f); // Đẩy điểm đầu thụt vào trong (cách trục Y)
        xAxis.setSpaceMax(0.3f); // Đẩy điểm cuối thụt lùi lại không dính mép phải

        lineChartBMI.getAxisLeft().setDrawGridLines(true); // Giữ đường kẻ ngang
        lineChartBMI.getAxisLeft().setTextColor(Color.parseColor("#9E9E9E"));

        // Đẩy đường line xa khỏi trần và sàn để nó lơ lửng đẹp mắt
        lineChartBMI.getAxisLeft().setSpaceTop(20f);
        lineChartBMI.getAxisLeft().setSpaceBottom(20f);
    }

    // =========================================================
    // HÀM CẬP NHẬT DỮ LIỆU THẬT LÊN BIỂU ĐỒ
    // =========================================================
    private void updateChartData(String filterType) {
        if (currentBmiLogs == null || currentBmiLogs.isEmpty()) {
            lineChartBMI.clear();
            lineChartBMI.setNoDataText("Chưa có dữ liệu BMI để hiển thị.");
            lineChartBMI.invalidate();
            return;
        }

        ArrayList<Entry> entries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();

        // Xác định số lượng mốc muốn vẽ dựa vào Tab đang chọn
        int limit = 0;
        if (filterType.equals("DAY")) limit = 7;         // 7 lần đo gần nhất
        else if (filterType.equals("WEEK")) limit = 14;  // 14 lần đo gần nhất
        else limit = currentBmiLogs.size();              // Lấy toàn bộ (Tháng/Tất cả)

        // Cắt mảng (Lấy từ dưới lên để lấy các record mới nhất)
        int startIndex = Math.max(0, currentBmiLogs.size() - limit);
        List<BmiLog> displayLogs = currentBmiLogs.subList(startIndex, currentBmiLogs.size());

        // Bộ chuyển đổi chuỗi ngày tháng từ DB (VD: 2025-10-25T14:30:00) sang định dạng ngắn (25/10)
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        for (int i = 0; i < displayLogs.size(); i++) {
            BmiLog log = displayLogs.get(i);

            // Xử lý lấy giá trị Y (BMI)
            float bmiValue = log.getBmiValue() != null ? log.getBmiValue().floatValue() : 0f;
            entries.add(new Entry(i, bmiValue));

            // Xử lý lấy giá trị X (Ngày hiển thị)
            try {
                if (log.getRecordedAt() != null) {
                    Date date = inputFormat.parse(log.getRecordedAt());
                    labels.add(outputFormat.format(date));
                } else {
                    labels.add("N/A");
                }
            } catch (Exception e) {
                labels.add("--");
            }
        }

        // CẤU HÌNH TRỤC X ĐỂ HIỂN THỊ CHỮ NGÀY THÁNG
        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        // Cấu hình đường vẽ
        LineDataSet dataSet = new LineDataSet(entries, "Chỉ số BMI");
        dataSet.setColor(android.graphics.Color.parseColor("#4DAA9A"));
        dataSet.setCircleColor(android.graphics.Color.parseColor("#4DAA9A"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(android.graphics.Color.parseColor("#212121"));

        // Thêm nền mờ dưới đường line cho biểu đồ hiện đại hơn
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#4DAA9A"));
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        lineChartBMI.setData(lineData);
        lineChartBMI.invalidate();
        lineChartBMI.animateX(400);
    }
    private void showUpdateBMIDialog() {
        if (currentUser == null) {
            Toast.makeText(this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khởi tạo Dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_bmi, null);
        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText edtWeight = dialogView.findViewById(R.id.edtUpdateWeight);
        EditText edtHeight = dialogView.findViewById(R.id.edtUpdateHeight);
        TextView btnCancel = dialogView.findViewById(R.id.btnCancelUpdate);
        TextView btnSave = dialogView.findViewById(R.id.btnSaveUpdate);

        if (currentUser.getWeight() != null && currentUser.getWeight() > 0) {
            edtWeight.setText(String.valueOf(currentUser.getWeight()));
        }
        if (currentUser.getHeight() != null && currentUser.getHeight() > 0) {
            edtHeight.setText(String.valueOf(currentUser.getHeight()));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String wStr = edtWeight.getText().toString().trim();
            String hStr = edtHeight.getText().toString().trim();

            if (wStr.isEmpty() || hStr.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Vui lòng nhập đầy đủ Cân nặng và Chiều cao", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double newWeight = Double.parseDouble(wStr);
                double newHeight = Double.parseDouble(hStr);

                User updateData = new User();
                updateData.setWeight(newWeight);
                updateData.setHeight(newHeight);

                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

                btnSave.setText("Đang lưu...");
                btnSave.setEnabled(false);

                // 1. CẬP NHẬT PROFILE
                apiService.updateUserProfile("eq." + username, updateData).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {

                            // 2. TÍNH BMI
                            double heightM = newHeight / 100.0;
                            double newBmi = newWeight / (heightM * heightM);

                            // Lấy thời gian hiện tại
                            String currentDateFull = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                            String todayDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            // 3. KIỂM TRA XEM HÔM NAY ĐÃ CÓ RECORD NÀO CHƯA
                            BmiLog todayLog = null;
                            if (currentBmiLogs != null && !currentBmiLogs.isEmpty()) {
                                // Lấy record cuối cùng (mới nhất)
                                BmiLog lastLog = currentBmiLogs.get(currentBmiLogs.size() - 1);
                                if (lastLog.getRecordedAt() != null && lastLog.getRecordedAt().startsWith(todayDateOnly)) {
                                    todayLog = lastLog; // Tìm thấy record của hôm nay!
                                }
                            }

                            // 4. XỬ LÝ LƯU (GHI ĐÈ NẾU CÓ, TẠO MỚI NẾU KHÔNG)
                            if (todayLog != null) {
                                // ĐÃ CÓ -> UPDATE BẢN GHI CỦA HÔM NAY
                                BmiLog updatePayload = new BmiLog(null, null, newWeight, newHeight, newBmi, currentDateFull);

                                apiService.updateBmiLog("eq." + todayLog.getId(), updatePayload).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        finishUpdate(dialog, "Đã cập nhật BMI hôm nay!");
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        finishUpdate(dialog, "Lưu biểu đồ thất bại!");
                                    }
                                });
                            } else {
                                // CHƯA CÓ -> TẠO BẢN GHI MỚI CHO NGÀY MỚI
                                BmiLog newLog = new BmiLog(UUID.randomUUID().toString(), currentUser.getId(), newWeight, newHeight, newBmi, currentDateFull);

                                apiService.saveBmiLog(newLog).enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        finishUpdate(dialog, "Đã thêm mốc BMI mới!");
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        finishUpdate(dialog, "Lưu biểu đồ thất bại!");
                                    }
                                });
                            }

                        } else {
                            btnSave.setText("Lưu thay đổi");
                            btnSave.setEnabled(true);
                            Toast.makeText(HomeActivity.this, "Lỗi cập nhật. Vui lòng thử lại!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        btnSave.setText("Lưu thay đổi");
                        btnSave.setEnabled(true);
                        Toast.makeText(HomeActivity.this, "Lỗi mạng!", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (NumberFormatException e) {
                Toast.makeText(HomeActivity.this, "Vui lòng nhập số hợp lệ!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    // Hàm phụ trợ để đóng Dialog và Tải lại data cho code gọn gàng
    private void finishUpdate(android.app.AlertDialog dialog, String message) {
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        loadUserData();
    }
}