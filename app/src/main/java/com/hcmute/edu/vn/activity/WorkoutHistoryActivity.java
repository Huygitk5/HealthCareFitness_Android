package com.hcmute.edu.vn.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.SessionHistoryAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.UserWorkoutSession;

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

public class WorkoutHistoryActivity extends AppCompatActivity {

    private String userId;
    private List<UserWorkoutSession> allSessions = new ArrayList<>();
    private Set<String> workoutDates = new HashSet<>();
    private Calendar currentMonthCal;

    private TextView tvEmptyState, tvMonthYear, tvWeekRange, tvWeekSessionCount, tvTotalTime, tvTotalKcal;
    private CardView cardCalendar, cardWeekly;
    private GridLayout gridCalendar;
    private RecyclerView rvSessions;
    private ImageButton btnPrevMonth, btnNextMonth, btnBack;
    private MaterialButton btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_history);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");
        currentMonthCal = Calendar.getInstance();
        currentMonthCal.set(Calendar.DAY_OF_MONTH, 1);

        initViews();
        setupListeners();
        loadSessions();
    }

    private void initViews() {
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvMonthYear = findViewById(R.id.tvMonthYear);
        tvWeekRange = findViewById(R.id.tvWeekRange);
        tvWeekSessionCount = findViewById(R.id.tvWeekSessionCount);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvTotalKcal = findViewById(R.id.tvTotalKcal);
        
        cardCalendar = findViewById(R.id.cardCalendar);
        cardWeekly = findViewById(R.id.cardWeekly);
        gridCalendar = findViewById(R.id.gridCalendar);
        
        rvSessions = findViewById(R.id.rvSessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnBack = findViewById(R.id.btnBack);
        btnDone = findViewById(R.id.btnDone);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnDone.setOnClickListener(v -> finish());
        
        btnPrevMonth.setOnClickListener(v -> {
            currentMonthCal.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        
        btnNextMonth.setOnClickListener(v -> {
            currentMonthCal.add(Calendar.MONTH, 1);
            renderCalendar();
        });
    }

    private void loadSessions() {
        if (userId == null || userId.isEmpty()) return;

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.getSessionsByUser("eq." + userId, "*,workout_days(name,day_order)", "started_at.desc")
            .enqueue(new Callback<List<UserWorkoutSession>>() {
                @Override
                public void onResponse(Call<List<UserWorkoutSession>> call, Response<List<UserWorkoutSession>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        allSessions = response.body();
                        if (allSessions.isEmpty()) {
                            renderCalendar();
                            showEmptyState();
                            return;
                        }
                        
                        workoutDates.clear();
                        for (UserWorkoutSession s : allSessions) {
                            String dStr = s.getDateString();
                            if (!dStr.isEmpty()) workoutDates.add(dStr);
                        }
                        
                        renderCalendar();
                        renderWeeklyReport(new Date());
                    } else {
                        showEmptyState();
                    }
                }

                @Override
                public void onFailure(Call<List<UserWorkoutSession>> call, Throwable t) {
                    Toast.makeText(WorkoutHistoryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        cardWeekly.setVisibility(View.GONE);
        findViewById(R.id.tvWeekRange).setVisibility(View.GONE); 
        // Need to hide Báo cáo hàng tuần label as well but avoiding ID search here for simplicity or relying on parent
    }

    private void renderCalendar() {
        gridCalendar.removeAllViews();
        
        SimpleDateFormat monthYearFmt = new SimpleDateFormat("MM yyyy", Locale.getDefault());
        tvMonthYear.setText("thg " + monthYearFmt.format(currentMonthCal.getTime()));

        int firstDayOfWeek = currentMonthCal.get(Calendar.DAY_OF_WEEK);
        int offset = firstDayOfWeek - 1; // CN = 1 => offset 0
        
        int maxDay = currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
        
        int cellSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        
        for (int i = 0; i < offset; i++) {
            TextView emptyTv = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.height = cellSize;
            emptyTv.setLayoutParams(params);
            gridCalendar.addView(emptyTv);
        }

        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = (Calendar) currentMonthCal.clone();
        
        for (int day = 1; day <= maxDay; day++) {
            cal.set(Calendar.DAY_OF_MONTH, day);
            String dateStr = isoFmt.format(cal.getTime());
            
            cal.add(Calendar.DAY_OF_MONTH, -1);
            String prevDateStr = isoFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 2);
            String nextDateStr = isoFmt.format(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, -1); // restore
            
            boolean isWorkout = workoutDates.contains(dateStr);
            boolean prevWorkout = workoutDates.contains(prevDateStr);
            boolean nextWorkout = workoutDates.contains(nextDateStr);

            TextView tv = new TextView(this);
            tv.setText(String.valueOf(day));
            tv.setGravity(Gravity.CENTER);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.height = cellSize;
            tv.setLayoutParams(params);

            if (isWorkout) {
                tv.setTextColor(Color.WHITE);
                if (prevWorkout && nextWorkout) {
                    tv.setBackgroundResource(R.drawable.bg_streak_middle);
                } else if (prevWorkout) {
                    tv.setBackgroundResource(R.drawable.bg_streak_end);
                } else if (nextWorkout) {
                    tv.setBackgroundResource(R.drawable.bg_streak_start);
                } else {
                    tv.setBackgroundResource(R.drawable.bg_streak_single);
                }
                
                Date cloneDate = cal.getTime();
                tv.setOnClickListener(v -> renderWeeklyReport(cloneDate));
            } else {
                tv.setTextColor(Color.parseColor(cal.getTimeInMillis() > System.currentTimeMillis() ? "#E0E0E0" : "#000000"));
            }
            gridCalendar.addView(tv);
        }
    }

    private void renderWeeklyReport(Date dateInWeek) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateInWeek);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        Date weekStart = cal.getTime();
        
        cal.add(Calendar.DAY_OF_MONTH, 6);
        Date weekEnd = cal.getTime();
        
        SimpleDateFormat fmt = new SimpleDateFormat("dd 'thg' MM", Locale.getDefault());
        tvWeekRange.setText(fmt.format(weekStart) + " – " + fmt.format(weekEnd));
        
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String startStr = isoFmt.format(weekStart);
        String endStr = isoFmt.format(weekEnd);
        
        List<UserWorkoutSession> weekSessions = new ArrayList<>();
        long totalSeconds = 0;
        double totalKcal = 0;
        
        for (UserWorkoutSession s : allSessions) {
            String dStr = s.getDateString();
            if (dStr.compareTo(startStr) >= 0 && dStr.compareTo(endStr) <= 0) {
                weekSessions.add(s);
                totalSeconds += s.getDurationSeconds();
                totalKcal += s.getEstimatedKcal();
            }
        }
        
        tvWeekSessionCount.setText(weekSessions.size() + " Lần tập");
        
        if (totalSeconds < 60) {
            tvTotalTime.setText(totalSeconds + "s");
        } else {
            tvTotalTime.setText((totalSeconds / 60) + "m " + (totalSeconds % 60) + "s");
        }
        
        tvTotalKcal.setText(String.format(Locale.getDefault(), "%.1f Kcal", totalKcal));
        
        SessionHistoryAdapter adapter = new SessionHistoryAdapter(weekSessions);
        rvSessions.setAdapter(adapter);
    }
}
