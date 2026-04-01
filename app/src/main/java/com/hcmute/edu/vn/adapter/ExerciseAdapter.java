package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.List;

public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {
    private List<Exercise> exerciseList;
    private boolean isEditMode = false;
    private OnSwapClickListener swapListener;

    public interface OnSwapClickListener {
        void onSwapClick(Exercise exercise, int position);
    }

    public ExerciseAdapter(List<Exercise> exerciseList) {
        this.exerciseList = exerciseList;
    }

    public void setOnSwapClickListener(OnSwapClickListener listener) {
        this.swapListener = listener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
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
        int sets = item.getBaseRecommendedSets() != null ? item.getBaseRecommendedSets() : 3;
        String reps = item.getBaseRecommendedReps() != null ? item.getBaseRecommendedReps() : "12";

        // Cập nhật lên UI (Ví dụ: "3 Hiệp x 12")
        holder.tvDuration.setText(sets + " Hiệp x " + reps);

        // ==========================================
        // XỬ LÝ ẢNH: HỖ TRỢ CẢ LINK WEB (HTTPS) VÀ LOCAL (DRAWABLE)
        // ==========================================
        if (holder.ivThumb != null) {
            String imageUrl = item.getImageUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                // KIỂM TRA: Nếu là đường link từ mạng Internet (Bắt đầu bằng http hoặc https)
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    Glide.with(holder.itemView.getContext())
                            .load(imageUrl)
                            .centerCrop()
                            .placeholder(R.mipmap.ic_launcher) // Ảnh hiển thị tạm trong lúc chờ tải
                            .error(R.mipmap.ic_launcher)       // Ảnh hiển thị nếu tải link thất bại
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Lưu cache để lần sau mở lẹ hơn
                            .into(holder.ivThumb);
                }
                // NẾU KHÔNG PHẢI LINK MẠNG: Xử lý như file có sẵn trong máy (drawable)
                else {
                    String imageName = imageUrl;
                    if (imageName.contains(".")) {
                        imageName = imageName.substring(0, imageName.lastIndexOf('.'));
                    }

                    int imageResId = holder.itemView.getContext().getResources().getIdentifier(
                            imageName,
                            "drawable",
                            holder.itemView.getContext().getPackageName()
                    );

                    if (imageResId != 0) {
                        Glide.with(holder.itemView.getContext())
                                .load(imageResId)
                                .centerCrop()
                                .into(holder.ivThumb);
                    } else {
                        holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            } else {
                holder.ivThumb.setImageResource(R.mipmap.ic_launcher);
            }
        }

        if (holder.btnSwap != null) {
            holder.btnSwap.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
            holder.btnSwap.setOnClickListener(v -> {
                if (swapListener != null) {
                    swapListener.onSwapClick(item, position);
                }
            });
        }

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
        ImageView ivThumb, btnSwap;
        TextView tvName, tvDuration;
        View lineSeparator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.imgExercise);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnSwap = itemView.findViewById(R.id.btnSwap);
            lineSeparator = itemView.findViewById(R.id.lineSeparator);
        }
    }
}