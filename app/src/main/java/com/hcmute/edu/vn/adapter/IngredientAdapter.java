package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.FoodIngredient;

import java.util.List;

public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {

    private List<FoodIngredient> ingredientList;
    private double multiplier = 1.0;

    public IngredientAdapter(List<FoodIngredient> ingredientList) {
        this.ingredientList = ingredientList;
    }

    public IngredientAdapter(List<FoodIngredient> ingredientList, double multiplier) {
        this.ingredientList = ingredientList;
        this.multiplier = multiplier;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FoodIngredient item = ingredientList.get(position);

        // Tránh lỗi NullPointerException nếu dữ liệu bị rỗng
        if (item.getIngredient() != null) {
            holder.tvName.setText(item.getIngredient().getName());
        }

        // ==========================================================
        // Dùng hàm getFormattedQuantity(multiplier)
        // Nó sẽ tự động nhân theo số phần
        // ==========================================================
        if (item.getIngredient() != null) {
            holder.tvQuantity.setText(item.getFormattedQuantity(multiplier));
        } else {
            holder.tvQuantity.setText("Tùy khẩu vị");
        }
    }

    @Override
    public int getItemCount() {
        return ingredientList != null ? ingredientList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvIngredientName);
            tvQuantity = itemView.findViewById(R.id.tvIngredientQuantity);
        }
    }
}