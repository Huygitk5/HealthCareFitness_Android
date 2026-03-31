package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.ExerciseHistoryItem;

import java.util.ArrayList;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<ExerciseHistoryItem> activityList;

    public ActivityAdapter(Context context, ArrayList<ExerciseHistoryItem> activityList) {
        this.context = context;
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseHistoryItem activity = activityList.get(position);

        holder.tvActivityName.setText(activity.getName());
        holder.tvActivityReps.setText(
                context.getString(R.string.exercise_history_reps_value, activity.getRepsText())
        );
        holder.tvActivityKcal.setText(
                String.format(Locale.getDefault(), "%.1f kcal", activity.getBurnedKcal())
        );

        Object imageSource = R.drawable.workout_1;
        if (activity.getImageUrl() != null && !activity.getImageUrl().trim().isEmpty()) {
            try {
                imageSource = Integer.parseInt(activity.getImageUrl());
            } catch (NumberFormatException e) {
                imageSource = activity.getImageUrl();
            }
        }

        Glide.with(context)
                .load(imageSource)
                .placeholder(R.drawable.workout_1)
                .error(R.drawable.workout_1)
                .into(holder.imgActivity);
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgActivity;
        TextView tvActivityName;
        TextView tvActivityReps;
        TextView tvActivityKcal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgActivity = itemView.findViewById(R.id.imgActivity);
            tvActivityName = itemView.findViewById(R.id.tvActivityName);
            tvActivityReps = itemView.findViewById(R.id.tvActivityReps);
            tvActivityKcal = itemView.findViewById(R.id.tvActivityKcal);
        }
    }
}
