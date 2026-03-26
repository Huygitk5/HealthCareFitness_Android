package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.ExerciseSelectionAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Equipment;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomExerciseSelectionActivity extends AppCompatActivity {

    private ArrayList<Integer> receivedEquipmentIds;
    private ArrayList<Integer> receivedMuscleIds;

    private Button btnNextStep;
    private ProgressBar progressBarApi;
    private RecyclerView rvSelection;
    private ExerciseSelectionAdapter adapter;
    private TextView tvStep1Circle, tvStep1Title;
    private TextView tvStep2Circle, tvStep2Title;
    private TextView tvStep3Circle, tvStep3Title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        // Hứng danh sách nhiều ID từ Bước 2
        receivedEquipmentIds = getIntent().getIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS");
        receivedMuscleIds = getIntent().getIntegerArrayListExtra("SELECTED_MUSCLE_IDS");

        if (receivedEquipmentIds == null) receivedEquipmentIds = new ArrayList<>();
        if (receivedMuscleIds == null) receivedMuscleIds = new ArrayList<>();

        initViews();
        setupStepIndicator();
        setupUI();
        fetchAndFilterExercises();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(100);

        // Hiệu ứng ẩn nút ban đầu
        btnNextStep.setVisibility(View.GONE);
        btnNextStep.setTranslationY(200f);
        btnNextStep.setAlpha(0f);
        tvStep1Circle = findViewById(R.id.tvStep1Circle);
        tvStep1Title = findViewById(R.id.tvStep1Title);
        tvStep2Circle = findViewById(R.id.tvStep2Circle);
        tvStep2Title = findViewById(R.id.tvStep2Title);
        tvStep3Circle = findViewById(R.id.tvStep3Circle);
        tvStep3Title = findViewById(R.id.tvStep3Title);
    }

    private void setupStepIndicator() {
        // --- BƯỚC 1 (Đã hoàn thành) ---
        tvStep1Circle.setBackgroundResource(R.drawable.step_circle_selected);
        tvStep1Circle.setTextColor(android.graphics.Color.WHITE);
        tvStep1Title.setTextColor(android.graphics.Color.parseColor("#589A8D"));
        tvStep1Title.setTypeface(null, android.graphics.Typeface.BOLD);

        // --- BƯỚC 2 (Đã hoàn thành) ---
        tvStep2Circle.setBackgroundResource(R.drawable.step_circle_selected);
        tvStep2Circle.setTextColor(android.graphics.Color.WHITE);
        tvStep2Title.setTextColor(android.graphics.Color.parseColor("#589A8D"));
        tvStep2Title.setTypeface(null, android.graphics.Typeface.BOLD);

        // Bước 3: HIGHLIGHT
        tvStep3Circle.setBackgroundResource(R.drawable.step_circle_selected);
        tvStep3Circle.setTextColor(Color.WHITE);
        tvStep3Title.setTextColor(Color.parseColor("#589A8D"));
        tvStep3Title.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void setupUI() {
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepTitle.setText("Chọn bài tập phù hợp");

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            if (adapter != null) {
                ArrayList<Exercise> finalSelectedList = adapter.getSelectedExercises();

                if (finalSelectedList.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất 1 bài tập", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(CustomExerciseSelectionActivity.this, ExerciseListActivity.class);
                intent.putExtra("EXTRA_EXERCISE_LIST", finalSelectedList);
                intent.putExtra("IS_FREE_WORKOUT", true);
                startActivity(intent);
            }
        });
    }

    private void fetchAndFilterExercises() {
        progressBarApi.setVisibility(View.VISIBLE);

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllExercises("*").enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                progressBarApi.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {

                    List<Exercise> filteredList = new ArrayList<>();

                    for (Exercise ex : response.body()) {
                        boolean matchMuscle = (ex.getMuscleGroupId() != null && receivedMuscleIds.contains(ex.getMuscleGroupId()));

                        boolean matchEquipment = false;
                        List<Equipment> exEquipments = ex.getEquipments();
                        if (exEquipments == null || exEquipments.isEmpty()) {
                            matchEquipment = true; // Bodyweight
                        } else {
                            for (Equipment eq : exEquipments) {
                                if (receivedEquipmentIds.contains(eq.getId())) {
                                    matchEquipment = true;
                                    break;
                                }
                            }
                        }

                        if (matchMuscle && matchEquipment) {
                            filteredList.add(ex);
                        }
                    }

                    if (filteredList.isEmpty()) {
                        Toast.makeText(CustomExerciseSelectionActivity.this, "Không có bài tập phù hợp cho lựa chọn này!", Toast.LENGTH_LONG).show();
                    } else {
                        adapter = new ExerciseSelectionAdapter(filteredList, selectedCount -> {
                            if (selectedCount > 0) {
                                btnNextStep.setText("BẮT ĐẦU (" + selectedCount + " BÀI) →");
                                if (btnNextStep.getVisibility() == View.GONE) {
                                    btnNextStep.setVisibility(View.VISIBLE);
                                    btnNextStep.animate().translationY(0f).alpha(1f).setDuration(300).start();
                                }
                            } else {
                                btnNextStep.animate().translationY(200f).alpha(0f).setDuration(300)
                                        .withEndAction(() -> btnNextStep.setVisibility(View.GONE)).start();
                            }
                        });
                        rvSelection.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
                Toast.makeText(CustomExerciseSelectionActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}