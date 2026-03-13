package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.edu.vn.R;

public class FreeWorkoutFilterActivity extends AppCompatActivity {

    private Spinner spinnerMuscleGroup, spinnerEquipment;
    private Button btnFindAndStart;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_free_workout_filter);
        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        // 1. Ánh xạ View
        spinnerMuscleGroup = findViewById(R.id.spinnerMuscleGroup);
        spinnerEquipment = findViewById(R.id.spinnerEquipment);
        btnFindAndStart = findViewById(R.id.btnFindAndStart);
        btnBack = findViewById(R.id.btnBack);

        // 2. Setup dữ liệu cho Spinner
        setupSpinners();

        // 3. Xử lý sự kiện
        btnBack.setOnClickListener(v -> finish()); // Đóng màn hình khi bấm Back

        btnFindAndStart.setOnClickListener(v -> {
            // Lấy giá trị Tuấn vừa chọn
            String selectedMuscle = spinnerMuscleGroup.getSelectedItem().toString();
            String selectedEquipment = spinnerEquipment.getSelectedItem().toString();

            // Đẩy dữ liệu sang màn hình Danh sách bài tập
            Intent intent = new Intent(FreeWorkoutFilterActivity.this, ExerciseListActivity.class);
            intent.putExtra("FILTER_MUSCLE", selectedMuscle);
            intent.putExtra("FILTER_EQUIPMENT", selectedEquipment);

            // Cờ cực kỳ quan trọng cho Kịch bản 2 (Tập tự do -> Không có plan_id)
            intent.putExtra("IS_FREE_WORKOUT", true);

            startActivity(intent);
        });
    }

    private void setupSpinners() {
        // Dữ liệu mẫu (Mock data).
        // Trong tương lai nếu dùng Clean Architecture, em sẽ gọi ViewModel để fetch List này từ Database lên.
        String[] muscles = {"Ngực (Chest)", "Lưng (Back)", "Chân (Legs)", "Vai (Shoulders)", "Tay (Arms)"};
        String[] equipments = {"Dumbbell & Bench", "Barbell", "Máy tập (Machine)", "Cáp (Cable)", "Trọng lượng cơ thể (Bodyweight)"};

        // Adapter cho Nhóm cơ
        ArrayAdapter<String> muscleAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, muscles);
        spinnerMuscleGroup.setAdapter(muscleAdapter);

        // Adapter cho Dụng cụ
        ArrayAdapter<String> equipmentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, equipments);
        spinnerEquipment.setAdapter(equipmentAdapter);
    }
}