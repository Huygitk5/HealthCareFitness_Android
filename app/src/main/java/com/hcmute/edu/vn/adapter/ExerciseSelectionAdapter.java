package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        Exercise exercise = exercises.get(position);
        holder.tvExerciseName.setText(exercise.getName());
        
        String info = (exercise.getBaseRecommendedReps() != null ? exercise.getBaseRecommendedReps() : "00:30") + " • Cường độ cao";
        holder.tvExerciseInfo.setText(info);

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
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                
                if (isSelected) {
                    selectedExercises.remove(exercise);
                } else {
                    selectedExercises.add(exercise);
                }
                notifyItemChanged(position);
                listener.onSelectionChanged(selectedExercises.size());
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