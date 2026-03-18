package com.hcmute.edu.vn.activity;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hcmute.edu.vn.R;


public class WorkoutJourneyActivity extends AppCompatActivity {

    private ProgressBar pbOverallProgress, pbTodayCircular;
    private TextView tvProgressFraction, tvTodayPercent, tvTodayAction;
    private Button btnTodayAction;

    // Core state
    private int totalPlanDays = 30;
    private int completedPlanDays = 14;

    private int totalTodayExercises = 8;
    private int completedTodayExercises = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_journey);

        initViews();
        loadDynamicData();
        setupClickListeners();
    }

    private void initViews() {
        pbOverallProgress = findViewById(R.id.pbOverallProgress);
        pbTodayCircular = findViewById(R.id.pbTodayCircular);
        tvProgressFraction = findViewById(R.id.tvProgressFraction);
        tvTodayPercent = findViewById(R.id.tvTodayPercent);
        btnTodayAction = findViewById(R.id.btnTodayAction);

        // Setup RecyclerView here using your adapter...
    }

    private void loadDynamicData() {
        // 1. Calculate & Animate Plan Progress
        int planProgress = (int) (((float) completedPlanDays / totalPlanDays) * 100);
        tvProgressFraction.setText(completedPlanDays + " / " + totalPlanDays + " days completed");
        animateProgressBar(pbOverallProgress, planProgress, 1200);

        // 2. Calculate & Animate Today's Circular Progress
        int todayProgress = (int) (((float) completedTodayExercises / totalTodayExercises) * 100);
        tvTodayPercent.setText(todayProgress + "%");
        animateProgressBar(pbTodayCircular, todayProgress, 1500);

        // 3. Dynamic Button State Logic
        if (completedTodayExercises == 0) {
            btnTodayAction.setText("START");
            btnTodayAction.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_green));
        } else if (completedTodayExercises < totalTodayExercises) {
            btnTodayAction.setText("CONTINUE");
            btnTodayAction.setBackgroundColor(ContextCompat.getColor(this, R.color.orange_warning));
        } else {
            btnTodayAction.setText("DONE ✅");
            btnTodayAction.setBackgroundColor(ContextCompat.getColor(this, R.color.grey_locked));
            btnTodayAction.setEnabled(false);
        }
    }

    /**
     * The secret to a premium feel: smoothly animating progress changes.
     */
    private void animateProgressBar(ProgressBar progressBar, int targetProgress, long duration) {
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress);
        animation.setDuration(duration);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();
    }

    private void setupClickListeners() {
        btnTodayAction.setOnClickListener(v -> {
            // Launch the active workout session
            Intent intent = new Intent(this, ExerciseActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.layoutCustomWorkout).setOnClickListener(v -> {
            // Launch custom builder
            startActivity(new Intent(this, CustomExerciseSelectionActivity.class));
        });
    }
}