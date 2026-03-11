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
        // 1. Đã đổi thành layout mới chuẩn Figma của bạn
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_exercise, parent, false);
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

        // 2. Logic làm đẹp: Ẩn đường kẻ xám ở bài tập cuối cùng trong danh sách
        if (position == exerciseList.size() - 1) {
            holder.lineSeparator.setVisibility(View.GONE);
        } else {
            holder.lineSeparator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() { return exerciseList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvDuration;
        View lineSeparator; // Thêm biến cho đường kẻ xám

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            // 3. Đã cập nhật toàn bộ ID khớp 100% với file item_workout_exercise.xml
            ivThumb = itemView.findViewById(R.id.imgExercise);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            lineSeparator = itemView.findViewById(R.id.lineSeparator);
        }
    }
}