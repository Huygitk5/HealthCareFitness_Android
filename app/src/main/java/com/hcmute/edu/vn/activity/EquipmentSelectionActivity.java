package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.hcmute.edu.vn.adapter.EquipmentGridAdapter;
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
    private EquipmentGridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection);

        initViews();
        setupUI();
        fetchEquipmentsFromApi();
    }

    private void initViews() {
        rvSelection = findViewById(R.id.rvSelection);
        btnNextStep = findViewById(R.id.btnNextStep);
        progressBarApi = findViewById(R.id.progressBarApi);

        LinearProgressIndicator stepProgressBar = findViewById(R.id.stepProgressBar);
        stepProgressBar.setProgress(33); 
    }

    private void setupUI() {
        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepCount.setText("BƯỚC 1/3");
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
        
        // Thêm Log để debug
        Log.d("API_DEBUG", "Bắt đầu gọi getAllEquipments...");

        apiService.getAllEquipments("*").enqueue(new Callback<List<Equipment>>() {
            @Override
            public void onResponse(Call<List<Equipment>> call, Response<List<Equipment>> response) {
                progressBarApi.setVisibility(View.GONE);
                
                if (response.isSuccessful()) {
                    List<Equipment> equipmentList = response.body();
                    Log.d("API_DEBUG", "Thành công! Số lượng: " + (equipmentList != null ? equipmentList.size() : 0));

                    if (equipmentList != null && !equipmentList.isEmpty()) {
                        adapter = new EquipmentGridAdapter(equipmentList, selectedCount -> {
                            if (selectedCount > 0) {
                                btnNextStep.setEnabled(true);
                                btnNextStep.setAlpha(1.0f);
                            } else {
                                btnNextStep.setEnabled(false);
                                btnNextStep.setAlpha(0.5f);
                            }
                        });
                        rvSelection.setAdapter(adapter);
                    } else {
                        Toast.makeText(EquipmentSelectionActivity.this, "Không có dữ liệu dụng cụ trên Server", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Log mã lỗi và nội dung lỗi từ Supabase
                    String errorMsg = "Lỗi: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                    
                    Log.e("API_DEBUG", errorMsg);
                    Toast.makeText(EquipmentSelectionActivity.this, "Lỗi server: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Equipment>> call, Throwable t) {
                progressBarApi.setVisibility(View.GONE);
                Log.e("API_DEBUG", "Lỗi kết nối: " + t.getMessage());
                Toast.makeText(EquipmentSelectionActivity.this, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}