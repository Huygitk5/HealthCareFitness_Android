package com.hcmute.edu.vn.activity;

import static com.hcmute.edu.vn.util.FitnessCalculator.adjustCaloriesWeekly;

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
import com.hcmute.edu.vn.model.UserMedicalCondition;
import com.hcmute.edu.vn.receiver.WeightReminderReceiver;
import com.hcmute.edu.vn.util.ChatbotHelper;
import com.hcmute.edu.vn.util.FitnessCalculator;

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

    private NewsAdapter newsAdapter;
    private List<News> currentNewsList = new ArrayList<>();

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

        btnChartDay.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "DAY");
        });

        btnChartWeek.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "WEEK");
        });

        btnChartMonth.setOnClickListener(v -> {
            if (currentUser != null) fetchBmiHistory(currentUser.getId(), "MONTH");
        });

        btnNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(HomeActivity.this, "Bạn có 0 thông báo mới", Toast.LENGTH_SHORT).show();
            }
        });

        ChatbotHelper.setupChatbotFAB(this, fabChatbot);

        // =========================================================
        // SETUP RECYCLER VIEW CHO ACTIVITIES (Bài tập)
        // =========================================================
        ArrayList<Exercise> activityList = new ArrayList<>();

        activityList.add(new Exercise(UUID.randomUUID().toString(), "Giảm Mỡ Thừa ⚡⚡", "Bài tập giúp đốt mỡ", 1, 1, 3, "20 mins", "", String.valueOf(R.drawable.workout_1), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Tăng Cơ 💪", "Xây dựng sức mạnh", 2, 2, 4, "30 mins", "", String.valueOf(R.drawable.workout_2), null));
        activityList.add(new Exercise(UUID.randomUUID().toString(), "Yoga Buổi Sáng 🧘", "Thư giãn tinh thần", 3, 1, 1, "15 mins", "", String.valueOf(R.drawable.workout_3), null));

        ActivityAdapter activityAdapter = new ActivityAdapter(this, activityList);
        LinearLayoutManager activityLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvActivities.setLayoutManager(activityLayoutManager);
        rvActivities.setAdapter(activityAdapter);

        // =========================================================
        // SETUP RECYCLER VIEW CHO NEWS (Tin tức - Đã xóa dummy data)
        // =========================================================
        LinearLayoutManager newsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvNews.setLayoutManager(newsLayoutManager);

        // Gắn Adapter tạm thời rỗng, sẽ đổ data vào sau khi gọi API
        newsAdapter = new NewsAdapter(currentNewsList); // Chú ý: NewsAdapter dùng constructor 1 tham số
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
            new Handler().postDelayed(() -> {
                showUpdateBMIDialog();
            }, 1000);
            getIntent().removeExtra("OPEN_UPDATE_BMI");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);

        if (username != null && !username.isEmpty()) {
            loadUserData();
        }
    }

    private void loadUserData() {
        if (username == null || username.isEmpty()) return;

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getUserByUsername("eq." + username, "*,user_medical_conditions(*)").enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    currentUser = response.body().get(0);

                    // 1. HIỂN THỊ THÔNG TIN LÊN BẢNG TÌNH TRẠNG
                    tvGreeting.setText(currentUser.getName() != null ? currentUser.getName() : username);
                    double heightCm = currentUser.getHeight() != null ? currentUser.getHeight() : 0.0;
                    double weightKg = currentUser.getWeight() != null ? currentUser.getWeight() : 0.0;
                    int age = calculateAge(currentUser.getDateOfBirth());

                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                            .putFloat("USER_WEIGHT", (float) weightKg)
                            .apply();

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

                    // 2. GỌI API LẤY LỊCH SỬ BMI
                    fetchBmiHistory(currentUser.getId(), "DAY");

                    // =======================================================
                    // 3. GỌI API LẤY BÀI BÁO CÁ NHÂN HÓA (DÒNG MỚI THÊM)
                    // =======================================================
                    List<Integer> conditionIds = new ArrayList<>();
                    if (currentUser.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : currentUser.getUserMedicalConditions()) {
                            if (umc.getConditionId() != null) conditionIds.add(umc.getConditionId());
                        }
                    }

                    if (conditionIds.isEmpty()) {
                        fetchGeneralNews(apiService); // Không bệnh -> Báo chung
                    } else {
                        fetchNewsByConditions(apiService, conditionIds); // Có bệnh -> Báo đúng bệnh
                    }

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
    // HÀM GỌI API BÀI BÁO THEO BỆNH LÝ
    // =========================================================
    private void fetchNewsByConditions(SupabaseApiService api, List<Integer> conditionIds) {
        StringBuilder query = new StringBuilder("in.(");
        for (int i = 0; i < conditionIds.size(); i++) {
            query.append(conditionIds.get(i));
            if (i < conditionIds.size() - 1) query.append(",");
        }
        query.append(")");

        api.getPersonalizedNews(query.toString(), "*").enqueue(new Callback<List<News>>() {
            @Override
            public void onResponse(Call<List<News>> call, Response<List<News>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    setupNewsRecyclerView(response.body());
                } else {
                    fetchGeneralNews(api); // Dự phòng
                }
            }
            @Override public void onFailure(Call<List<News>> call, Throwable t) {
                fetchGeneralNews(api);
            }
        });
    }

    // =========================================================
    // HÀM GỌI API BÀI BÁO CHUNG (NGẪU NHIÊN 10 BÀI)
    // =========================================================
    private void fetchGeneralNews(SupabaseApiService api) {
        api.getGeneralNews("*", 10).enqueue(new Callback<List<News>>() {
            @Override
            public void onResponse(Call<List<News>> call, Response<List<News>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setupNewsRecyclerView(response.body());
                }
            }
            @Override public void onFailure(Call<List<News>> call, Throwable t) {}
        });
    }

    private void setupNewsRecyclerView(List<News> news) {
        currentNewsList.clear();
        currentNewsList.addAll(news);
        if (newsAdapter == null) {
            newsAdapter = new NewsAdapter(currentNewsList);
            rvNews.setAdapter(newsAdapter);
        } else {
            newsAdapter.notifyDataSetChanged();
        }
    }

    // =========================================================
    // HÀM LẤY LỊCH SỬ BMI TỪ SUPABASE THEO KHOẢNG THỜI GIAN
    // =========================================================
    private void fetchBmiHistory(String userId, String period) {
        if (userId == null || userId.isEmpty()) return;

        Calendar calStart = Calendar.getInstance();
        Calendar calEnd = Calendar.getInstance();

        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);

        switch (period) {
            case "DAY":
                calStart.add(Calendar.DAY_OF_YEAR, -6);
                break;
            case "WEEK":
                calStart.add(Calendar.WEEK_OF_YEAR, -4);
                break;
            case "MONTH":
                calStart.add(Calendar.MONTH, -6);
                break;
        }
        calStart.set(Calendar.HOUR_OF_DAY, 0);
        calStart.set(Calendar.MINUTE, 0);
        calStart.set(Calendar.SECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        String gteDate = "gte." + sdf.format(calStart.getTime());
        String lteDate = "lte." + sdf.format(calEnd.getTime());

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getUserBmiLogsByDateRange("eq." + userId, gteDate, lteDate, "*", "recorded_at.asc").enqueue(new Callback<List<BmiLog>>() {
            @Override
            public void onResponse(Call<List<BmiLog>> call, Response<List<BmiLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentBmiLogs = response.body();

                    if ("DAY".equals(period)) setActiveTab(btnChartDay, btnChartWeek, btnChartMonth);
                    else if ("WEEK".equals(period)) setActiveTab(btnChartWeek, btnChartDay, btnChartMonth);
                    else if ("MONTH".equals(period)) setActiveTab(btnChartMonth, btnChartDay, btnChartWeek);

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
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void setActiveTab(TextView active, TextView inactive1, TextView inactive2) {
        active.setBackgroundResource(R.drawable.bg_nav_active);
        active.setTextColor(Color.WHITE);

        inactive1.setBackgroundColor(Color.TRANSPARENT);
        inactive1.setTextColor(Color.parseColor("#757575"));

        inactive2.setBackgroundColor(Color.TRANSPARENT);
        inactive2.setTextColor(Color.parseColor("#757575"));
    }

    private void setupChartAppearance() {
        lineChartBMI.getDescription().setEnabled(false);
        lineChartBMI.getLegend().setEnabled(false);
        lineChartBMI.setDrawGridBackground(false);
        lineChartBMI.getAxisRight().setEnabled(false);

        XAxis xAxis = lineChartBMI.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(Color.parseColor("#9E9E9E"));

        xAxis.setSpaceMin(0.3f);
        xAxis.setSpaceMax(0.3f);

        lineChartBMI.getAxisLeft().setDrawGridLines(true);
        lineChartBMI.getAxisLeft().setTextColor(Color.parseColor("#9E9E9E"));

        lineChartBMI.getAxisLeft().setSpaceTop(20f);
        lineChartBMI.getAxisLeft().setSpaceBottom(20f);
    }

    private void updateChartData(String filterType) {
        if (currentBmiLogs == null || currentBmiLogs.isEmpty()) {
            lineChartBMI.clear();
            lineChartBMI.setNoDataText("Chưa có dữ liệu BMI để hiển thị.");
            lineChartBMI.invalidate();
            return;
        }

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

        LinkedHashMap<String, ArrayList<Float>> groupedData = new LinkedHashMap<>();

        for (BmiLog log : currentBmiLogs) {
            float bmi = log.getBmiValue() != null ? log.getBmiValue().floatValue() : 0f;
            String key = "";
            try {
                if (log.getRecordedAt() != null) {
                    Date date = inputFormat.parse(log.getRecordedAt());
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);

                    if (filterType.equals("DAY")) {
                        key = dayFormat.format(date);
                    } else if (filterType.equals("WEEK")) {
                        key = "Tuần " + cal.get(Calendar.WEEK_OF_YEAR);
                    } else if (filterType.equals("MONTH")) {
                        key = "Tháng " + (cal.get(Calendar.MONTH) + 1);
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

                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                        .putFloat("USER_WEIGHT", (float) newWeight)
                        .apply();

                User updateData = new User();
                updateData.setWeight(newWeight);
                updateData.setHeight(newHeight);

                // Kiểm tra điều chỉnh kcal vào cuối tuần
                boolean isCalorieChanged = false;
                Calendar todayCal = Calendar.getInstance();
                boolean isSunday = (todayCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY);

                // Nếu là Chủ nhật và có đủ dữ liệu lịch sử cân nặng
                if (isSunday && currentBmiLogs != null && currentBmiLogs.size() >= 2) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                        Date now = new Date();

                        // Tìm bản ghi cân nặng cách đây khoảng 7 ngày (Chủ nhật tuần trước)
                        BmiLog weekAgoLog = null;
                        for (int i = currentBmiLogs.size() - 1; i >= 0; i--) {
                            if (currentBmiLogs.get(i).getRecordedAt() == null) continue;

                            Date logDate = sdf.parse(currentBmiLogs.get(i).getRecordedAt());
                            long diffInDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now.getTime() - logDate.getTime());

                            // Lấy mốc cách đây từ 6 đến 8 ngày
                            if (diffInDays >= 6 && diffInDays <= 8) {
                                weekAgoLog = currentBmiLogs.get(i);
                                break;
                            }
                        }

                        // Nếu tìm thấy cân nặng tuần trước và đã có mức Calo hiện tại
                        if (weekAgoLog != null && currentUser.getCurrentDailyCalories() != null && currentUser.getCurrentDailyCalories() > 0) {
                            double oldWeight = weekAgoLog.getWeight() != null ? weekAgoLog.getWeight() : newWeight;
                            double oldCalo = currentUser.getCurrentDailyCalories();
                            int goalId = currentUser.getFitnessGoalId() != null ? currentUser.getFitnessGoalId() : 3;
                            String gender = currentUser.getGender() != null ? currentUser.getGender() : "Male";

                            // Lấy các chỉ số cần thiết để tính TDEE
                            int age = calculateAge(currentUser.getDateOfBirth());
                            int activityIndex = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("ACTIVITY_INDEX", 2);

                            // Tính BMR và TDEE dựa trên cân nặng cũ (hoặc mới tùy bạn, ở đây lấy cũ cho chuẩn 1 tuần trước)
                            double bmr = FitnessCalculator.calcBMR(oldWeight, newHeight, age, gender);
                            double tdee = FitnessCalculator.calcTDEE(bmr, activityIndex);

                            // GỌI THUẬT TOÁN ĐIỀU CHỈNH (trong FitnessCalculator)
                            double adjustedCalo = adjustCaloriesWeekly(oldCalo, tdee, goalId, oldWeight, newWeight, gender);

                            // Nếu Calo bị thay đổi do kết quả tăng/giảm cân không đạt KPI
                            if (Math.abs(adjustedCalo - oldCalo) > 5) { // Chỉ đổi khi lệch > 5 kcal
                                updateData.setCurrentDailyCalories(adjustedCalo);
                                isCalorieChanged = true;

                                // Nếu đang giảm mỡ mà bị đẩy Calo lên cao -> Báo hiệu đang giảm lố
                                if (goalId == 1 && adjustedCalo > oldCalo) {
                                    Toast.makeText(HomeActivity.this,
                                            "⚠️ Bạn đang giảm cân quá nhanh! Hệ thống đã tăng nhẹ lượng thức ăn để bảo vệ sức khỏe và cơ bắp.",
                                            Toast.LENGTH_LONG).show();
                                }

                                // Đánh cờ báo hiệu cho NutritionActivity sinh lại thực đơn
                                getSharedPreferences("UserPrefs", MODE_PRIVATE).edit()
                                        .putBoolean("CALORIES_CHANGED", true)
                                        .apply();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // tài khoản mới chưa có data
                if (!isCalorieChanged && (currentUser.getCurrentDailyCalories() == null || currentUser.getCurrentDailyCalories() == 0)) {
                    int age = calculateAge(currentUser.getDateOfBirth());
                    String gender = currentUser.getGender() != null ? currentUser.getGender() : "Male";
                    int activityIndex = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("ACTIVITY_INDEX", 2);
                    int goalId = currentUser.getFitnessGoalId() != null ? currentUser.getFitnessGoalId() : 3;
                    String goalName = goalId == 1 ? "giảm mỡ" : (goalId == 2 ? "tăng cơ" : "giữ dáng");
                    boolean isBeginner = (currentUser.getUserExperienceId() != null && currentUser.getUserExperienceId() == 1);

                    double bmr = com.hcmute.edu.vn.util.FitnessCalculator.calcBMR(newWeight, newHeight, age, gender);
                    double tdee = com.hcmute.edu.vn.util.FitnessCalculator.calcTDEE(bmr, activityIndex);

                    com.hcmute.edu.vn.util.FitnessCalculator.FitnessResult result =
                            com.hcmute.edu.vn.util.FitnessCalculator.calculate(goalName, newWeight, currentUser.getTarget(), tdee, gender, isBeginner);

                    updateData.setCurrentDailyCalories(result.dailyCalories);

                }

                // Gọi api lưu vào supa
                SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
                btnSave.setText("Đang lưu...");
                btnSave.setEnabled(false);

                // Gọi API cập nhật Profile
                apiService.updateUserProfile("eq." + username, updateData).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {

                            // LƯU LỊCH SỬ BMI NHƯ BÌNH THƯỜNG
                            double heightM = newHeight / 100.0;
                            double newBmi = newWeight / (heightM * heightM);
                            String currentDateFull = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());
                            String todayDateOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                            BmiLog todayLog = null;
                            if (currentBmiLogs != null && !currentBmiLogs.isEmpty()) {
                                BmiLog lastLog = currentBmiLogs.get(currentBmiLogs.size() - 1);
                                if (lastLog.getRecordedAt() != null && lastLog.getRecordedAt().startsWith(todayDateOnly)) {
                                    todayLog = lastLog;
                                }
                            }

                            if (todayLog != null) {
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

    private void finishUpdate(AlertDialog dialog, String message) {
        Toast.makeText(HomeActivity.this, message, Toast.LENGTH_SHORT).show();
        dialog.dismiss();
        loadUserData();
    }

    private void setupDailyWeightReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, WeightReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 200, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, 7);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }
}