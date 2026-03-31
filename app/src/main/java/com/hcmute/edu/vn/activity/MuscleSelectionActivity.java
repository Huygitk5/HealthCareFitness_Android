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
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MuscleSelectionActivity extends AppCompatActivity {
    private Button btnNextStep;
    private RecyclerView rvSelection;
    private ProgressBar progressBarApi;

    private ArrayList<Integer> receivedEquipmentIds;
    private MuscleSelectionAdapter adapter;

    private List<MuscleGroup> allMuscleGroups = new ArrayList<>();
    private Set<Integer> validMuscleIds = new HashSet<>();
    private TextView tvStep1Circle, tvStep1Title;
    private TextView tvStep2Circle, tvStep2Title;
    private TextView tvStep3Circle, tvStep3Title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        Intent intent = getIntent();
        if (intent != null) {
            receivedEquipmentIds = intent.getIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS");
        }
        
        if (receivedEquipmentIds == null) {
            receivedEquipmentIds = new ArrayList<>();
        }

        initViews();
        setupStepIndicator();
        setupUI();
        fetchDataFromApi();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(66);

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
        tvStep1Circle.setBackgroundResource(R.drawable.step_circle_selected);

        // Bước 2: HIGHLIGHT
        tvStep2Circle.setBackgroundResource(R.drawable.step_circle_selected);
        tvStep2Circle.setTextColor(android.graphics.Color.WHITE);
        tvStep2Title.setTextColor(android.graphics.Color.parseColor("#589A8D"));
        tvStep2Title.setTypeface(null, android.graphics.Typeface.BOLD);

        // Bước 3: Chưa tới
        tvStep3Circle.setBackgroundResource(R.drawable.step_circle_unselected);
    }

    private void setupUI() {
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepTitle.setText("Hôm nay bạn muốn tập nhóm cơ nào?");

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            if (adapter != null && !adapter.getSelectedMuscleIds().isEmpty()) {
                Intent nextIntent = new Intent(MuscleSelectionActivity.this, CustomExerciseSelectionActivity.class);
                nextIntent.putIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS", receivedEquipmentIds);
                
                ArrayList<Integer> selectedIds = new ArrayList<>(adapter.getSelectedMuscleIds());
                nextIntent.putIntegerArrayListExtra("SELECTED_MUSCLE_IDS", selectedIds);
                
                startActivity(nextIntent);
            }
        });
    }

    private void fetchDataFromApi() {
        progressBarApi.setVisibility(View.VISIBLE);

        Map<String, String> filters = new HashMap<>();
        filters.put("select", "*");

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
        apiService.getMuscleGroups(filters).enqueue(new Callback<List<MuscleGroup>>() {
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

                    adapter = new MuscleSelectionAdapter(allMuscleGroups, validMuscleIds, selectedCount -> {
                        if (selectedCount > 0) {
                            btnNextStep.setText("TIẾP TỤC (" + selectedCount + ") →");

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

            @Override
            public void onFailure(Call<List<Exercise>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
            }
        });
    }
}