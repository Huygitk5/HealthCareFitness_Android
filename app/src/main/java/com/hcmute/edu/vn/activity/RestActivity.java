package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;

public class RestActivity extends AppCompatActivity {

    private TextView tvTimer, tvNextExerciseName, tvTopSkip;
    private MaterialButton btnMinus20, btnPlus20, btnSkipRest;
    private android.widget.ProgressBar progressBarTimer;

    private int timeLeft = 20;
    private int maxTime = 20;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest);
        
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F4F7F6"));
        androidx.core.view.WindowInsetsControllerCompat wic = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(true);

        tvTimer = findViewById(R.id.tvTimer);
        tvNextExerciseName = findViewById(R.id.tvNextExerciseName);
        tvTopSkip = findViewById(R.id.tvTopSkip);
        btnMinus20 = findViewById(R.id.btnMinus20);
        btnPlus20 = findViewById(R.id.btnPlus20);
        btnSkipRest = findViewById(R.id.btnSkipRest);
        progressBarTimer = findViewById(R.id.progressBarTimer);

        String nextExercise = getIntent().getStringExtra("NEXT_EXERCISE_NAME");
        if (nextExercise != null && !nextExercise.isEmpty()) {
            tvNextExerciseName.setText(nextExercise);
        }

        updateTimerUI();

        btnMinus20.setOnClickListener(v -> {
            timeLeft -= 20;
            if (timeLeft < 1) timeLeft = 1;
            updateTimerUI();
        });

        btnPlus20.setOnClickListener(v -> {
            timeLeft += 20;
            if (timeLeft > maxTime) maxTime = timeLeft;
            updateTimerUI();
        });

        btnSkipRest.setOnClickListener(v -> finishRestAndGoNext());
        tvTopSkip.setOnClickListener(v -> finishRestAndGoNext());

        startTimer();
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeft > 0) {
                    timeLeft--;
                    updateTimerUI();
                    timerHandler.postDelayed(this, 1000);
                } else {
                    finishRestAndGoNext();
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void updateTimerUI() {
        tvTimer.setText(String.valueOf(timeLeft));
        // Cập nhật Progress Bar (giảm dần)
        int progress = (int) (((float) timeLeft / maxTime) * 100);
        progressBarTimer.setProgress(progress);
    }

    private void finishRestAndGoNext() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}