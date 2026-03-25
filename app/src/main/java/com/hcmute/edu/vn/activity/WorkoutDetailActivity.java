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

        apiService.getWorkoutPlanByIdAndSort("eq." + planId, selectQuery, "day_order.asc")
                .enqueue(new Callback<List<WorkoutPlan>>() {
                    @Override
                    public void onResponse(Call<List<WorkoutPlan>> call, Response<List<WorkoutPlan>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            WorkoutPlan plan = response.body().get(0);

                            if (plan.getDays() != null) {
                                data.clear();
                                data.addAll(plan.getDays());

                                adapter = new WorkoutAdapter(data, planId);
                                rvWorkoutDays.setAdapter(adapter);
                            } else {
                                Toast.makeText(WorkoutDetailActivity.this, "Gói tập chưa có ngày nào!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(WorkoutDetailActivity.this, "Dữ liệu rỗng hoặc lỗi API", Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<List<WorkoutPlan>> call, Throwable t) {
                        Toast.makeText(WorkoutDetailActivity.this, "Lỗi kết nối mạng: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}