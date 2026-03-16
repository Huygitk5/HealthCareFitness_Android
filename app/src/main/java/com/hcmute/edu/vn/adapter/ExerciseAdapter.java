package com.hcmute.edu.vn.adapter;

// ĐÃ XÓA DÒNG IMPORT R BỊ SAI CỦA ANDROID STUDIO
// ĐÃ THÊM DÒNG IMPORT R CHUẨN CỦA APP BẠN VÀO ĐÂY:
import com.hcmute.edu.vn.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // IMPORT THƯ VIỆN GLIDE
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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

        // ==========================================
        // XỬ LÝ ẢNH TỪ TÊN FILE LOCAL (TRONG DRAWABLE)
        // ==========================================
        if (holder.ivThumb != null) {
            String imageName = item.getImageUrl(); // Trả về tên file (VD: "pushup", "squat")

            if (imageName != null && !imageName.isEmpty()) {

                // Mẹo nhỏ: Nếu trên DB bạn lỡ lưu có đuôi ".png" hay ".jpg", ta cắt nó đi
                if (imageName.contains(".")) {
                    imageName = imageName.substring(0, imageName.lastIndexOf('.'));
                }

                // "Thủ thư" đi tìm ID ảnh dựa trên tên chuỗi
                int imageResId = holder.itemView.getContext().getResources().getIdentifier(
                        imageName,
                        "drawable", // Nếu bạn để ảnh ở thư mục mipmap thì đổi chữ này thành "mipmap"
                        holder.itemView.getContext().getPackageName()
                );

                if (imageResId != 0) {
                    // TÌM THẤY ẢNH: Đưa cho Glide load để tối ưu hiệu năng
                    Glide.with(holder.itemView.getContext())
                            .load(imageResId)
                            .centerCrop()
                            .into(holder.ivThumb);
                } else {
                    // KHÔNG TÌM THẤY: Gán ảnh mặc định (Tránh lỗi văng app)
                    Glide.with(holder.itemView.getContext())
                            .load(R.mipmap.ic_launcher) // Đã hết bị đỏ rồi nhé!
                            .centerCrop()
                            .into(holder.ivThumb);
                }
            } else {
                // Tên ảnh bị rỗng từ DB
                holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
            }
        }

        // Xử lý dòng kẻ ngang cuối cùng
        if (position == exerciseList.size() - 1) {
            holder.lineSeparator.setVisibility(View.GONE);
        } else {
            holder.lineSeparator.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return exerciseList != null ? exerciseList.size() : 0;
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