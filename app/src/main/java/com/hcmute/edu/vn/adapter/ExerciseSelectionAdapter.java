package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class ExerciseSelectionAdapter extends RecyclerView.Adapter<ExerciseSelectionAdapter.ViewHolder> {
    private List<Exercise> exerciseList;
    private java.util.Set<Integer> selectedPositions = new java.util.HashSet<>();
    private OnSelectionChangedListener listener;

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }

    public ExerciseSelectionAdapter(List<Exercise> exerciseList, OnSelectionChangedListener listener) {
        this.exerciseList = exerciseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exerciseList.get(position);
        holder.tvName.setText(exercise.getName());
        
        boolean isSelected = selectedPositions.contains(position);
        holder.cbSelect.setChecked(isSelected);
        
        // Highlight background if selected
        holder.itemView.setAlpha(isSelected ? 1.0f : 0.9f);
        
        Glide.with(holder.itemView.getContext())
                .load(exercise.getImageUrl())
                .placeholder(R.drawable.ic_image_gray)
                .into(holder.ivThumb);

        holder.itemView.setOnClickListener(v -> toggleSelection(holder.getAdapterPosition()));
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        if (listener != null) listener.onSelectionChanged(selectedPositions.size());
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    public ArrayList<Exercise> getSelectedExercises() {
        ArrayList<Exercise> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < exerciseList.size()) {
                selected.add(exerciseList.get(pos));
            }
        }
        return selected;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName;
        android.widget.CheckBox cbSelect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.imgExercise);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            cbSelect = itemView.findViewById(R.id.cbSelect);
        }
    }
}