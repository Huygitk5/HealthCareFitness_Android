package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.MuscleGroup;

import java.util.List;
import java.util.Set;

public class MuscleSelectionAdapter extends RecyclerView.Adapter<MuscleSelectionAdapter.ViewHolder> {

    private List<MuscleGroup> muscleList;
    private Set<Integer> validMuscleIds; // Chứa ID các nhóm cơ khả dụng
    private int selectedPosition = -1;
    private OnMuscleSelectionListener listener;

    public interface OnMuscleSelectionListener {
        void onSelected(MuscleGroup selectedMuscle);
    }

    public MuscleSelectionAdapter(List<MuscleGroup> muscleList, Set<Integer> validMuscleIds, OnMuscleSelectionListener listener) {
        this.muscleList = muscleList;
        this.validMuscleIds = validMuscleIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dùng file item_muscle_row.xml anh đưa ở bài trước
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_muscle_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MuscleGroup muscle = muscleList.get(position);
        holder.tvMuscleName.setText(muscle.getName());

        // Kiểm tra xem nhóm cơ này có bài tập nào tương ứng với thiết bị không
        boolean isValid = validMuscleIds.contains(muscle.getId());

        if (!isValid) {
            // NẾU KHÔNG CÓ BÀI TẬP -> DISABLE
            holder.cardMuscle.setAlpha(0.4f); // Làm mờ đi
            holder.cardMuscle.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.cardMuscle.setStrokeColor(Color.TRANSPARENT);
            holder.tvMuscleName.setTextColor(Color.parseColor("#9E9E9E"));

            // Chặn click
            holder.itemView.setOnClickListener(null);
        } else {
            // NẾU CÓ BÀI TẬP -> ENABLE
            holder.cardMuscle.setAlpha(1.0f);

            if (selectedPosition == position) {
                // Trạng thái đang được chọn
                holder.cardMuscle.setStrokeColor(Color.parseColor("#009688"));
                holder.cardMuscle.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
                holder.tvMuscleName.setTextColor(Color.parseColor("#009688"));
            } else {
                // Trạng thái bình thường
                holder.cardMuscle.setStrokeColor(Color.parseColor("#E0E0E0"));
                holder.cardMuscle.setCardBackgroundColor(Color.WHITE);
                holder.tvMuscleName.setTextColor(Color.BLACK);
            }

            holder.itemView.setOnClickListener(v -> {
                int previousSelected = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // Cập nhật giao diện mượt mà (Chỉ vẽ lại 2 thẻ bị thay đổi)
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);

                listener.onSelected(muscle);
            });
        }
    }

    @Override
    public int getItemCount() {
        return muscleList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardMuscle;
        TextView tvMuscleName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardMuscle = itemView.findViewById(R.id.cardMuscle);
            tvMuscleName = itemView.findViewById(R.id.tvMuscleName);
        }
    }
}