package com.hcmute.edu.vn.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.WorkoutAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkoutDetailActivity extends AppCompatActivity {

    private RecyclerView rvWorkoutDays;
    private WorkoutAdapter adapter;
    private List<WorkoutDay> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail_workout);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        rvWorkoutDays = findViewById(R.id.rvWorkoutDays);
        ViewCompat.setOnApplyWindowInsetsListener(rvWorkoutDays, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        rvWorkoutDays.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String planId = getIntent().getStringExtra("PLAN_ID");
        if (planId != null && !planId.isEmpty()) {
            fetchWorkoutPlan(planId);
        } else {
            Toast.makeText(this, "Không tìm thấy ID gói tập!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchWorkoutPlan(String planId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*,workout_days(*, workout_day_exercises(*, exercise:exercises(*)))";

        apiService.getWorkoutPlanById("eq." + planId, selectQuery).enqueue(new Callback<List<WorkoutPlan>>() {
            @Override
            public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && !response.body().isEmpty()) {
                        WorkoutPlan plan = response.body().get(0);

                        if (plan.getDays() != null) {
                            data.clear();

                            List<WorkoutDay> sortedDays = plan.getDays();
                            java.util.Collections.sort(sortedDays, (d1, d2) -> {
                                if (d1.getDayOrder() == null) return 1;
                                if (d2.getDayOrder() == null) return -1;
                                return Integer.compare(d1.getDayOrder(), d2.getDayOrder());
                            });

                            data.addAll(sortedDays);

                            adapter = new WorkoutAdapter(data);
                            rvWorkoutDays.setAdapter(adapter);
                        } else {
                            Toast.makeText(WorkoutDetailActivity.this, "Gói tập chưa có ngày nào!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(WorkoutDetailActivity.this, "Dữ liệu rỗng! Hãy kiểm tra lại ID hoặc tắt RLS trên Supabase", Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        String errorMsg = response.errorBody().string();
                        Toast.makeText(WorkoutDetailActivity.this, "Lỗi API: " + errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(WorkoutDetailActivity.this, "Lỗi truy vấn API!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                Toast.makeText(WorkoutDetailActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}