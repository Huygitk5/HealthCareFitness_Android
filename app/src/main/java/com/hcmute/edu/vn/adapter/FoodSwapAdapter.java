package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Food;

import java.util.List;

public class FoodSwapAdapter extends RecyclerView.Adapter<FoodSwapAdapter.ViewHolder> {
    private Context context;
    private List<Food> foodList;
    private List<Double> quantityList; // Chứa số phần ăn (1.5, 2.0...)
    private OnSwapClickListener listener;

    public interface OnSwapClickListener {
        void onSwapClick(Food selectedFood, double quantity);
    }

    public FoodSwapAdapter(Context context, List<Food> foodList, List<Double> quantityList, OnSwapClickListener listener) {
        this.context = context;
        this.foodList = foodList;
        this.quantityList = quantityList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tái sử dụng giao diện thẻ món ăn của bạn (Ví dụ: item_food_vertical)
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foodList.get(position);
        double qty = quantityList.get(position);
        double totalCalo = (food.getCalories() != null ? food.getCalories() : 0) * qty;

        // HIỂN THỊ: Gà luộc (1.5 phần)
        holder.tvFoodNameList.setText(food.getName() + " (" + qty + " phần)");

        if (holder.tvFoodCaloList != null) {
            holder.tvFoodCaloList.setText(Math.round(totalCalo) + " Kcal");
        }

        if (holder.imgFoodList != null) {
            Glide.with(context).load(food.getImageUrl()).into(holder.imgFoodList);
        }

        // Đổi icon nút [+] thành icon mũi tên vòng tròn (Swap)
        if (holder.btnAddFoodList != null) {
            // Gắn sự kiện click cho nguyên cái thẻ Nút
            holder.btnAddFoodList.setOnClickListener(v -> listener.onSwapClick(food, qty));
        }

        // Bấm vào bất cứ đâu trên thẻ cũng được tính là Chọn đổi món
        holder.itemView.setOnClickListener(v -> listener.onSwapClick(food, qty));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodNameList, tvFoodCaloList;
        ImageView imgFoodList;
        View btnAddFoodList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodNameList = itemView.findViewById(R.id.tvFoodNameList);
            tvFoodCaloList = itemView.findViewById(R.id.tvFoodCaloList);
            imgFoodList = itemView.findViewById(R.id.imgFoodList);
            // 2. Ép kiểu về View thay vì ImageButton
            btnAddFoodList = itemView.findViewById(R.id.btnAddFoodList);
        }
    }
}