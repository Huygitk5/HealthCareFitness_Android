package com.hcmute.edu.vn.activity;

import android.content.Intent;
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
    private int receivedMuscleId;
    private String receivedMuscleName;

    private Button btnNextStep;
    private ProgressBar progressBarApi;
    private RecyclerView rvSelection;
    private ExerciseSelectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        receivedEquipmentIds = getIntent().getIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS");
        receivedMuscleId = getIntent().getIntExtra("SELECTED_MUSCLE_ID", -1);
        receivedMuscleName = getIntent().getStringExtra("SELECTED_MUSCLE_NAME");

        if (receivedEquipmentIds == null) receivedEquipmentIds = new ArrayList<>();

        initViews();
        setupUI();
        fetchAndFilterExercises();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(100);
    }

    private void setupUI() {
        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);

        tvStepCount.setText("BƯỚC 3/3");
        tvStepTitle.setText("Chọn bài tập " + (receivedMuscleName != null ? receivedMuscleName : ""));
        btnNextStep.setText("BẮT ĐẦU TẬP");

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            if (adapter != null) {
                ArrayList<Exercise> finalSelectedList = adapter.getSelectedExercises();

                if (finalSelectedList.isEmpty()) {
                    Toast.makeText(this, "Vui lòng chọn ít nhất 1 bài tập", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(CustomExerciseSelectionActivity.this, ExerciseActivity.class);
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
                        // Khớp nhóm cơ
                        boolean matchMuscle = (ex.getMuscleGroupId() != null && ex.getMuscleGroupId() == receivedMuscleId);
                        
                        // Khớp dụng cụ (Bodyweight hoặc có dụng cụ trong list đã chọn)
                        boolean matchEquipment = false;
                        List<Equipment> exEquipments = ex.getEquipments();
                        if (exEquipments == null || exEquipments.isEmpty()) {
                            matchEquipment = true;
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
                        Toast.makeText(CustomExerciseSelectionActivity.this, "Rất tiếc không có bài tập nào phù hợp!", Toast.LENGTH_LONG).show();
                    } else {
                        adapter = new ExerciseSelectionAdapter(filteredList, selectedCount -> {
                            if (selectedCount > 0) {
                                btnNextStep.setEnabled(true);
                                btnNextStep.setAlpha(1.0f);
                                btnNextStep.setText("BẮT ĐẦU TẬP (" + selectedCount + " BÀI)");
                            } else {
                                btnNextStep.setEnabled(false);
                                btnNextStep.setAlpha(0.5f);
                                btnNextStep.setText("BẮT ĐẦU TẬP");
                            }
                        });
                        rvSelection.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
                Toast.makeText(CustomExerciseSelectionActivity.this, "Lỗi lấy bài tập", Toast.LENGTH_SHORT).show();
            }
        });
    }
}