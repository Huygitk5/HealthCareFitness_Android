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

    private TextView tvMonthYear, tvWeekRange, tvWeekSessionCount, tvTotalTime, tvTotalKcal;
    private View layoutEmptyState;
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
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
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
        layoutEmptyState.setVisibility(View.VISIBLE);
        cardWeekly.setVisibility(View.GONE);
        findViewById(R.id.tvWeekRange).setVisibility(View.GONE); 
    }

    private void renderCalendar() {
        gridCalendar.removeAllViews();

        int displayedYear = currentMonthCal.get(Calendar.YEAR);
        int displayedMonth = currentMonthCal.get(Calendar.MONTH);

        Calendar cal = Calendar.getInstance();
        cal.set(displayedYear, displayedMonth, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1; // 0=CN, 1=T2...
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < firstDayOfWeek; i++) {
            gridCalendar.addView(createEmptyCell());
        }

        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        for (int day = 1; day <= daysInMonth; day++) {
            String dateStr = String.format(Locale.getDefault(),
                "%04d-%02d-%02d", displayedYear, displayedMonth + 1, day);
            String prevDateStr = String.format(Locale.getDefault(),
                "%04d-%02d-%02d", displayedYear, displayedMonth + 1, day - 1);
            String nextDateStr = String.format(Locale.getDefault(),
                "%04d-%02d-%02d", displayedYear, displayedMonth + 1, day + 1);

            boolean isDone     = workoutDates.contains(dateStr);
            boolean isPrevDone = workoutDates.contains(prevDateStr);
            boolean isNextDone = workoutDates.contains(nextDateStr);
            boolean isToday    = (day == todayDay && displayedMonth == todayMonth
                                   && displayedYear == todayYear);
            boolean isFuture   = displayedYear > todayYear
                               || (displayedYear == todayYear
                                   && displayedMonth > todayMonth)
                               || (displayedYear == todayYear
                                   && displayedMonth == todayMonth
                                   && day > todayDay);

            View cell = createDayCell(day, isDone, isPrevDone, isNextDone, isToday, isFuture);
            
            if (isDone) {
                try {
                    SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date clickDate = isoFmt.parse(dateStr);
                    cell.setOnClickListener(v -> renderWeeklyReport(clickDate));
                } catch (Exception e){}
            }
            gridCalendar.addView(cell);
        }

        tvMonthYear.setText("thg " + (displayedMonth + 1) + " " + displayedYear);
    }

    private View createEmptyCell() {
        android.widget.FrameLayout cell = new android.widget.FrameLayout(this);
        int size = dpToPx(40);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width  = 0;
        params.height = size;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cell.setLayoutParams(params);
        return cell;
    }

    private View createDayCell(int day, boolean isDone, boolean isPrev, boolean isNext,
                                 boolean isToday, boolean isFuture) {
        android.widget.FrameLayout cell = new android.widget.FrameLayout(this);
        int size = dpToPx(40);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width  = 0;
        params.height = size;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        cell.setLayoutParams(params);

        if (isDone) {
            if (isPrev || isNext) {
                View band = new View(this);
                android.widget.FrameLayout.LayoutParams bandParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT, dpToPx(32));
                bandParams.gravity = android.view.Gravity.CENTER_VERTICAL;
                band.setLayoutParams(bandParams);

                if (isPrev && isNext) {
                    band.setBackgroundColor(Color.parseColor("#B5F0DF"));
                } else if (isPrev) {
                    band.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_streak_half_left));
                } else {
                    band.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_streak_half_right));
                }
                cell.addView(band);
            }

            View circle = new View(this);
            android.widget.FrameLayout.LayoutParams circleParams = new android.widget.FrameLayout.LayoutParams(size - dpToPx(4), size - dpToPx(4));
            circleParams.gravity = android.view.Gravity.CENTER;
            circle.setLayoutParams(circleParams);
            circle.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_day_single));
            cell.addView(circle);

            android.widget.ImageView checkIcon = new android.widget.ImageView(this);
            android.widget.FrameLayout.LayoutParams checkParams = new android.widget.FrameLayout.LayoutParams(dpToPx(16), dpToPx(16));
            checkParams.gravity = android.view.Gravity.CENTER;
            checkIcon.setLayoutParams(checkParams);
            checkIcon.setImageResource(R.drawable.ic_check);
            checkIcon.setColorFilter(Color.WHITE);
            cell.addView(checkIcon);

        } else {
            TextView tvDay = new TextView(this);
            android.widget.FrameLayout.LayoutParams tvParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
            tvDay.setLayoutParams(tvParams);
            tvDay.setText(String.valueOf(day));
            tvDay.setGravity(android.view.Gravity.CENTER);
            tvDay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

            if (isToday) {
                tvDay.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_day_today));
                tvDay.setTextColor(Color.parseColor("#4DAA9A"));
                tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (isFuture) {
                tvDay.setTextColor(Color.parseColor("#BDBDBD"));
            } else {
                tvDay.setTextColor(Color.parseColor("#1A1A1A"));
            }

            cell.addView(tvDay);
        }

        return cell;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
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
