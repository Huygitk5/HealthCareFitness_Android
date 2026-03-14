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

public class MuscleSelectionActivity extends AppCompatActivity {
    private String selectedMuscle = null;
    private String receivedEquipment;
    private Button btnNextStep;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_selection); // Vẫn dùng Layout cũ cực kỳ sạch code

        receivedEquipment = getIntent().getStringExtra("FILTER_EQUIPMENT");

        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        btnNextStep = findViewById(R.id.btnNextStep);
        RecyclerView rvSelection = findViewById(R.id.rvSelection);

        // Đổ Data cho màn hình Nhóm Cơ
        tvStepCount.setText("Bước 2/3");
        tvStepTitle.setText("Hôm nay bạn muốn tập nhóm cơ nào?");

        List<SelectionItem> muscles = new ArrayList<>();
        muscles.add(new SelectionItem("Ngực (Chest)"));
        muscles.add(new SelectionItem("Lưng (Back)"));
        muscles.add(new SelectionItem("Chân & Mông (Legs & Glutes)"));
        muscles.add(new SelectionItem("Vai & Tay (Shoulders & Arms)"));
        muscles.add(new SelectionItem("Cơ Bụng (Core/Abs)"));

        SelectionAdapter adapter = new SelectionAdapter(muscles, selectedName -> {
            selectedMuscle = selectedName;
            btnNextStep.setEnabled(true);
            btnNextStep.setAlpha(1.0f);
        });

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        rvSelection.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnNextStep.setOnClickListener(v -> {
             Intent intent = new Intent(MuscleSelectionActivity.this, CustomExerciseSelectionActivity.class);
             intent.putExtra("FILTER_EQUIPMENT", receivedEquipment);
             intent.putExtra("FILTER_MUSCLE", selectedMuscle);
             startActivity(intent);
        });
    }
}