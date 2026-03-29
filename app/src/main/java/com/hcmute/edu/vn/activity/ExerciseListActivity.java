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
import androidx.core.view.WindowInsetsControllerCompat;
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

       WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        rvExercises = findViewById(R.id.rvExercises);
        ViewCompat.setOnApplyWindowInsetsListener(rvExercises, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rvExercises.setLayoutManager(new LinearLayoutManager(this));

        // Nhận ID ngày tập từ màn hình trước và tải dữ liệu mới nhất
        if (getIntent().hasExtra("EXTRA_EXERCISE_LIST")) {
            // Bài tập đã được lọc theo bệnh lý từ Journey HOẶC tập tự do
            exercises = (ArrayList<Exercise>) getIntent().getSerializableExtra("EXTRA_EXERCISE_LIST");
            TextView tvDayTitle = findViewById(R.id.tvDayTitle);
            String dayTitle = getIntent().getStringExtra("EXTRA_DAY_TITLE");
            if(tvDayTitle != null) tvDayTitle.setText(dayTitle != null ? dayTitle : "Tập Tự Do");

            adapter = new ExerciseAdapter(exercises);
            rvExercises.setAdapter(adapter);

        } else if (getIntent().hasExtra("EXTRA_DAY_ID")) {
            // KHÔNG có danh sách lọc sẵn → fetch trực tiếp từ API
            String dayId = getIntent().getStringExtra("EXTRA_DAY_ID");
            String dayTitle = getIntent().getStringExtra("EXTRA_DAY_TITLE");

            TextView tvDayTitle = findViewById(R.id.tvDayTitle);
            if(tvDayTitle != null) {
                tvDayTitle.setText(dayTitle != null ? dayTitle : "Đang tải...");
            }

            fetchExercisesForDay(dayId);
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Chuyển danh sách bài tập vào màn hình tập (ExerciseActivity)
        findViewById(R.id.btnStartWorkout).setOnClickListener(v -> {
            if (exercises == null || exercises.isEmpty()) {
                Toast.makeText(ExerciseListActivity.this, "Danh sách bài tập trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(ExerciseListActivity.this, ExerciseActivity.class);
            intent.putExtra("EXTRA_EXERCISE_LIST", exercises);

            if (getIntent().hasExtra("EXTRA_DAY_ID")) {
                intent.putExtra("EXTRA_DAY_ID", getIntent().getStringExtra("EXTRA_DAY_ID"));
            }
            if (getIntent().hasExtra("EXTRA_PLAN_ID")) {
                intent.putExtra("EXTRA_PLAN_ID", getIntent().getStringExtra("EXTRA_PLAN_ID"));
            }

            if (getIntent().hasExtra("IS_FREE_WORKOUT")) {
                intent.putExtra("IS_FREE_WORKOUT", getIntent().getBooleanExtra("IS_FREE_WORKOUT", false));
            }
            startActivity(intent);
        });
    }

    private void fetchExercisesForDay(String dayId) {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        String selectQuery = "*, workout_day_exercises(*, exercise:exercises(*))";

        apiService.getExercisesForSpecificDay("eq." + dayId, selectQuery).enqueue(new Callback<List<WorkoutDay>>() {
            @Override
            public void onResponse(Call<List<WorkoutDay>> call, Response<List<WorkoutDay>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WorkoutDay day = response.body().get(0);
                    List<Exercise> fetchedList = new ArrayList<>();

                    if (day.getExercises() != null) {
                        for (WorkoutDayExercise wde : day.getExercises()) {
                            Exercise ex = wde.getExercise();
                            if (ex != null) {
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
                        Toast.makeText(ExerciseListActivity.this, "Ngày này chưa có bài tập", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ExerciseListActivity.this, "Không thể lấy danh sách bài tập!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<WorkoutDay>> call, Throwable t) {
                Toast.makeText(ExerciseListActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}