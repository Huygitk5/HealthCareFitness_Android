package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.MuscleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MuscleSelectionAdapter extends RecyclerView.Adapter<MuscleSelectionAdapter.ViewHolder> {

    private List<MuscleGroup> muscleList;
    private Set<Integer> validMuscleIds; 
    private List<Integer> selectedMuscleIds = new ArrayList<>();
    private OnMuscleSelectionListener listener;

    public interface OnMuscleSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public MuscleSelectionAdapter(List<MuscleGroup> muscleList, Set<Integer> validMuscleIds, OnMuscleSelectionListener listener) {
        this.muscleList = muscleList;
        this.validMuscleIds = validMuscleIds;
        this.listener = listener;
    }

    public List<Integer> getSelectedMuscleIds() {
        return selectedMuscleIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_muscle_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MuscleGroup muscle = muscleList.get(position);
        holder.tvMuscleName.setText(muscle.getName());

        boolean isValid = validMuscleIds.contains(muscle.getId());
        boolean isSelected = selectedMuscleIds.contains(muscle.getId());

        if (!isValid) {
            holder.itemView.setAlpha(0.4f);
            holder.cardMuscle.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.cardMuscle.setStrokeWidth(0);
            holder.itemView.setClickable(false);
        } else {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setClickable(true);

            if (isSelected) {
                holder.cardMuscle.setCardBackgroundColor(Color.parseColor("#EAF4F3"));
                holder.cardMuscle.setStrokeColor(Color.parseColor("#589A8D"));
                holder.cardMuscle.setStrokeWidth(5);
                holder.tvMuscleName.setTextColor(Color.parseColor("#589A8D"));
                holder.layoutCheck.setVisibility(View.VISIBLE);
            } else {
                holder.cardMuscle.setCardBackgroundColor(Color.WHITE);
                holder.cardMuscle.setStrokeColor(Color.parseColor("#E0E0E0"));
                holder.cardMuscle.setStrokeWidth(2);
                holder.tvMuscleName.setTextColor(Color.parseColor("#333333"));
                holder.layoutCheck.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (!isValid) return;

            v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                
                if (isSelected) {
                    selectedMuscleIds.remove(Integer.valueOf(muscle.getId()));
                } else {
                    selectedMuscleIds.add(muscle.getId());
                }
                notifyItemChanged(position);
                listener.onSelectionChanged(selectedMuscleIds.size());
            }).start();
        });
    }

    @Override
    public int getItemCount() { return muscleList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMuscle;
        TextView tvMuscleName;
        FrameLayout layoutCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMuscle = itemView.findViewById(R.id.cardMuscle);
            tvMuscleName = itemView.findViewById(R.id.tvMuscleName);
            layoutCheck = itemView.findViewById(R.id.layoutCheck);
        }
    }
}