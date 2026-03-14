package com.hcmute.edu.vn.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.ExerciseSelectionAdapter;
import com.hcmute.edu.vn.model.Exercise;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomExerciseSelectionActivity extends AppCompatActivity {

    private String receivedEquipment, receivedMuscle;
    private Button btnNextStep;
    private ExerciseSelectionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Tái sử dụng cái Layout Bước 1/3 (Cực đỉnh!)
        setContentView(R.layout.activity_step_selection);

        // Hứng Data từ màn 2
        receivedEquipment = getIntent().getStringExtra("FILTER_EQUIPMENT");
        receivedMuscle = getIntent().getStringExtra("FILTER_MUSCLE");

        TextView tvStepCount = findViewById(R.id.tvStepCount);
        TextView tvStepTitle = findViewById(R.id.tvStepTitle);
        btnNextStep = findViewById(R.id.btnNextStep);
        RecyclerView rvSelection = findViewById(R.id.rvSelection);

        // Đổi Header
        tvStepCount.setText("Bước 3/3");
        tvStepTitle.setText("Chọn bài tập " + receivedMuscle + " với " + receivedEquipment);
        btnNextStep.setText("BẮT ĐẦU TẬP"); // Đổi text nút dưới cùng

        // TODO: Đoạn này sau này em gọi SQL JOIN 3 bảng để lấy danh sách bài tập.
        // Tạm thời anh Mock Data nhé:
        List<Exercise> filteredExercises = getMockExercises(receivedMuscle);

        adapter = new ExerciseSelectionAdapter(filteredExercises, selectedCount -> {
            // Logic UI xịn: Phải chọn ít nhất 1 bài thì mới cho tập
            if (selectedCount > 0) {
                btnNextStep.setEnabled(true);
                btnNextStep.setAlpha(1.0f);
                btnNextStep.setText("BẮT ĐẦU TẬP (" + selectedCount + " BÀI)");
            } else {
                btnNextStep.setEnabled(false);
                btnNextStep.setAlpha(0.5f);
                btnNextStep.setText("CHỌN BÀI TẬP");
            }
        });

        rvSelection.setLayoutManager(new LinearLayoutManager(this));
        rvSelection.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Bấm BẮT ĐẦU -> Ném list các bài đã tick sang màn hình đếm giờ
        btnNextStep.setOnClickListener(v -> {
            ArrayList<Exercise> finalSelectedList = adapter.getSelectedExercises();

            if (finalSelectedList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn bài tập", Toast.LENGTH_SHORT).show();
                return;
            }

            // Bay thẳng sang màn hình Tập
            Intent intent = new Intent(CustomExerciseSelectionActivity.this, ExerciseActivity.class);
            intent.putExtra("EXTRA_EXERCISE_LIST", finalSelectedList);
            // Gắn cờ tập tự do để lưu Null Plan_ID như em yêu cầu
            intent.putExtra("IS_FREE_WORKOUT", true);
            startActivity(intent);
        });
    }

    // Hàm tạo Data mẫu test UI
    private List<Exercise> getMockExercises(String muscle) {
        List<Exercise> list = new ArrayList<>();
        list.add(new Exercise(UUID.randomUUID().toString(), "Dumbbell Bench Press", "Đẩy ngực", 1, 1, 3, "x12", "", String.valueOf(R.drawable.workout_1), null));
        list.add(new Exercise(UUID.randomUUID().toString(), "Dumbbell Flyes", "Ép ngực", 1, 1, 3, "x15", "", String.valueOf(R.drawable.workout_2), null));
        list.add(new Exercise(UUID.randomUUID().toString(), "Push Ups", "Hít đất", 1, 1, 3, "x20", "", String.valueOf(R.drawable.workout_3), null));
        return list;
    }
}