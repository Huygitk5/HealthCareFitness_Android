package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.ExerciseAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExerciseListActivity extends AppCompatActivity {

    private ArrayList<Exercise> exercises = new ArrayList<>();
    private RecyclerView rvExercises;
    private ExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_workout_exercise_list);

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        rvExercises = findViewById(R.id.rvExercises);
        ViewCompat.setOnApplyWindowInsetsListener(rvExercises, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvExercises.setLayoutManager(new LinearLayoutManager(this));

        // 1. Kiểm tra luồng: Đây là Tập Theo Ngày hay Tập Tự Do?
        if (getIntent().hasExtra("EXTRA_DAY_ID")) {
            // LUỒNG TẬP THEO NGÀY CỦA PLAN: Lấy Day ID và gọi API
            String dayId = getIntent().getStringExtra("EXTRA_DAY_ID");
            TextView tvDayTitle = findViewById(R.id.tvDayTitle);
            if(tvDayTitle != null) tvDayTitle.setText("Đang tải...");

            fetchExercisesForDay(dayId);

            // Sửa lại nhánh này trong onCreate()
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Nút START bắt đầu tập
        findViewById(R.id.btnStartWorkout).setOnClickListener(v -> {
            if (exercises == null || exercises.isEmpty()) {
                Toast.makeText(ExerciseListActivity.this, "Danh sách bài tập trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExerciseListActivity.this, ExerciseActivity.class);
            intent.putExtra("EXTRA_EXERCISE_LIST", exercises);

            if (getIntent().hasExtra("IS_FREE_WORKOUT")) {
                intent.putExtra("IS_FREE_WORKOUT", getIntent().getBooleanExtra("IS_FREE_WORKOUT", false));
            }
            startActivity(intent);
        });
    }

    // Hàm gọi API để nạp bài tập
    // Hàm gọi API để nạp bài tập (Đã được nâng cấp để hiển thị lỗi thật của Supabase)
    private void fetchExercisesForDay(String dayId) {
        try {
            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

            // ĐÃ SỬA: Bỏ chữ "exercises:" đi, gọi thẳng tên bảng trung gian
            String selectQuery = "*, workout_day_exercises(*, exercise:exercises(*))";

            apiService.getExercisesForSpecificDay("eq." + dayId, selectQuery).enqueue(new Callback<List<WorkoutDay>>() {
                @Override
                public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            WorkoutDay day = response.body().get(0);
                            TextView tvDayTitle = findViewById(R.id.tvDayTitle);
                            if(tvDayTitle != null) {
                                int dayOrder = day.getDayOrder() != null ? day.getDayOrder() : 1;
                                tvDayTitle.setText("Ngày " + dayOrder);
                            }

                            List<Exercise> fetchedList = new ArrayList<>();

                            // Gọi getExercises() theo đúng tên biến trong Model của bạn
                            if (day.getExercises() != null) {
                                for (WorkoutDayExercise wde : day.getExercises()) {
                                    Exercise ex = wde.getExercise();
                                    if (ex != null) {
                                        // Ép kiểu an toàn chống crash
                                        if (wde.getReps() != null) ex.setBaseRecommendedReps(String.valueOf(wde.getReps()));
                                        if (wde.getSets() != null) ex.setBaseRecommendedSets(wde.getSets());
                                        fetchedList.add(ex);
                                    }
                                }
                            }

                            exercises = new ArrayList<>(fetchedList);
                            adapter = new ExerciseAdapter(exercises);
                            rvExercises.setAdapter(adapter);

                            if (exercises.isEmpty()) {
                                Toast.makeText(ExerciseListActivity.this, "Ngày này là ngày nghỉ hoặc chưa có bài tập", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(ExerciseListActivity.this, "Dữ liệu trả về rỗng từ Server", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(ExerciseListActivity.this, "Lỗi xử lý dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {
                    Toast.makeText(ExerciseListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi gọi API: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}