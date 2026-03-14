package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;
import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionAdapter extends RecyclerView.Adapter<ExerciseSelectionAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private List<Exercise> selectedExercises = new ArrayList<>(); // Danh sách chứa các bài đã chọn
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public ExerciseSelectionAdapter(List<Exercise> exercises, OnSelectionChangedListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

    // Hàm public để Activity lấy danh sách các bài đã chọn ra ngoài
    public ArrayList<Exercise> getSelectedExercises() {
        return (ArrayList<Exercise>) selectedExercises;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.tvExerciseName.setText(exercise.getName());
        
        // Thay đổi getter cho đúng với model Exercise
        String info = "Nhóm cơ: " + exercise.getMuscleGroupId() + " • " + exercise.getBaseRecommendedReps();
        holder.tvExerciseInfo.setText(info);

        // Hiển thị ảnh (Sử dụng getImageUrl)
        try {
            String imageUrl = exercise.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                int imageRes = Integer.parseInt(imageUrl);
                holder.ivExerciseImage.setImageResource(imageRes);
            } else {
                holder.ivExerciseImage.setImageResource(R.drawable.workout_1);
            }
        } catch (Exception e) {
            holder.ivExerciseImage.setImageResource(R.drawable.workout_1);
        }

        // Logic thay đổi UI dựa vào việc bài này CÓ NẰM TRONG list selectedExercises không
        boolean isSelected = selectedExercises.contains(exercise);
        if (isSelected) {
            holder.layoutContainer.setBackgroundResource(R.drawable.bg_workout_chip_active); // Dùng lại viền xanh
            holder.ivCheck.setImageResource(android.R.drawable.checkbox_on_background); // Icon Tích Xanh
            holder.ivCheck.setColorFilter(Color.parseColor("#009688"));
            holder.tvExerciseName.setTextColor(Color.WHITE);
            holder.tvExerciseInfo.setTextColor(Color.WHITE);
        } else {
            holder.layoutContainer.setBackgroundResource(R.drawable.bg_input_rounded);
            holder.ivCheck.setImageResource(android.R.drawable.ic_input_add); // Icon Dấu Cộng
            holder.ivCheck.setColorFilter(Color.parseColor("#BDBDBD"));
            holder.tvExerciseName.setTextColor(Color.BLACK);
            holder.tvExerciseInfo.setTextColor(Color.parseColor("#757575"));
        }

        // Xử lý sự kiện bấm
        holder.itemView.setOnClickListener(v -> {
            if (isSelected) {
                selectedExercises.remove(exercise); // Bấm lần 2 -> Bỏ chọn
            } else {
                selectedExercises.add(exercise); // Bấm lần 1 -> Chọn
            }
            notifyItemChanged(position); // Chỉ cập nhật đúng item vừa bấm cho mượt
            listener.onSelectionChanged(selectedExercises.size()); // Báo về Activity số lượng đã chọn
        });
    }

    @Override
    public int getItemCount() { return exercises.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View layoutContainer;
        ImageView ivExerciseImage, ivCheck;
        TextView tvExerciseName, tvExerciseInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutContainer = itemView.findViewById(R.id.layoutContainer);
            ivExerciseImage = itemView.findViewById(R.id.ivExerciseImage);
            ivCheck = itemView.findViewById(R.id.ivCheck);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvExerciseInfo = itemView.findViewById(R.id.tvExerciseInfo);
        }
    }
}