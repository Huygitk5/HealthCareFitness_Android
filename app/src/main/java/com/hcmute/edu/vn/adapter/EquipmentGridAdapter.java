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
    private ArrayList<Integer> selectedIds = new ArrayList<>(); // Lưu ID thay vì Name
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

        // TODO: Chèn ảnh thật từ eq.getImageUrl() qua Glide/Picasso nếu có

        boolean isSelected = selectedIds.contains(eq.getId());

        if (isSelected) {
            holder.cardEquipment.setStrokeColor(Color.parseColor("#009688"));
            holder.cardEquipment.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
            holder.ivCheck.setVisibility(View.VISIBLE);
        } else {
            holder.cardEquipment.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardEquipment.setCardBackgroundColor(Color.WHITE);
            holder.ivCheck.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isSelected) {
                selectedIds.remove(Integer.valueOf(eq.getId()));
            } else {
                selectedIds.add(eq.getId());
            }
            notifyItemChanged(position);
            listener.onSelectionChanged(selectedIds.size());
        });
    }

    @Override
    public int getItemCount() { return equipmentList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardEquipment;
        TextView tvEqName;
        ImageView ivEquipment, ivCheck;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEquipment = itemView.findViewById(R.id.cardEquipment);
            tvEqName = itemView.findViewById(R.id.tvEqName);
            ivEquipment = itemView.findViewById(R.id.ivEquipment);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }
    }
}