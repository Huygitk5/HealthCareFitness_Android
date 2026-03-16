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

    public IngredientAdapter(List<FoodIngredient> ingredientList) {
        this.ingredientList = ingredientList;
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
        // ĐÃ SỬA Ở ĐÂY: Dùng hàm getFormattedQuantity() thay thế
        // Nó sẽ tự động in ra chuỗi đẹp kiểu "100 g" hoặc "2 muỗng"
        // ==========================================================
        if (item.getFormattedQuantity() != null) {
            holder.tvQuantity.setText(item.getFormattedQuantity());
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