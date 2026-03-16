package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {
    private List<Exercise> exerciseList;

    public ExerciseAdapter(List<Exercise> exerciseList) {
        this.exerciseList = exerciseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise item = exerciseList.get(position);

        holder.tvName.setText(item.getName());
        holder.tvDuration.setText(item.getBaseRecommendedReps());

        // Cập nhật load ảnh bằng Glide
        if (holder.ivThumb != null) {
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                if (imageUrl.startsWith("http")) {
                    // Nếu là đường link từ mạng (Supabase)
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .placeholder(R.drawable.workout_1) // Ảnh mặc định khi đang tải
                            .error(R.mipmap.ic_launcher)       // Ảnh lỗi nếu link die
                            .into(holder.ivThumb);
                } else {
                    // Dữ liệu cũ (Local ID)
                    try {
                        int imageResId = Integer.parseInt(imageUrl);
                        holder.ivThumb.setImageResource(imageResId);
                    } catch (NumberFormatException e) {
                        holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            } else {
                holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
            }
        }

        if (position == exerciseList.size() - 1) {
            holder.lineSeparator.setVisibility(View.GONE);
        } else {
            holder.lineSeparator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return exerciseList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvDuration;
        View lineSeparator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.imgExercise);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            lineSeparator = itemView.findViewById(R.id.lineSeparator);
        }
    }
}