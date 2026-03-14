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
import com.hcmute.edu.vn.adapter.MuscleSelectionAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Equipment;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.MuscleGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MuscleSelectionActivity extends AppCompatActivity {
    private Button btnNextStep;
    private RecyclerView rvSelection;
    private ProgressBar progressBarApi;

    private ArrayList<Integer> receivedEquipmentIds;
    private MuscleGroup selectedMuscle = null;

    private List<MuscleGroup> allMuscleGroups = new ArrayList<>();
    private Set<Integer> validMuscleIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        receivedEquipmentIds = getIntent().getIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS");
        if (receivedEquipmentIds == null) receivedEquipmentIds = new ArrayList<>();

        initViews();
        setupUI();
        fetchDataFromApi();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(66);
    }

    private void setupUI() {
        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepCount.setText("BƯỚC 2/3");
        tvStepTitle.setText("Hôm nay bạn muốn tập nhóm cơ nào?");

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            if (selectedMuscle != null) {
                Intent intent = new Intent(MuscleSelectionActivity.this, CustomExerciseSelectionActivity.class);
                intent.putIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS", receivedEquipmentIds);
                intent.putExtra("SELECTED_MUSCLE_ID", selectedMuscle.getId());
                intent.putExtra("SELECTED_MUSCLE_NAME", selectedMuscle.getName());
                startActivity(intent);
            }
        });
    }

    private void fetchDataFromApi() {
        progressBarApi.setVisibility(View.VISIBLE);

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getMuscleGroups(new HashMap<>()).enqueue(new Callback<List<MuscleGroup>>() {
            @Override
            public void onResponse(Call<List<MuscleGroup>> call, Response<List<MuscleGroup>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allMuscleGroups = response.body();
                    fetchExercisesToFilterMuscles();
                } else {
                    progressBarApi.setVisibility(View.GONE);
                    Toast.makeText(MuscleSelectionActivity.this, "Lỗi lấy nhóm cơ", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<MuscleGroup>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
            }
        });
    }

    private void fetchExercisesToFilterMuscles() {
        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getAllExercises("*").enqueue(new Callback<List<Exercise>>() {
            @Override
            public void onResponse(Call<List<Exercise>> call, Response<List<Exercise>> response) {
                progressBarApi.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {

                    for (Exercise ex : response.body()) {
                        boolean canDo = false;
                        List<Equipment> exEquipments = ex.getEquipments();

                        if (exEquipments == null || exEquipments.isEmpty()) {
                            canDo = true;
                        } else {
                            for (Equipment eq : exEquipments) {
                                if (receivedEquipmentIds.contains(eq.getId())) {
                                    canDo = true;
                                    break;
                                }
                            }
                        }

                        if (canDo && ex.getMuscleGroupId() != null) {
                            validMuscleIds.add(ex.getMuscleGroupId());
                        }
                    }

                    MuscleSelectionAdapter adapter = new MuscleSelectionAdapter(allMuscleGroups, validMuscleIds, muscle -> {
                        selectedMuscle = muscle;
                        btnNextStep.setEnabled(true);
                        btnNextStep.setAlpha(1.0f);
                    });
                    rvSelection.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
            }
        });
    }
}