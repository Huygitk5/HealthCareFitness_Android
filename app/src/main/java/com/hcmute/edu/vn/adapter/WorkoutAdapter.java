package com.hcmute.edu.vn.adapter;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.ExerciseListActivity;
import com.hcmute.edu.vn.model.WorkoutDay;

import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {
    private List<WorkoutDay> mList;
    private String planId;

    public WorkoutAdapter(List<WorkoutDay> list, String planId) {
        this.mList = list;
        this.planId = planId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutDay item = mList.get(position);
        if (item == null) return;

        int dayOrder = item.getDayOrder() != null ? item.getDayOrder() : (position + 1);
        String displayTitle = "Ngày " + dayOrder;
        holder.tvTitle.setText(displayTitle);

        if (position == 0) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#B5D3C9"));
            holder.tvTitle.setTextColor(Color.parseColor("#3E665D"));
        } else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#D1E4DE"));
            holder.tvTitle.setTextColor(Color.parseColor("#4A7A6F"));
        }

        String originalName = item.getName() != null ? item.getName() : "";
        boolean isRestDay = originalName.toLowerCase().contains("nghỉ") || item.getExercises() == null || item.getExercises().isEmpty();

        if (isRestDay) {
            holder.tvSub.setText("Nghỉ ngơi");
            holder.ivRest.setVisibility(View.VISIBLE);
            holder.btnStart.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), com.hcmute.edu.vn.activity.RestDayCompleteActivity.class);
                intent.putExtra("EXTRA_DAY_ID", item.getId());
                intent.putExtra("EXTRA_PLAN_ID", planId);
                v.getContext().startActivity(intent);
            });
        } else {
            holder.tvSub.setText("Danh sách bài tập");
            holder.ivRest.setVisibility(View.GONE);

            View.OnClickListener clickListener = v -> {
                Intent intent = new Intent(v.getContext(), ExerciseListActivity.class);
                intent.putExtra("EXTRA_DAY_ID", item.getId());
                intent.putExtra("EXTRA_PLAN_ID", planId);
                intent.putExtra("EXTRA_DAY_TITLE", displayTitle);
                v.getContext().startActivity(intent);
            };

            holder.itemView.setOnClickListener(clickListener);

            if (position == 0) {
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnStart.setOnClickListener(clickListener);
            } else {
                holder.btnStart.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub, btnStart;
        ImageView ivRest;
        CardView cardView;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvDayTitle);
            tvSub = itemView.findViewById(R.id.tvSubTitle);
            btnStart = itemView.findViewById(R.id.btnStart);
            ivRest = itemView.findViewById(R.id.ivRestIcon);
            cardView = itemView.findViewById(R.id.itemCardView);
        }
    }
}