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
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.WorkoutDayExercise;
import java.util.ArrayList;
import java.util.List;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {
    private List<WorkoutDay> mList;

    public WorkoutAdapter(List<WorkoutDay> list) {
        this.mList = list;
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

        String title = item.getName() != null ? item.getName() : "Ngày Tập";
        holder.tvTitle.setText(title);

        boolean isRestDay = title.toLowerCase().contains("nghỉ");

        if (isRestDay) {
            holder.tvSub.setText("Nghỉ ngơi");
            holder.ivRest.setVisibility(View.VISIBLE);
            holder.btnStart.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.tvSub.setText("Danh sách bài tập");
            holder.ivRest.setVisibility(View.GONE);

            // KỸ THUẬT BYPASS: Đóng gói bài tập gửi thẳng qua trang sau!
            View.OnClickListener clickListener = v -> {
                Intent intent = new Intent(v.getContext(), ExerciseListActivity.class);
                ArrayList<Exercise> exercisesToPass = new ArrayList<>();

                if (item.getExercises() != null) {
                    for (WorkoutDayExercise wde : item.getExercises()) {
                        if (wde.getExercise() != null) {
                            Exercise ex = wde.getExercise();
                            if (wde.getReps() != null) ex.setBaseRecommendedReps(String.valueOf(wde.getReps()));
                            if (wde.getSets() != null) ex.setBaseRecommendedSets(wde.getSets());
                            exercisesToPass.add(ex);
                        }
                    }
                }

                // Gửi danh sách và gửi luôn cả tên ngày để hiển thị
                intent.putExtra("EXTRA_EXERCISE_LIST", exercisesToPass);
                intent.putExtra("EXTRA_DAY_TITLE", title);
                v.getContext().startActivity(intent);
            };

            holder.itemView.setOnClickListener(clickListener);

            if (position == 0) {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#B5D3C9"));
                holder.tvTitle.setTextColor(Color.parseColor("#3E665D"));
                holder.btnStart.setVisibility(View.VISIBLE);
                holder.btnStart.setOnClickListener(clickListener);
            } else {
                holder.cardView.setCardBackgroundColor(Color.parseColor("#D1E4DE"));
                holder.tvTitle.setTextColor(Color.parseColor("#4A7A6F"));
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