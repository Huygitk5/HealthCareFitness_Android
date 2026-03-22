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
import com.hcmute.edu.vn.model.WorkoutDay;

import java.util.List;
import java.util.Set;

public class JourneyDayAdapter extends RecyclerView.Adapter<JourneyDayAdapter.ViewHolder> {

    public interface OnDayClickListener {
        void onDayClick(WorkoutDay day);
    }

    private final List<WorkoutDay> days;
    private final int currentDayIndex;        // index của ngày hôm nay
    private final Set<String> completedDayIds; // dayId đã hoàn thành
    private final OnDayClickListener listener;

    public JourneyDayAdapter(List<WorkoutDay> days, int currentDayIndex,
                             Set<String> completedDayIds, OnDayClickListener listener) {
        this.days = days;
        this.currentDayIndex = currentDayIndex;
        this.completedDayIds = completedDayIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_journey_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutDay day = days.get(position);
        int dayOrder = day.getDayOrder() != null ? day.getDayOrder() : (position + 1);

        boolean isCompleted = completedDayIds.contains(day.getId());
        boolean isToday = (position == currentDayIndex);
        boolean isPast = position < currentDayIndex;
        boolean isFuture = position > currentDayIndex;

        // Số ngày
        holder.tvDayNumber.setText("Ngày " + dayOrder);

        // Tên buổi tập (rút ngắn nếu quá dài)
        String name = day.getName() != null ? day.getName() : "";
        holder.tvDayName.setText(name);

        // === Dấu tick cho ngày đã hoàn thành ===
        holder.ivTick.setVisibility(isCompleted ? View.VISIBLE : View.GONE);

        // === Màu sắc theo trạng thái ===
        if (isToday) {
            // Ngày hôm nay: nền xanh đậm, viền xanh
            holder.card.setCardBackgroundColor(Color.parseColor("#4DAA9A"));
            holder.card.setStrokeColor(Color.parseColor("#2F7F73"));
            holder.card.setStrokeWidth(4);
            holder.tvDayNumber.setTextColor(Color.WHITE);
            holder.tvDayName.setTextColor(Color.parseColor("#E0F2F1"));
            holder.tvTodayBadge.setVisibility(View.VISIBLE);
        } else if (isCompleted) {
            // Ngày đã hoàn thành: nền xanh nhạt, viền xanh
            holder.card.setCardBackgroundColor(Color.parseColor("#E8F5F3"));
            holder.card.setStrokeColor(Color.parseColor("#4DAA9A"));
            holder.card.setStrokeWidth(2);
            holder.tvDayNumber.setTextColor(Color.parseColor("#2F7F73"));
            holder.tvDayName.setTextColor(Color.parseColor("#4DAA9A"));
            holder.tvTodayBadge.setVisibility(View.GONE);
        } else if (isFuture) {
            // Ngày tương lai: nền xám nhạt, chữ mờ
            holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.card.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.card.setStrokeWidth(1);
            holder.tvDayNumber.setTextColor(Color.parseColor("#BDBDBD"));
            holder.tvDayName.setTextColor(Color.parseColor("#BDBDBD"));
            holder.tvTodayBadge.setVisibility(View.GONE);
        } else {
            // Ngày quá khứ chưa hoàn thành
            holder.card.setCardBackgroundColor(Color.WHITE);
            holder.card.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.card.setStrokeWidth(1);
            holder.tvDayNumber.setTextColor(Color.parseColor("#757575"));
            holder.tvDayName.setTextColor(Color.parseColor("#9E9E9E"));
            holder.tvTodayBadge.setVisibility(View.GONE);
        }

        // Click: Ngày hôm nay, đã qua, hoặc đã hoàn thành đều có thể bấm vào tập
        boolean isClickable = isToday || isPast || isCompleted;
        holder.card.setClickable(isClickable);
        holder.card.setForeground(isClickable
                ? holder.itemView.getContext().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground}).getDrawable(0)
                : null);

        if (isClickable) {
            holder.card.setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(day);
            });
        } else {
            holder.card.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return days != null ? days.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvDayNumber, tvDayName, tvTodayBadge;
        ImageView ivTick;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card        = itemView.findViewById(R.id.cardJourneyDay);
            tvDayNumber = itemView.findViewById(R.id.tvJourneyDayNumber);
            tvDayName   = itemView.findViewById(R.id.tvJourneyDayName);
            tvTodayBadge = itemView.findViewById(R.id.tvJourneyTodayBadge);
            ivTick      = itemView.findViewById(R.id.ivJourneyTick);
        }
    }
}