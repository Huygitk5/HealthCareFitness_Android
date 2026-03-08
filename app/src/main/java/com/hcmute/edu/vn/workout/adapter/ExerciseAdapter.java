package com.hcmute.edu.vn.workout.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.workout.model.ExerciseItem;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {
    private List<ExerciseItem> exerciseList;

    public ExerciseAdapter(List<ExerciseItem> exerciseList) {
        this.exerciseList = exerciseList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_exercise_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseItem item = exerciseList.get(position);
        holder.tvName.setText(item.getName());
        holder.tvDuration.setText(item.getDuration());

        // Kiểm tra an toàn trước khi set ảnh
        if (holder.ivThumb != null) {
            holder.ivThumb.setImageResource(item.getImageResId());
        }
    }

    @Override
    public int getItemCount() { return exerciseList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvDuration;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // XÁC NHẬN ID Ở ĐÂY PHẢI KHỚP VỚI XML
            // Trong file item_exercise_detail.xml, thẻ <ImageView> phải có android:id="@+id/ivExImage"
            ivThumb = itemView.findViewById(R.id.ivExImage);
            tvName = itemView.findViewById(R.id.tvExNameItem);
            tvDuration = itemView.findViewById(R.id.tvExDuration);
        }
    }
}