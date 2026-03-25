package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.EquipmentSelectionAdapter;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Equipment;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EquipmentSelectionActivity extends AppCompatActivity {
    private Button btnNextStep;
    private RecyclerView rvSelection;
    private ProgressBar progressBarApi;
    private EquipmentSelectionAdapter adapter;
    private TextView tvStep1Circle, tvStep1Title;
    private TextView tvStep2Circle, tvStep2Title;
    private TextView tvStep3Circle, tvStep3Title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        initViews();
        setupStepIndicator();
        setupUI();
        fetchEquipmentsFromApi();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(33);
        tvStep1Circle = findViewById(R.id.tvStep1Circle);
        tvStep1Title = findViewById(R.id.tvStep1Title);
        tvStep2Circle = findViewById(R.id.tvStep2Circle);
        tvStep2Title = findViewById(R.id.tvStep2Title);
        tvStep3Circle = findViewById(R.id.tvStep3Circle);
        tvStep3Title = findViewById(R.id.tvStep3Title);
    }

    private void setupStepIndicator() {
        // SET TRẠNG THÁI CHO BƯỚC 1 (Highlight)
        tvStep1Circle.setBackgroundResource(R.drawable.step_circle_selected);
        tvStep1Circle.setTextColor(android.graphics.Color.WHITE);
        tvStep1Title.setTextColor(android.graphics.Color.parseColor("#589A8D"));
        tvStep1Title.setTypeface(null, android.graphics.Typeface.BOLD);

        // ĐẢM BẢO CÁC BƯỚC CÒN LẠI LÀ CHƯA CHỌN
        tvStep2Circle.setBackgroundResource(R.drawable.step_circle_unselected);
        tvStep3Circle.setBackgroundResource(R.drawable.step_circle_unselected);
    }

    private void setupUI() {
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepTitle.setText("Dụng cụ bạn đang có?");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        rvSelection.setLayoutManager(new GridLayoutManager(this, 2));

        btnNextStep.setOnClickListener(v -> {
            if (adapter != null) {
                ArrayList<Integer> selectedIds = adapter.getSelectedEquipmentIds();
                Intent intent = new Intent(EquipmentSelectionActivity.this, MuscleSelectionActivity.class);
                intent.putIntegerArrayListExtra("SELECTED_EQUIPMENT_IDS", selectedIds);
                startActivity(intent);
            }
        });
    }

    private void fetchEquipmentsFromApi() {
        progressBarApi.setVisibility(View.VISIBLE);

        SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);

        apiService.getAllEquipments("*").enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                progressBarApi.setVisibility(View.GONE);

                if (response.isSuccessful()) {
                    List<Equipment> equipmentList = response.body();

                    if (equipmentList != null && !equipmentList.isEmpty()) {
                        // Logic Animation trượt nút Tiếp tục
                        adapter = new EquipmentSelectionAdapter(equipmentList, selectedCount -> {
                            if (selectedCount > 0) {
                                btnNextStep.setText("TIẾP TỤC (" + selectedCount + ") →");

                                // Nếu nút đang ẩn thì cho trượt lên
                                if (btnNextStep.getVisibility() == View.GONE) {
                                    btnNextStep.setVisibility(View.VISIBLE);
                                    btnNextStep.setTranslationY(200f);
                                    btnNextStep.animate().translationY(0f).alpha(1f).setDuration(300).start();
                                }
                            } else {
                                // Ẩn đi khi bỏ chọn hết
                                btnNextStep.animate().translationY(200f).alpha(0f).setDuration(300)
                                        .withEndAction(() -> btnNextStep.setVisibility(View.GONE)).start();
                            }
                        });
                        rvSelection.setAdapter(adapter);
                    } else {
                        Toast.makeText(EquipmentSelectionActivity.this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(EquipmentSelectionActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Equipment>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
                Toast.makeText(EquipmentSelectionActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}