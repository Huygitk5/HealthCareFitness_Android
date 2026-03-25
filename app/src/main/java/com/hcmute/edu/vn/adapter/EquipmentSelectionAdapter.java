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
import com.hcmute.edu.vn.model.Equipment;

import java.util.ArrayList;
import java.util.List;

public class EquipmentSelectionAdapter extends RecyclerView.Adapter<EquipmentSelectionAdapter.ViewHolder> {

    private List<Equipment> equipmentList;
    private ArrayList<Integer> selectedIds = new ArrayList<>();
    private OnEquipmentSelectionListener listener;

    public interface OnEquipmentSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public EquipmentSelectionAdapter(List<Equipment> equipmentList, OnEquipmentSelectionListener listener) {
        this.equipmentList = equipmentList;
        this.listener = listener;
    }

    public ArrayList<Integer> getSelectedEquipmentIds() {
        return selectedIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_equipment_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 1. QUAN TRỌNG: Đặt lại scale về 1f ngay từ đầu để chống lỗi teo nhỏ khi tái sử dụng View
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        Equipment eq = equipmentList.get(position);
        holder.tvEqName.setText(eq.getName());

        boolean isSelected = selectedIds.contains(eq.getId());

        if (isSelected) {
            holder.cardEquipment.setCardBackgroundColor(Color.parseColor("#EAF4F3"));
            holder.cardEquipment.setStrokeColor(Color.parseColor("#589A8D"));
            holder.cardEquipment.setStrokeWidth(5);
            holder.tvEqName.setTextColor(Color.parseColor("#589A8D"));
        } else {
            holder.cardEquipment.setCardBackgroundColor(Color.WHITE);
            holder.cardEquipment.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardEquipment.setStrokeWidth(2); // Viền mỏng mờ
            holder.tvEqName.setTextColor(Color.parseColor("#333333"));
        }

        // Hiệu ứng "nhún nhường" khi bấm
        holder.itemView.setOnClickListener(v -> {
            // Thu nhỏ xuống 94%
            v.animate()
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(120)
                    .withEndAction(() -> {

                        // Nảy trở lại 100%
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).withEndAction(() -> {

                            // 2. Dời logic cập nhật Data và UI vào đây (chờ nảy xong mới đổi)
                            // Dùng selectedIds.contains() kiểm tra trực tiếp thay vì biến isSelected cũ
                            if (selectedIds.contains(eq.getId())) {
                                selectedIds.remove(Integer.valueOf(eq.getId()));
                            } else {
                                selectedIds.add(eq.getId());
                            }
                            notifyItemChanged(position); // Update UI
                            listener.onSelectionChanged(selectedIds.size());

                        }).start();

                    }).start();
        });
    }

    @Override
    public int getItemCount() { return equipmentList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardEquipment;
        TextView tvEqName;
        ImageView ivEquipment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEquipment = itemView.findViewById(R.id.cardEquipment);
            tvEqName = itemView.findViewById(R.id.tvEqName);
            ivEquipment = itemView.findViewById(R.id.ivEquipment);
        }
    }
}