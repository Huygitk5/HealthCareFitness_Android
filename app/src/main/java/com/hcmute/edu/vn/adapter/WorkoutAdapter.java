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
    public WorkoutAdapter(List<WorkoutDay> list) { this.mList = list; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_day, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutDay item = mList.get(position);
        holder.tvTitle.setText(item.getDayName());
        holder.tvSub.setText(item.getSubTitle());

        if (position == 0) { // Ngày 1
            holder.cardView.setCardBackgroundColor(Color.parseColor("#B5D3C9"));
            holder.btnStart.setVisibility(View.VISIBLE);
            holder.ivRest.setVisibility(View.GONE);

            // XỬ LÝ CLICK ĐẾN ĐÚNG ACTIVITY DANH SÁCH BÀI TẬP
            holder.btnStart.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ExerciseListActivity.class);
                v.getContext().startActivity(intent);
            });

            holder.tvTitle.setTextColor(Color.parseColor("#3E665D"));
        } else { // Các ngày khác
            holder.cardView.setCardBackgroundColor(Color.parseColor("#D1E4DE"));
            holder.btnStart.setVisibility(View.GONE);
            holder.ivRest.setVisibility(item.isRestDay() ? View.VISIBLE : View.GONE);
            holder.tvTitle.setTextColor(Color.parseColor("#4A7A6F"));
            holder.btnStart.setOnClickListener(null); // Tránh lỗi click nhầm
        }
    }

    @Override
    public int getItemCount() { return mList.size(); }

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