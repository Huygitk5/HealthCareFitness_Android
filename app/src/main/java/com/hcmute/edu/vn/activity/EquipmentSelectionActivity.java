package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.SelectionAdapter;
import com.hcmute.edu.vn.model.SelectionItem;
import java.util.ArrayList;
import java.util.List;

public class EquipmentSelectionActivity extends AppCompatActivity {
    private String selectedEquipment = null;
    private Button btnNextStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tái sử dụng Layout chung
        setContentView(R.layout.activity_step_selection);

        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        btnNextStep = findViewById(R.id.btnNextStep);
        RecyclerView rvSelection = findViewById(R.id.rvSelection);

        // Đổ Data cho màn hình Dụng Cụ
        tvStepCount.setText("Bước 1/3");
        tvStepTitle.setText("Dụng cụ bạn đang có sẵn là gì?");

        List<SelectionItem> equipments = new ArrayList<>();
        equipments.add(new SelectionItem("Chỉ Tạ Đơn (Dumbbell Only)"));
        equipments.add(new SelectionItem("Tạ Đơn & Ghế (Dumbbell & Bench)"));
        equipments.add(new SelectionItem("Đầy Đủ Phòng Gym (Full Gym)"));
        equipments.add(new SelectionItem("Không Dụng Cụ (Bodyweight)"));

        SelectionAdapter adapter = new SelectionAdapter(equipments, selectedName -> {
            selectedEquipment = selectedName;
            btnNextStep.setEnabled(true); // Chọn xong mới cho bấm Next
            btnNextStep.setAlpha(1.0f);
        });

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        rvSelection.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
            Intent intent = new Intent(EquipmentSelectionActivity.this, MuscleSelectionActivity.class);
            intent.putExtra("FILTER_EQUIPMENT", selectedEquipment); // Truyền dữ liệu đi
            startActivity(intent);
        });
    }
}