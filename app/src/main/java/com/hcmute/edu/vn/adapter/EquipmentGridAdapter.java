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

public class EquipmentGridAdapter extends RecyclerView.Adapter<EquipmentGridAdapter.ViewHolder> {

    private List<Equipment> equipmentList;
    private ArrayList<Integer> selectedIds = new ArrayList<>();
    private OnEquipmentSelectionListener listener;

    public interface OnEquipmentSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public EquipmentGridAdapter(List<Equipment> equipmentList, OnEquipmentSelectionListener listener) {
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
            v.animate()
                    .scaleX(0.94f).scaleY(0.94f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();

                        if (isSelected) {
                            selectedIds.remove(Integer.valueOf(eq.getId()));
                        } else {
                            selectedIds.add(eq.getId());
                        }
                        notifyItemChanged(position); // Update UI
                        listener.onSelectionChanged(selectedIds.size());
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