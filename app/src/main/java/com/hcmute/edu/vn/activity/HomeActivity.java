package com.hcmute.edu.vn.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.adapter.ActivityAdapter;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.News;
import com.hcmute.edu.vn.adapter.NewsAdapter;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.receiver.WeightReminderReceiver;
import com.hcmute.edu.vn.util.ChatbotHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    FloatingActionButton fabChatbot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.home_view);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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
        fabChatbot = findViewById(R.id.fabChatbot);

        setupChartAppearance();

        // ĐÃ SỬA: Gọi API fetchBmiHistory mỗi khi chuyển Tab thay vì chỉ vẽ lại
        btnChartDay.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "DAY");
        });

        btnChartWeek.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "WEEK");
        });

        btnChartMonth.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "MONTH");
        });

        // 4. Sự kiện Click Chuông Thông báo
        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Bạn có 0 thông báo mới", Toast.LENGTH_SHORT).show();
            }
        });

        // Click và kéo thả FAB Chatbot (Helper)
        ChatbotHelper.setupChatbotFAB(this, fabChatbot);

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
                Intent intent = new Intent(HomeActivity.this, WorkoutJourneyActivity.class);
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
        setupDailyWeightReminder();
        if (getIntent() != null && getIntent().getBooleanExtra("OPEN_UPDATE_BMI", false)) {
            // Đợi 1 giây để app gọi API lấy currentUser xong rồi mới bật Pop-up
            new Handler().postDelayed(() -> {
                showUpdateBMIDialog();
            }, 1000);

            // Xóa tín hiệu đi để nếu người dùng xoay màn hình, pop-up không bị bật lại liên tục
            getIntent().removeExtra("OPEN_UPDATE_BMI");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // LẤY USERNAME TỪ SHAREDPREFS
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
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
                            tvBMIStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFCA28")));
                            tvBMIValue.setTextColor(Color.parseColor("#FFCA28"));
                        } else if (bmi < 23) {
                            tvBMIStatus.setText("Bình thường");
                            tvBMIStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                            tvBMIValue.setTextColor(Color.parseColor("#4CAF50"));
                        } else if (bmi < 25) {
                            tvBMIStatus.setText("Thừa cân");
                            tvBMIStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF9800")));
                            tvBMIValue.setTextColor(Color.parseColor("#FF9800"));
                        } else {
                            tvBMIStatus.setText("Béo phì");
                            tvBMIStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
                            tvBMIValue.setTextColor(Color.parseColor("#F44336"));
                        }
                    } else {
                        tvCurrentHeight.setText("-- cm");
                        tvCurrentWeight.setText("-- kg");
                        tvBMIValue.setText("--");
                        tvBMIStatus.setText("Chưa có");
                        tvBMIStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
                    }
                    tvCurrentAge.setText(age > 0 ? String.valueOf(age) : "--");

                    // =======================================================
                    // 2. GỌI API LẤY LỊCH SỬ BMI ĐỂ VẼ BIỂU ĐỒ (DÒNG MỚI THÊM)
                    // =======================================================
                    fetchBmiHistory(currentUser.getId(), "DAY");

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
    // HÀM LẤY LỊCH SỬ BMI TỪ SUPABASE THEO KHOẢNG THỜI GIAN
    // =========================================================
    private void fetchBmiHistory(String userId, String period) {
        if (userId == null || userId.isEmpty()) return;

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();

        // Thời gian kết thúc (lte) luôn là cuối ngày hôm nay
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);

        // Tính thời gian bắt đầu (gte) dựa theo Tab
        switch (period) {
            case "DAY":
                calStart.add(Calendar.DAY_OF_YEAR, -6); // 7 ngày gần nhất (tính cả hôm nay)
                break;
            case "WEEK":
                calStart.add(Calendar.WEEK_OF_YEAR, -4); // 4 tuần gần nhất
                break;
            case "MONTH":
                calStart.add(Calendar.MONTH, -6); // 6 tháng gần nhất
                break;
        }
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);

        // Format chuẩn yyyy-MM-dd'T'HH:mm:ss
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String gteDate = "gte." + sdf.format(calStart.getTime());
        String lteDate = "lte." + sdf.format(calEnd.getTime());

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        // DÙNG API MỚI CỦA BẠN Ở ĐÂY
        apiService.getUserBmiLogsByDateRange("eq." + userId, gteDate, lteDate, "*", "recorded_at.asc").enqueue(new Callback<List<BmiLog>>() {
            @Override
            public void onResponse(Call<List<BmiLog>> call, Response<List<BmiLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBmiLogs = response.body();

                    // Chuyển đổi màu sắc Tab tương ứng
                    if ("DAY".equals(period)) setActiveTab(btnChartDay, btnChartWeek, btnChartMonth);
                    else if ("WEEK".equals(period)) setActiveTab(btnChartWeek, btnChartDay, btnChartMonth);
                    else if ("MONTH".equals(period)) setActiveTab(btnChartMonth, btnChartDay, btnChartWeek);

                    // Vẽ biểu đồ
                    updateChartData(period);
                }
            }

            @Override
            public void onFailure(Call<List<BmiLog>> call, Throwable t) {
                Log.e("BMI_CHART", "Lỗi tải biểu đồ: " + t.getMessage());
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
    // HÀM GOM NHÓM DỮ LIỆU & VẼ BIỂU ĐỒ
    // =========================================================
    private void updateChartData(String filterType) {
        if (currentBmiLogs == null || currentBmiLogs.isEmpty()) {
            lineChartBMI.clear();
            lineChartBMI.setNoDataText("Chưa có dữ liệu BMI để hiển thị.");
            lineChartBMI.invalidate();
            return;
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        // Dùng LinkedHashMap để phân loại dữ liệu theo khung thời gian
        LinkedHashMap<String, ArrayList<Float>> groupedData = new LinkedHashMap<>();

        // Thuật toán chia nhóm
        for (BmiLog log : currentBmiLogs) {
            float bmi = log.getBmiValue() != null ? log.getBmiValue().floatValue() : 0f;
            String key = "";
            try {
                if (log.getRecordedAt() != null) {
                    Date date = inputFormat.parse(log.getRecordedAt());
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);

                    if (filterType.equals("DAY")) {
                        key = dayFormat.format(date); // In ra "14/03"
                    } else if (filterType.equals("WEEK")) {
                        key = "Tuần " + cal.get(Calendar.WEEK_OF_YEAR); // In ra "Tuần 11"
                    } else if (filterType.equals("MONTH")) {
                        key = "Tháng " + (cal.get(Calendar.MONTH) + 1); // In ra "Tháng 3"
                    }
                }
            } catch (Exception e) {
                key = "--";
            }

            if (!groupedData.containsKey(key)) {
                groupedData.put(key, new ArrayList<>());
            }
            groupedData.get(key).add(bmi);
        }

        ArrayList<Entry> entries = new ArrayList<>();
        final ArrayList<String> labels = new ArrayList<>();
        int index = 0;

        // Tính trung bình BMI cho từng nhóm
        for (Map.Entry<String, ArrayList<Float>> group : groupedData.entrySet()) {
            labels.add(group.getKey());

            float sum = 0;
            for (Float b : group.getValue()) {
                sum += b;
            }
            float avgBmi = sum / group.getValue().size();

            entries.add(new Entry(index, avgBmi));
            index++;
        }

        // Đổ dữ liệu chữ vào trục X
        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int idx = (int) value;
                if (idx >= 0 && idx < labels.size()) {
                    return labels.get(idx);
                }
                return "";
            }
        });

        LineDataSet dataSet = new LineDataSet(entries, "Chỉ số BMI");
        dataSet.setColor(Color.parseColor("#4DAA9A"));
        dataSet.setCircleColor(Color.parseColor("#4DAA9A"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTextColor(Color.parseColor("#212121"));

        // Làm tròn số trên biểu đồ
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4DAA9A"));
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_bmi, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
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
    private void finishUpdate(AlertDialog dialog, String message) {
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        loadUserData();
    }

    // Thêm hàm này vào HomeActivity.java
    private void setupDailyWeightReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, WeightReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 200, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 7); // Hẹn 7h sáng
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // Nếu lúc đăng nhập đã qua 7h sáng thì dời chuông sang ngày hôm sau
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            // Đặt lặp lại mỗi ngày (INTERVAL_DAY)
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }
}