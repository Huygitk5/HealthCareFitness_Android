package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionAdapter extends RecyclerView.Adapter<ExerciseSelectionAdapter.ViewHolder> {

    private List<Exercise> exercises;
    private List<Exercise> selectedExercises = new ArrayList<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public ExerciseSelectionAdapter(List<Exercise> exercises, OnSelectionChangedListener listener) {
        this.exercises = exercises;
        this.listener = listener;
    }

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
        // 1. QUAN TRỌNG: Đặt lại scale về 100% mỗi khi vẽ View để không bị lỗi tái sử dụng
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        Exercise exercise = exercises.get(position);
        holder.tvExerciseName.setText(exercise.getName());

        String info = (exercise.getBaseRecommendedReps() != null ? exercise.getBaseRecommendedReps() : "00:30") + " • Cường độ cao";
        holder.tvExerciseInfo.setText(info);

        // Cập nhật load ảnh bằng Glide
        String imageUrl = exercise.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            if (imageUrl.startsWith("http")) {
                // Nếu là đường link từ mạng (Supabase)
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.workout_1) // Ảnh chờ
                        .error(R.drawable.workout_1)       // Ảnh nếu lỗi
                        .into(holder.ivExerciseImage);
            } else {
                // Nếu là dữ liệu mẫu cũ
                try {
                    int imageRes = Integer.parseInt(imageUrl);
                    holder.ivExerciseImage.setImageResource(imageRes);
                } catch (Exception e) {
                    holder.ivExerciseImage.setImageResource(R.drawable.workout_1);
                }
            }
        } else {
            // Nếu Supabase không có ảnh
            holder.ivExerciseImage.setImageResource(R.drawable.workout_1);
        }

        boolean isSelected = selectedExercises.contains(exercise);

        // LOGIC HIỂN THỊ TRẠNG THÁI CHỌN
        if (isSelected) {
            holder.cardContainer.setStrokeWidth(4);
            holder.cardContainer.setStrokeColor(Color.parseColor("#589A8D"));
            holder.cardContainer.setCardBackgroundColor(Color.parseColor("#F1F8F7"));
            holder.tvExerciseName.setTextColor(Color.parseColor("#589A8D"));
        } else {
            holder.cardContainer.setStrokeWidth(0);
            holder.cardContainer.setCardBackgroundColor(Color.WHITE);
            holder.tvExerciseName.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            // Thu nhỏ xuống 96%
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction(() -> {

                // Nảy trở lại 100%
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(() -> {

                    // 2. Đợi nảy lại hoàn toàn (EndAction) rồi mới đổi trạng thái và Notify
                    // Dùng selectedExercises.contains() thay vì isSelected để tránh lỗi khi click quá nhanh
                    if (selectedExercises.contains(exercise)) {
                        selectedExercises.remove(exercise);
                    } else {
                        selectedExercises.add(exercise);
                    }
                    notifyItemChanged(position);
                    listener.onSelectionChanged(selectedExercises.size());

                }).start();

            }).start();
        });
    }

    @Override
    public int getItemCount() { return exercises.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardContainer;
        ImageView ivExerciseImage;
        TextView tvExerciseName, tvExerciseInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = (MaterialCardView) itemView.findViewById(R.id.layoutContainerCard);
            ivExerciseImage = itemView.findViewById(R.id.ivExerciseImage);
            tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
            tvExerciseInfo = itemView.findViewById(R.id.tvExerciseInfo);
        }
    }
}