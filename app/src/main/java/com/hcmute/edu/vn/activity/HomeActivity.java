package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
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

                // Nếu API gọi thành công và có dữ liệu trả về
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {

                    // Lấy User đầu tiên trong danh sách trả về
                    User currentUser = response.body().get(0);

                    // Cập nhật tên hiển thị (Đổi getFullName() thành getName())
                    tvGreeting.setText(currentUser.getName() != null ? currentUser.getName() : username);

                    // Lấy Chiều cao & Cân nặng (Xử lý an toàn vì Double có thể null trên DB)
                    double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                    double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;

                    // Tính tuổi (Đổi getDob() thành getDateOfBirth())
                    int age = calculateAge(currentUser.getDateOfBirth());

                    // Tính toán và hiển thị BMI
                    if (heightCm > 0 && weightKg > 0) {
                        tvCurrentHeight.setText(heightCm + " cm");
                        tvCurrentWeight.setText(weightKg + " kg");

                        double heightM = heightCm / 100.0;
                        double bmi = weightKg / (heightM * heightM);
                        tvBMIValue.setText(String.format("%.1f", bmi));

                        // Code set màu sắc BMI Status
                        if (bmi < 18.5) {
                            tvBMIStatus.setText("Thiếu cân");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#FF9800"));
                        } else if (bmi >= 18.5 && bmi < 23) {
                            tvBMIStatus.setText("Bình thường");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                        } else {
                            tvBMIStatus.setText("Béo phì");
                            tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#F44336")));
                            tvBMIValue.setTextColor(android.graphics.Color.parseColor("#F44336"));
                        }
                    } else {
                        // Hiển thị mặc định khi chưa có data
                        tvCurrentHeight.setText("-- cm");
                        tvCurrentWeight.setText("-- kg");
                        tvBMIValue.setText("--");
                        tvBMIStatus.setText("Chưa có");
                        // Trả về màu xám nếu chưa có data cho đỡ bị giữ màu cũ
                        tvBMIStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#9E9E9E")));
                        tvBMIValue.setTextColor(android.graphics.Color.parseColor("#9E9E9E"));
                    }

                    // Hiển thị tuổi
                    if (age > 0) tvCurrentAge.setText(String.valueOf(age));
                    else tvCurrentAge.setText("--");

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
    private void setupChartAppearance() {
        lineChartBMI.getDescription().setEnabled(false); // Ẩn chữ mô tả
        lineChartBMI.getLegend().setEnabled(false); // Ẩn chú thích
        lineChartBMI.setDrawGridBackground(false);
        lineChartBMI.getAxisRight().setEnabled(false); // Ẩn cột số bên phải

        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false); // Ẩn đường kẻ dọc dọc
        xAxis.setTextColor(Color.parseColor("#9E9E9E"));

        lineChartBMI.getAxisLeft().setDrawGridLines(true); // Giữ đường kẻ ngang
        lineChartBMI.getAxisLeft().setTextColor(Color.parseColor("#9E9E9E"));
    }

    // =========================================================
    // HÀM CẬP NHẬT DỮ LIỆU LÊN BIỂU ĐỒ TÙY THEO TAB
    // =========================================================
    private void updateChartData(String filterType) {
        ArrayList<Entry> entries = new ArrayList<>();
        final String[] labels; // Mảng chứa chữ hiển thị dưới trục X

        // Chú ý: Trục X giờ sẽ bắt đầu từ 0 để dễ dàng map với mảng labels
        if (filterType.equals("DAY")) {
            // Dữ liệu 5 ngày (x: 0->4)
            entries.add(new Entry(0, 23.5f));
            entries.add(new Entry(1, 23.4f));
            entries.add(new Entry(2, 23.4f));
            entries.add(new Entry(3, 23.2f));
            entries.add(new Entry(4, 23.1f));
            labels = new String[]{"Ngày 1", "Ngày 2", "Ngày 3", "Ngày 4", "Ngày 5"};

        } else if (filterType.equals("WEEK")) {
            // Dữ liệu 4 tuần (x: 0->3)
            entries.add(new Entry(0, 24.0f));
            entries.add(new Entry(1, 23.8f));
            entries.add(new Entry(2, 23.5f));
            entries.add(new Entry(3, 23.1f));
            labels = new String[]{"Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4"};

        } else {
            // Dữ liệu các tháng (x: 0->2)
            entries.add(new Entry(0, 25.0f));
            entries.add(new Entry(1, 24.5f));
            entries.add(new Entry(2, 23.1f));
            labels = new String[]{"Tháng 1", "Tháng 2", "Tháng 3"};
        }

        // ==========================================
        // CẤU HÌNH TRỤC X ĐỂ HIỂN THỊ CHỮ (Labels)
        // ==========================================
        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setGranularity(1f); // Ép chỉ hiện số nguyên, không hiện số thập phân như 1.5, 2.5
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value; // Ép từ float sang int
                // Đảm bảo index không bị vượt quá độ dài của mảng
                if (index >= 0 && index < labels.length) {
                    return labels[index];
                }
                return "";
            }
        });

        // Tạo đường vẽ (Line)
        LineDataSet dataSet = new LineDataSet(entries, "BMI");
        dataSet.setColor(android.graphics.Color.parseColor("#4DAA9A")); // Đường màu xanh lá
        dataSet.setCircleColor(android.graphics.Color.parseColor("#4DAA9A")); // Chấm tròn màu xanh
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true); // Hiện con số trên biểu đồ
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(android.graphics.Color.parseColor("#212121"));

        LineData lineData = new LineData(dataSet);
        lineChartBMI.setData(lineData);
        lineChartBMI.invalidate(); // Lệnh này giúp biểu đồ vẽ lại ngay lập tức
        lineChartBMI.animateX(500); // Thêm hiệu ứng chạy ngang ra cho xịn xò
    }
}