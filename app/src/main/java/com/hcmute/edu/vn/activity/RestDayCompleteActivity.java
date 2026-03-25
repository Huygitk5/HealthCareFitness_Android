package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.UserWorkoutSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RestDayCompleteActivity extends AppCompatActivity {

    private MaterialButton btnComplete;
    private ImageButton btnBack;

    private String planId, dayId, userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rest_day_complete);

        // Sync status bar với nền trắng
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F4F7F6"));
        new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

        // Lấy dữ liệu từ Intent và SharedPreferences
        planId = getIntent().getStringExtra("EXTRA_PLAN_ID");
        dayId  = getIntent().getStringExtra("EXTRA_DAY_ID");
        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getString("KEY_USER_ID", "");

        btnBack     = findViewById(R.id.btnBack);
        btnComplete = findViewById(R.id.btnComplete);

        btnBack.setOnClickListener(v -> finish());

        btnComplete.setOnClickListener(v -> saveRestDaySession());
    }

    private void saveRestDaySession() {
        if (userId.isEmpty() || planId == null || dayId == null) {
            Toast.makeText(this, "Thiếu thông tin phiên nghỉ ngơi!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnComplete.setEnabled(false);
        btnComplete.setText("Đang lưu...");

        // Lấy thời gian hiện tại (cả start và finish đều là NOW vì chỉ check-in)
        String now = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(new Date());

        UserWorkoutSession session = new UserWorkoutSession(
                UUID.randomUUID().toString(),
                userId,
                planId,
                dayId,
                now,
                now,
                null
        );

        SupabaseApiService api = SupabaseClient.getClient().create(SupabaseApiService.class);
        api.saveWorkoutSession(session).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(RestDayCompleteActivity.this,
                            "Tuyệt vời! Bạn đã hoàn thành ngày nghỉ ngơi 🎉",
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Vẫn cho qua, không chặn user
                    Toast.makeText(RestDayCompleteActivity.this,
                            "Lưu thất bại (mã: " + response.code() + "), thử lại sau!",
                            Toast.LENGTH_SHORT).show();
                }
                finish();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(RestDayCompleteActivity.this,
                        "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
