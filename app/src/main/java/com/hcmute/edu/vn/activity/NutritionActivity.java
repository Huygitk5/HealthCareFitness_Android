package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.CalendarAdapter;
import com.hcmute.edu.vn.adapter.DailyMealAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.UserDailyMeal;
import com.hcmute.edu.vn.model.UserMedicalCondition;
import com.hcmute.edu.vn.receiver.MealNotificationReceiver;
import com.hcmute.edu.vn.util.ChatbotHelper;
import com.hcmute.edu.vn.util.FitnessCalculator;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NutritionActivity extends AppCompatActivity {

    private RecyclerView rvCalendar;
    private RecyclerView rvBreakfast, rvLunch, rvDinner;
    private TextView tvEmptyBreakfast, tvEmptyLunch, tvEmptyDinner;
    private TextView btnAddBreakfast, btnAddLunch, btnAddDinner;

    private CircularProgressIndicator progressCalories;
    private LinearProgressIndicator progressCarb, progressProtein, progressFat;
    private TextView tvTotalCalories, tvTargetCalories, tvTotalCarb, tvTotalProtein, tvTotalFat;
    private TextView tvAllergiesWarning;
    private CardView cardAllergiesWarning;

    private String username, userId;
    private Date selectedDate;
    private SimpleDateFormat apiDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private DailyMealAdapter breakfastAdapter, lunchAdapter, dinnerAdapter;
    private List<UserDailyMeal> breakfastList = new ArrayList<>();
    private List<UserDailyMeal> lunchList = new ArrayList<>();
    private List<UserDailyMeal> dinnerList = new ArrayList<>();

    private List<String> currentAllergies = new ArrayList<>();

    private double targetCalories = 0.0;
    private String currentGoalName = ""; // "giảm" / "tăng" / "giữ"
    private double targetCarb = 0.0;
    private double targetProtein = 0.0;
    private double targetFat = 0.0;
    private boolean isGeneratingMeals = false;

    private TextView tvWeekLabel, btnPrevWeek, btnNextWeek;
    private Calendar currentWeekBase;

    // Survey variables
    private CardView cardSurvey;
    private RadioGroup rgSurvey;
    private RadioButton rbEnough, rbMissed, rbNone;
    private EditText edtCustomFood;
    private Button btnSubmitSurvey;
    private TextView btnClearSurvey;

    private ActivityResultLauncher<Intent> addMealLauncher;

    private List<String> restrictedIngredientIds = new ArrayList<>();

    List<String> userAllergies = new ArrayList<>();

    private ImageButton btnTickBreakfast, btnTickLunch, btnTickDinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nutrition);

        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        username = pref.getString("KEY_USER", null);
        userId = pref.getString("KEY_USER_ID", "");

        selectedDate = new Date();
        currentWeekBase = Calendar.getInstance();
        currentWeekBase.setFirstDayOfWeek(Calendar.MONDAY);
        currentWeekBase.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        initViews();
        setupCalendar();
        setupMealAdapters();
        setupClickListeners();

        loadUserAllergies();

        setupBottomNavigation();

        FloatingActionButton fabChatbot = findViewById(R.id.fabChatbot);
        ChatbotHelper.setupChatbotFAB(this, fabChatbot);

        addMealLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        loadMealsForSelectedDate();
                    }
                }
        );

        if (getIntent().hasExtra("EXTRA_ALLERGIES")) {
            userAllergies = getIntent().getStringArrayListExtra("EXTRA_ALLERGIES");
        }
        if (userAllergies == null) {
            userAllergies = new ArrayList<>();
        }

        setupDaily8PMAlarm();
    }
    private void setupDaily8PMAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, MealNotificationReceiver.class);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 100, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        // Nếu đã qua 20h hôm nay, set cho 20h ngày mai
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // KIỂM TRA PHIÊN BẢN VÀ QUYỀN TRƯỚC KHI SET ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            } else {
                // KHÔNG có quyền -> Lùi về dùng báo thức linh hoạt (chấp nhận trễ 5-10 phút)
                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
                Toast.makeText(this, "Hệ thống có thể báo thức trễ vài phút để tiết kiệm pin", Toast.LENGTH_SHORT).show();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        if (pref.getBoolean("TARGET_CHANGED", false)) {
            pref.edit().putBoolean("TARGET_CHANGED", false).apply();
            showRegenerateMealDialog();
        }
        else if (pref.getBoolean("ALLERGY_DIRTY", false)) {
            new android.os.Handler().postDelayed(this::loadUserAllergies, 300);
            pref.edit().putBoolean("ALLERGY_DIRTY", false).apply();
        }
    }

    private void showRegenerateMealDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Mục tiêu đã thay đổi! 🎯")
                .setMessage("Thực đơn hiện tại có thể không còn phù hợp với lượng Calo mới.\n\nBạn có muốn AI xóa sạch và thiết kế lại thực đơn từ đầu không?")
                .setPositiveButton("Tạo lại từ đầu", (dialog, which) -> {
                    Toast.makeText(this, "Đang dọn dẹp thực đơn cũ...", Toast.LENGTH_SHORT).show();
                    SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
                    apiService.deleteMealsByUserId("eq." + userId).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            isGeneratingMeals = false;
                            breakfastList.clear();
                            lunchList.clear();
                            dinnerList.clear();
                            loadUserAllergies();
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            isGeneratingMeals = false;
                            loadUserAllergies();
                        }
                    });
                })
                .setNegativeButton("Giữ thực đơn cũ", (dialog, which) -> {
                    loadUserAllergies();
                })
                .setCancelable(false)
                .show();
    }

    private void initViews() {
        rvCalendar = findViewById(R.id.rvCalendar);
        rvBreakfast = findViewById(R.id.rvBreakfast);
        rvLunch = findViewById(R.id.rvLunch);
        rvDinner = findViewById(R.id.rvDinner);
        tvEmptyBreakfast = findViewById(R.id.tvEmptyBreakfast);
        tvEmptyLunch = findViewById(R.id.tvEmptyLunch);
        tvEmptyDinner = findViewById(R.id.tvEmptyDinner);
        btnAddBreakfast = findViewById(R.id.btnAddBreakfast);
        btnAddLunch = findViewById(R.id.btnAddLunch);
        btnAddDinner = findViewById(R.id.btnAddDinner);
        progressCalories = findViewById(R.id.progressCalories);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTargetCalories = findViewById(R.id.tvTargetCalories);
        progressCarb = findViewById(R.id.progressCarb);
        tvTotalCarb = findViewById(R.id.tvTotalCarb);
        progressProtein = findViewById(R.id.progressProtein);
        tvTotalProtein = findViewById(R.id.tvTotalProtein);
        progressFat = findViewById(R.id.progressFat);
        tvTotalFat = findViewById(R.id.tvTotalFat);
        tvAllergiesWarning = findViewById(R.id.tvAllergiesWarning);
        cardAllergiesWarning = findViewById(R.id.cardAllergiesWarning);
        tvWeekLabel = findViewById(R.id.tvWeekLabel);
        btnPrevWeek = findViewById(R.id.btnPrevWeek);
        btnNextWeek = findViewById(R.id.btnNextWeek);

        cardSurvey = findViewById(R.id.cardSurvey);
        rgSurvey = findViewById(R.id.rgSurvey);
        rbEnough = findViewById(R.id.rbEnough);
        rbMissed = findViewById(R.id.rbMissed);
        rbNone = findViewById(R.id.rbNone);
        edtCustomFood = findViewById(R.id.edtCustomFood);
        btnSubmitSurvey = findViewById(R.id.btnSubmitSurvey);
        btnClearSurvey = findViewById(R.id.btnClearSurvey);
        btnTickBreakfast = findViewById(R.id.btnTickBreakfast);
        btnTickLunch = findViewById(R.id.btnTickLunch);
        btnTickDinner = findViewById(R.id.btnTickDinner);
    }

    private void setupCalendar() {
        List<Date> dates = new ArrayList<>();
        Calendar cal = (Calendar) currentWeekBase.clone();
        int selectedIndex = 0;
        SimpleDateFormat matchFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String targetDateStr = matchFormat.format(selectedDate);

        for (int i = 0; i < 7; i++) {
            dates.add(cal.getTime());
            if (matchFormat.format(cal.getTime()).equals(targetDateStr)) selectedIndex = i;
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        updateMonthLabel(selectedDate);
        CalendarAdapter calendarAdapter = new CalendarAdapter(this, dates, selectedIndex, date -> {
            selectedDate = date;
            updateMonthLabel(date);
            loadMealsForSelectedDate();
        });

        rvCalendar.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCalendar.setAdapter(calendarAdapter);
        rvCalendar.scrollToPosition(selectedIndex);
    }

    private void updateMonthLabel(Date date) {
        if (tvWeekLabel == null) return;
        tvWeekLabel.setText("Tháng " + new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(date));
    }

    private void setupMealAdapters() {
        DailyMealAdapter.OnMealItemListener mealListener = new DailyMealAdapter.OnMealItemListener() {
            @Override public void onDeleteClick(UserDailyMeal meal) { deleteMealFromDatabase(meal); }
            @Override public void onSwapClick(UserDailyMeal meal) { handleSwapMeal(meal); }
        };
        breakfastAdapter = new DailyMealAdapter(this, breakfastList, mealListener);
        lunchAdapter = new DailyMealAdapter(this, lunchList, mealListener);
        dinnerAdapter = new DailyMealAdapter(this, dinnerList, mealListener);

        rvBreakfast.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvLunch.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});
        rvDinner.setLayoutManager(new LinearLayoutManager(this){ @Override public boolean canScrollVertically() { return false; }});

        rvBreakfast.setAdapter(breakfastAdapter);
        rvLunch.setAdapter(lunchAdapter);
        rvDinner.setAdapter(dinnerAdapter);
    }

    private void setupClickListeners() {
        btnAddBreakfast.setOnClickListener(v -> openMealSearch("BREAKFAST"));
        btnAddLunch.setOnClickListener(v -> openMealSearch("LUNCH"));
        btnAddDinner.setOnClickListener(v -> openMealSearch("DINNER"));
        btnTickBreakfast.setOnClickListener(v -> toggleMealStatus("BREAKFAST", btnTickBreakfast, breakfastAdapter));
        btnTickLunch.setOnClickListener(v -> toggleMealStatus("LUNCH", btnTickLunch, lunchAdapter));
        btnTickDinner.setOnClickListener(v -> toggleMealStatus("DINNER", btnTickDinner, dinnerAdapter));
        if (btnPrevWeek != null) btnPrevWeek.setOnClickListener(v -> shiftWeek(-7));
        if (btnNextWeek != null) btnNextWeek.setOnClickListener(v -> shiftWeek(7));
        setupSurveyListeners();
    }

    private void toggleMealStatus(String mealType, ImageButton btn, DailyMealAdapter adapter) {
        String dateStr = apiDateFormat.format(selectedDate);
        String key = "MEAL_LOG_" + userId + "_" + dateStr + "_" + mealType;
        SharedPreferences pref = getSharedPreferences("MealLogs", MODE_PRIVATE);

        boolean isLogged = !pref.getBoolean(key, false); // Đảo trạng thái
        pref.edit().putBoolean(key, isLogged).apply();

        // Cập nhật giao diện
        updateTickUI(btn, isLogged);
        adapter.setMealLogged(isLogged);

        // Kiểm tra để hiện khảo sát
        checkSurveyVisibility();
    }

    private void updateTickUI(ImageButton btn, boolean isLogged) {
        btn.setColorFilter(isLogged ? Color.parseColor("#4DAA9A") : Color.parseColor("#BDBDBD"));
    }

    private void checkSurveyVisibility() {
        if (userId == null || userId.isEmpty() || cardSurvey == null) return;

        String dateStr = apiDateFormat.format(selectedDate);
        String todayStr = apiDateFormat.format(new Date());
        boolean isToday = dateStr.equals(todayStr);

        SharedPreferences prefLogs = getSharedPreferences("MealLogs", MODE_PRIVATE);
        boolean bLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_BREAKFAST", false);
        boolean lLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_LUNCH", false);
        boolean dLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_DINNER", false);

        // Đếm số bữa đã tick
        int checkedCount = 0;
        if (bLog) checkedCount++;
        if (lLog) checkedCount++;
        if (dLog) checkedCount++;

        // Kiểm tra xem đã hoàn thành khảo sát hôm nay chưa
        boolean surveyDone = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                .getBoolean("SURVEY_" + userId + "_" + dateStr, false);

        Calendar rightNow = Calendar.getInstance();
        boolean isPast8PM = rightNow.get(Calendar.HOUR_OF_DAY) >= 20;

        // ĐIỀU KIỆN HIỂN THỊ: Hôm nay + Chưa làm khảo sát + (Đã tick ít nhất 1 bữa HOẶC Đã qua 20h)
        if (isToday && !surveyDone && (checkedCount > 0 || isPast8PM)) {
            cardSurvey.setVisibility(View.VISIBLE);
            updateSurveyFormState(checkedCount); // Cập nhật trạng thái disable/enable
        } else {
            cardSurvey.setVisibility(View.GONE);
        }
    }

    /**
     * Hàm cập nhật trạng thái Disable/Enable của form dựa trên số bữa đã tick
     */
    private void updateSurveyFormState(int count) {
        // Vô hiệu hóa tất cả trước
        rbEnough.setEnabled(false);
        rbMissed.setEnabled(false);
        rbNone.setEnabled(false);

        // Bật và tự động chọn theo số lượng tick
        if (count == 3) {
            rbEnough.setEnabled(true);
            rbEnough.setChecked(true);
        } else if (count > 0) { // 1 hoặc 2 bữa
            rbMissed.setEnabled(true);
            rbMissed.setChecked(true);
        } else { // 0 bữa (chỉ rớt vào case này nếu đã qua 20h)
            rbNone.setEnabled(true);
            rbNone.setChecked(true);
        }
    }

    private void setupSurveyListeners() {
        if (rgSurvey == null) return;

        rgSurvey.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId != -1) {
                edtCustomFood.setEnabled(true);
                edtCustomFood.setHint("Nhập món ăn ngoài kế hoạch (nếu có)...");
                btnClearSurvey.setVisibility(View.VISIBLE);
            }
        });

        btnClearSurvey.setOnClickListener(v -> {
            rgSurvey.clearCheck();
            edtCustomFood.setEnabled(true);
            edtCustomFood.setHint("Hoặc nhập món ăn (VD: 2 bát phở bò)...");
            btnClearSurvey.setVisibility(View.GONE);
        });

        btnSubmitSurvey.setOnClickListener(v -> {
            int checkedId = rgSurvey.getCheckedRadioButtonId();
            String customFood = edtCustomFood.getText().toString().trim();

            // Xử lý trường hợp user không chọn gì cả
            if (checkedId == -1 && customFood.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn trạng thái hoặc nhập món ăn!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Xây dựng Prompt thông minh gửi cho AI
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Mục tiêu Calo hôm nay của tôi là ")
                    .append(Math.round(targetCalories)).append(" kcal. ");

            // Gắn trạng thái số lượng bữa ăn
            if (checkedId == R.id.rbEnough) {
                promptBuilder.append("Tôi đã ăn ĐỦ số bữa yêu cầu. ");
            } else if (checkedId == R.id.rbMissed) {
                promptBuilder.append("Tôi đã ăn THIẾU bữa so với yêu cầu. ");
            } else if (checkedId == R.id.rbNone) {
                promptBuilder.append("Hôm nay tôi CHƯA ĂN bữa nào trong kế hoạch. ");
            }

            // Gắn thêm thông tin món ăn ngoài luồng (nếu có)
            if (!customFood.isEmpty()) {
                promptBuilder.append("Tuy nhiên, thực tế hôm nay tôi ăn: '").append(customFood).append("'. ");
                promptBuilder.append("Hãy ước lượng tổng lượng calo của các món này, so sánh với mục tiêu calo của tôi và đưa ra lời khuyên (đóng vai trò là một chuyên gia dinh dưỡng).");

                // Chuyển sang Chatbot để AI xử lý
                Intent intent = new Intent(this, ChatbotActivity.class);
                intent.putExtra("EXTRA_SURVEY_TEXT", promptBuilder.toString());
                startActivity(intent);
                saveSurveyDoneStatus();
            } else {
                // Nếu KHÔNG nhập món ăn ngoài, chỉ dùng logic khen/chê cục bộ như cũ
                String message = "";
                if (checkedId == R.id.rbEnough) message = "Tuyệt vời, bạn đã bám sát kế hoạch! Hãy giữ vững phong độ nhé!";
                else if (checkedId == R.id.rbMissed) message = "Hôm nay bạn ăn thiếu bữa rồi. Ngày mai nhớ chú ý ăn uống đầy đủ nhé!";
                else if (checkedId == R.id.rbNone) message = "Báo động đỏ! Bạn chưa ăn bữa nào. Hãy nạp năng lượng ngay đi nào!";

                showCustomDialog(message);
            }
        });
    }

    private void showCustomDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_custom_dialog, null);
        builder.setView(dialogView);

        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        Button btnClose = dialogView.findViewById(R.id.btnDialogClose);
        ImageView imgIcon = dialogView.findViewById(R.id.imgDialogIcon);

        tvMessage.setText(message);

        if (message.contains("Tuyệt vời")) {
            imgIcon.setImageResource(R.drawable.ic_check);
        }

        AlertDialog alertDialog = builder.create();

        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> {
            saveSurveyDoneStatus();
            alertDialog.dismiss();
        });

        alertDialog.show();
    }

    private void saveSurveyDoneStatus() {
        if (userId != null && !userId.isEmpty()) {
            String targetDate = apiDateFormat.format(selectedDate);
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            pref.edit().putBoolean("SURVEY_" + userId + "_" + targetDate, true).apply();
            checkSurveyVisibility();
        }
    }

    private void shiftWeek(int days) {
        currentWeekBase.add(Calendar.DAY_OF_YEAR, days);
        Calendar sdCal = Calendar.getInstance(); sdCal.setTime(selectedDate);
        sdCal.add(Calendar.DAY_OF_YEAR, days);
        selectedDate = sdCal.getTime();
        setupCalendar();
        loadMealsForSelectedDate();
    }

    private void openMealSearch(String mealType) {
        Intent intent = new Intent(NutritionActivity.this, MealSearchActivity.class);
        intent.putExtra("EXTRA_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", mealType);
        intent.putStringArrayListExtra("EXTRA_ALLERGIES", new ArrayList<>(currentAllergies));
        addMealLauncher.launch(intent);
    }

    private void loadMealsForSelectedDate() {
        if (userId == null || userId.isEmpty()) return;
        String formattedDate = apiDateFormat.format(selectedDate);
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        syncMealTickStates();
        checkSurveyVisibility();

        apiService.getDailyMeals("eq." + userId, "eq." + formattedDate, "*, foods(*)").enqueue(new Callback<List<UserDailyMeal>>() {
            @Override
            public void onResponse(Call<List<UserDailyMeal>> call, Response<List<UserDailyMeal>> response) {
                breakfastList.clear(); lunchList.clear(); dinnerList.clear();
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    for (UserDailyMeal meal : response.body()) {
                        if ("BREAKFAST".equalsIgnoreCase(meal.getMealType())) breakfastList.add(meal);
                        else if ("LUNCH".equalsIgnoreCase(meal.getMealType())) lunchList.add(meal);
                        else if ("DINNER".equalsIgnoreCase(meal.getMealType())) dinnerList.add(meal);
                    }
                    updateMealUI();
                    calculateAndDisplayTotals(response.body());
                } else {
                    updateMealUI();
                    calculateAndDisplayTotals(new ArrayList<>());
                    Calendar today = Calendar.getInstance(); today.set(Calendar.HOUR_OF_DAY, 0);
                    Calendar selected = Calendar.getInstance(); selected.setTime(selectedDate);
                    if (!selected.before(today) && !isGeneratingMeals && targetCalories > 0) triggerAIGeneration();
                }
            }
            @Override public void onFailure(Call<List<UserDailyMeal>> call, Throwable t) {}
        });
    }

    private void syncMealTickStates() {
        String dateStr = apiDateFormat.format(selectedDate);
        SharedPreferences pref = getSharedPreferences("MealLogs", MODE_PRIVATE);

        // Đọc trạng thái của ngày được chọn
        boolean bLog = pref.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_BREAKFAST", false);
        boolean lLog = pref.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_LUNCH", false);
        boolean dLog = pref.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_DINNER", false);

        // Cập nhật UI nút tick
        updateTickUI(btnTickBreakfast, bLog);
        updateTickUI(btnTickLunch, lLog);
        updateTickUI(btnTickDinner, dLog);

        // Cập nhật độ mờ cho Adapter
        breakfastAdapter.setMealLogged(bLog);
        lunchAdapter.setMealLogged(lLog);
        dinnerAdapter.setMealLogged(dLog);
    }

    private void triggerAIGeneration() {
        isGeneratingMeals = true;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        java.util.Map<String, String> queryMap = new java.util.HashMap<>();
        queryMap.put("select", "*, food_ingredients(*, ingredients(*))");

        apiService.searchFoods(queryMap).enqueue(new Callback<List<Food>>() {
            @Override
            public void onResponse(Call<List<Food>> call, Response<List<Food>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    autoGenerateWeeklyMeals(res.body());
                } else {
                    isGeneratingMeals = false;
                }
            }
            @Override public void onFailure(Call<List<Food>> call, Throwable t) {
                isGeneratingMeals = false;
            }
        });
    }

    private void autoGenerateWeeklyMeals(List<Food> allFoods) {
        if (allFoods == null || allFoods.isEmpty() || targetCalories <= 0) {
            isGeneratingMeals = false;
            return;
        }

        // --- LỌC THỰC PHẨM DỊ ỨNG ---
        List<Food> safeFoods = filterSafeFoods(allFoods);
        if (safeFoods.isEmpty()) safeFoods = allFoods;

        // --- PHÂN LOẠI MÓN ĂN THEO GOAL ---
        // Giảm mỡ  : ưu tiên protein cao, carb thấp
        // Tăng cơ  : ưu tiên carb & protein cao
        // Giữ dáng : cân bằng
        List<Food> breakfastPool = new ArrayList<>();
        List<Food> lunchPool = new ArrayList<>();
        List<Food> dinnerPool = new ArrayList<>();

        for (Food food : safeFoods) {
            double cal = food.getCalories() != null ? food.getCalories() : 0;
            if (cal <= 0) continue;

            double protein = food.getProteinG() != null ? food.getProteinG() : 0;
            double carb    = food.getCarbG()    != null ? food.getCarbG()    : 0;
            double fat     = food.getFatG()     != null ? food.getFatG()     : 0;

            // Tính "protein density" = protein% trong tổng macro
            double totalMacro = protein + carb + fat;
            double proteinPct = totalMacro > 0 ? protein / totalMacro : 0;

            boolean isHighProtein = proteinPct >= 0.3;   // ≥30% protein
            boolean isLowCalDense = cal < 350;            // Nhẹ — phù hợp bữa sáng

            // Phân loại đơn giản
            if (isLowCalDense) {
                breakfastPool.add(food);
            } else if (isHighProtein) {
                lunchPool.add(food);    // Bữa trưa: ưu tiên protein
                dinnerPool.add(food);
            } else {
                lunchPool.add(food);
                dinnerPool.add(food);
            }
        }

        // Fallback nếu pool rỗng
        if (breakfastPool.isEmpty()) breakfastPool.addAll(safeFoods);
        if (lunchPool.isEmpty())     lunchPool.addAll(safeFoods);
        if (dinnerPool.isEmpty())    dinnerPool.addAll(safeFoods);

        // --- TỶ LỆ PHÂN BỔ CALO THEO BỮA (theo goal) ---
        double bfRatio, lunchRatio, dinnerRatio;
        if (currentGoalName.contains("giảm")) {
            // Giảm mỡ: bữa tối nhẹ hơn (không muốn carb ban đêm)
            bfRatio = 0.35; lunchRatio = 0.40; dinnerRatio = 0.25;
        } else if (currentGoalName.contains("tăng")) {
            // Tăng cơ: bữa trưa nhiều nhất (carb cho tập luyện)
            bfRatio = 0.30; lunchRatio = 0.40; dinnerRatio = 0.30;
        } else {
            // Giữ dáng: đều nhau
            bfRatio = 0.25; lunchRatio = 0.40; dinnerRatio = 0.35;
        }

        double[] targets  = { targetCalories * bfRatio, targetCalories * lunchRatio, targetCalories * dinnerRatio };
        String[] types    = { "BREAKFAST", "LUNCH", "DINNER" };
        List<Food>[] pools = new List[]{ breakfastPool, lunchPool, dinnerPool };

        // --- GENERATE THỰC ĐƠN CHO 7 NGÀY  ---
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
        cal.set(Calendar.HOUR_OF_DAY, 12);
        int dow  = cal.get(Calendar.DAY_OF_WEEK);
        int diff = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -diff); // đầu tuần (Thứ Hai)

        List<UserDailyMeal> generatedMeals = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int day = 0; day < 7; day++) {
            String dateStr = sdf.format(cal.getTime());

            for (int mealIdx  = 0; mealIdx < 3; mealIdx++) {
                double mealTarget      = targets[mealIdx];
                List<Food> pool        = pools[mealIdx];
                double accumulatedCal  = 0;
                int attempts           = 0;
                int addedFoods         = 0;
                int maxFoodsPerMeal    = 4;

                while (accumulatedCal < mealTarget - 20 && addedFoods < maxFoodsPerMeal && attempts < 15) {
                    attempts++;
                    Food f      = pool.get((int)(Math.random() * pool.size()));
                    double fCal = (f.getCalories() != null && f.getCalories() > 0) ? f.getCalories() : 100;

                    // Tính số phần ăn cần để đạt phần còn thiếu, làm tròn 0.5
                    double needed     = mealTarget - accumulatedCal;
                    double rawMult    = needed / fCal;
                    double multiplier = Math.round(rawMult * 2.0) / 2.0;  // làm tròn đến 0.5
                    multiplier        = Math.max(0.5, Math.min(multiplier, 3.0));

                    if (accumulatedCal + fCal * multiplier > mealTarget + 50) {
                        multiplier = 0.5;
                    }

                    generatedMeals.add(new UserDailyMeal(
                            userId, dateStr, types[mealIdx], f.getId(), multiplier));
                    accumulatedCal += fCal * multiplier;
                    addedFoods++;
                }
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // --- LƯU LÊN SUPABASE ---
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.addMultipleDailyMeals(generatedMeals).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> res) {
                new android.os.Handler().postDelayed(() -> {
                    isGeneratingMeals = false;
                    loadMealsForSelectedDate();
                }, 1000);
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                isGeneratingMeals = false;
            }
        });
    }

    private void updateMealUI() {
        breakfastAdapter.updateList(breakfastList);
        lunchAdapter.updateList(lunchList);
        dinnerAdapter.updateList(dinnerList);

        tvEmptyBreakfast.setVisibility(breakfastList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyLunch.setVisibility(lunchList.isEmpty() ? View.VISIBLE : View.GONE);
        tvEmptyDinner.setVisibility(dinnerList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void calculateAndDisplayTotals(List<UserDailyMeal> allMeals) {
        double totalCal = 0, totalCarb = 0, totalProtein = 0, totalFat = 0;
        if (allMeals != null) {
            for (UserDailyMeal m : allMeals) {
                if (m.getFood() != null) {
                    double q = m.getQuantityMultiplier();
                    totalCal += (m.getFood().getCalories() != null ? m.getFood().getCalories() : 0) * q;
                    totalCarb += (m.getFood().getCarbG() != null ? m.getFood().getCarbG() : 0) * q;
                    totalProtein += (m.getFood().getProteinG() != null ? m.getFood().getProteinG() : 0) * q;
                    totalFat += (m.getFood().getFatG() != null ? m.getFood().getFatG() : 0) * q;
                }
            }
        }

        tvTotalCalories.setText(String.valueOf(Math.round(totalCal)));
        tvTargetCalories.setText("/ " + Math.round(targetCalories) + " Kcal");
        tvTotalCarb.setText(Math.round(totalCarb) + "g/" + Math.round(targetCarb) + "g");
        tvTotalProtein.setText(Math.round(totalProtein) + "g/" + Math.round(targetProtein) + "g");
        tvTotalFat.setText(Math.round(totalFat) + "g/" + Math.round(targetFat) + "g");

        if (targetCalories > 0) {
            animateProgress(progressCalories, (int) ((totalCal / targetCalories) * 100));
            animateProgress(progressCarb, (int) ((totalCarb / targetCarb) * 100));
            animateProgress(progressProtein, (int) ((totalProtein / targetProtein) * 100));
            animateProgress(progressFat, (int) ((totalFat / targetFat) * 100));
        }
    }

    private void animateProgress(android.widget.ProgressBar pb, int p) {
        ObjectAnimator.ofInt(pb, "progress", pb.getProgress(), p).setDuration(500).start();
    }

    private void loadUserAllergies() {
        if (username == null || username.isEmpty()) return;
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        String selectQuery = "*, user_medical_conditions(*, medical_conditions(*, condition_restricted_ingredients(*)))";

        apiService.getUserByUsername("eq." + username, selectQuery).enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> res) {
                if (res.isSuccessful() && res.body() != null && !res.body().isEmpty()) {
                    User u = res.body().get(0);
                    if (userId == null || userId.isEmpty()) {
                        userId = u.getId();
                        getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().putString("KEY_USER_ID", userId).apply();
                    }

                    if (u.getCurrentDailyCalories() != null) {
                        calculateTargetMacros(u.getCurrentDailyCalories(), u.getFitnessGoalId() != null ? u.getFitnessGoalId() : 3);
                    }

                    currentAllergies.clear();
                    restrictedIngredientIds.clear();

                    if (u.getUserMedicalConditions() != null) {
                        for (UserMedicalCondition umc : u.getUserMedicalConditions()) {
                            MedicalCondition mc = umc.getMedicalCondition();
                            if (mc != null) {
                                // 1. Lấy tên dị ứng để hiển thị UI
                                if (mc.getType() != null) {
                                    String type = mc.getType().toLowerCase();
                                    if (type.contains("allergy") || type.contains("dị ứng")) {
                                        currentAllergies.add(mc.getName());
                                    }
                                }
                                // 2. Lấy ID nguyên liệu cấm để AI né ra
                                if (mc.getRestrictedIngredients() != null) {
                                    for (com.hcmute.edu.vn.model.ConditionRestrictedIngredient cri : mc.getRestrictedIngredients()) {
                                        if (cri.getIngredientId() != null) {
                                            restrictedIngredientIds.add(cri.getIngredientId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    setupWarningDisplay(currentAllergies);
                    loadMealsForSelectedDate();
                }
            }
            @Override public void onFailure(Call<List<User>> call, Throwable t) {
                loadMealsForSelectedDate();
            }
        });
    }

    private List<Food> filterSafeFoods(List<Food> allFoods) {
        List<Food> safeFoods = new ArrayList<>();
        for (Food food : allFoods) {
            boolean isSafe = true;

            // Lọc theo tên món
            if (food.getName() != null) {
                String foodName = food.getName().toLowerCase();
                for (String allergy : currentAllergies) {
                    if (allergy != null && !allergy.isEmpty()) {
                        String keyword = allergy.toLowerCase()
                                .replace("dị ứng", "").replace("allergy", "").trim();
                        if (!keyword.isEmpty() && foodName.contains(keyword)) {
                            isSafe = false; break;
                        }
                    }
                }
            }

            // Lọc theo nguyên liệu
            if (isSafe && food.getFoodIngredients() != null) {
                for (com.hcmute.edu.vn.model.FoodIngredient fi : food.getFoodIngredients()) {
                    if (fi.getIngredientId() != null
                            && restrictedIngredientIds.contains(fi.getIngredientId())) {
                        isSafe = false; break;
                    }
                    if (fi.getIngredient() != null && fi.getIngredient().getName() != null) {
                        String ingName = fi.getIngredient().getName().toLowerCase();
                        for (String allergy : currentAllergies) {
                            if (allergy != null && !allergy.isEmpty()) {
                                String keyword = allergy.toLowerCase()
                                        .replace("dị ứng", "").replace("allergy", "").trim();
                                if (!keyword.isEmpty() && ingName.contains(keyword)) {
                                    isSafe = false; break;
                                }
                            }
                        }
                    }
                    if (!isSafe) break;
                }
            }

            if (isSafe) safeFoods.add(food);
        }
        return safeFoods;
    }

    private void setupWarningDisplay(List<String> dataList) {
        if (dataList.isEmpty()) {
            tvAllergiesWarning.setText("⚠️ Cần tránh: Không có");
            cardAllergiesWarning.setOnClickListener(null);
            return;
        }

        StringBuilder displayStr = new StringBuilder();
        for (int i = 0; i < dataList.size(); i++) {
            if (i < 3) displayStr.append("• ").append(dataList.get(i)).append("\n");
        }
        if (dataList.size() > 3) {
            int extra = dataList.size() - 3;
            displayStr.append("+ ").append(extra).append(" mục khác...");
        }
        tvAllergiesWarning.setText("⚠️ Cần tránh:\n" + displayStr.toString().trim());
        cardAllergiesWarning.setOnClickListener(v -> showCustomChipDialog("Thực phẩm cần tránh", dataList, true));
    }

    private void showCustomChipDialog(String title, List<String> items, boolean isAllergy) {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.layout_dialog_chips, null);
        TextView tvDialogTitle = dialogView.findViewById(R.id.tvDialogTitle);
        com.google.android.material.chip.ChipGroup chipGroupItems = dialogView.findViewById(R.id.chipGroupItems);
        com.google.android.material.button.MaterialButton btnDialogClose = dialogView.findViewById(R.id.btnDialogClose);

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
                chipGradient.setColors(new int[]{android.graphics.Color.parseColor("#FFE0B2"), android.graphics.Color.parseColor("#FFCCBC")});
                chip.setTextColor(android.graphics.Color.parseColor("#BF360C"));
            } else {
                chipGradient.setColors(new int[]{android.graphics.Color.parseColor("#E0F2F1"), android.graphics.Color.parseColor("#B2DFDB")});
                chip.setTextColor(android.graphics.Color.parseColor("#004D40"));
            }
            chip.setBackground(chipGradient);
            chipGroupItems.addView(chip);
        }

        btnDialogClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Tính macro targets từ dailyCalories + goalId.
     * Đồng thời lưu currentGoalName để dùng trong generate meal.
     */
    private void calculateTargetMacros(double dailyCalories, int goalId) {
        // Tỷ lệ macro theo mục tiêu
        double pRatio, cRatio, fRatio;
        if (goalId == 1) {          // Giảm mỡ: protein cao hơn, carb thấp hơn
            pRatio = 0.35; cRatio = 0.35; fRatio = 0.30;
            currentGoalName = "giảm";
        } else if (goalId == 2) {   // Tăng cơ: carb cao hơn để có năng lượng tập
            pRatio = 0.30; cRatio = 0.50; fRatio = 0.20;
            currentGoalName = "tăng";
        } else {                    // Giữ dáng
            pRatio = 0.30; cRatio = 0.40; fRatio = 0.30;
            currentGoalName = "giữ";
        }

        targetCalories = dailyCalories;
        targetProtein  = (dailyCalories * pRatio) / 4.0;
        targetCarb     = (dailyCalories * cRatio) / 4.0;
        targetFat      = (dailyCalories * fRatio) / 9.0;
    }

    private void deleteMealFromDatabase(UserDailyMeal meal) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.deleteDailyMeal("eq." + meal.getId()).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) { loadMealsForSelectedDate(); }
            @Override public void onFailure(Call<Void> call, Throwable t) {}
        });
    }

    private void handleSwapMeal(UserDailyMeal oldMeal) {
        if (oldMeal.getFood() == null) return;
        Intent intent = new Intent(this, MealSwapActivity.class);
        intent.putExtra("EXTRA_OLD_MEAL_ID", oldMeal.getId());
        intent.putExtra("EXTRA_TARGET_DATE", apiDateFormat.format(selectedDate));
        intent.putExtra("EXTRA_MEAL_TYPE", oldMeal.getMealType());
        intent.putExtra("EXTRA_TARGET_KCAL", (oldMeal.getFood().getCalories() != null ? oldMeal.getFood().getCalories() : 0.0) * oldMeal.getQuantityMultiplier());
        intent.putStringArrayListExtra("EXTRA_ALLERGIES", new ArrayList<>(currentAllergies));
        addMealLauncher.launch(intent);
    }

    private void setupBottomNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(v -> { startActivity(new Intent(this, HomeActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_workout).setOnClickListener(v -> { startActivity(new Intent(this, WorkoutJourneyActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
        findViewById(R.id.nav_profile).setOnClickListener(v -> { startActivity(new Intent(this, ProfileActivity.class).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)); overridePendingTransition(0, 0); });
    }
}