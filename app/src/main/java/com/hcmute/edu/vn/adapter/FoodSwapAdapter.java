package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Food;

import java.util.List;

public class FoodSwapAdapter extends RecyclerView.Adapter<FoodSwapAdapter.ViewHolder> {
    private final Context context;
    private final List<Food> foodList;
    private final List<Double> quantityList;
    private final OnSwapClickListener listener;

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
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Food food = foodList.get(position);
        double quantity = quantityList.get(position);

        holder.tvFoodNameList.setText(food.getName() + " (" + getQuantityText(quantity) + " phần)");
        holder.tvServingSize.setText(getQuantityText(quantity) + " phần");
        holder.tvFoodCaloList.setText(String.valueOf(Math.round(getValue(food.getCalories()) * quantity)));
        holder.tvMacroP.setText("P: " + Math.round(getValue(food.getProteinG()) * quantity) + "g");
        holder.tvMacroC.setText("C: " + Math.round(getValue(food.getCarbG()) * quantity) + "g");
        holder.tvMacroF.setText("F: " + Math.round(getValue(food.getFatG()) * quantity) + "g");

        Glide.with(context)
                .load(food.getImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(holder.imgFoodList);

        holder.btnAddFoodList.setOnClickListener(v -> listener.onSwapClick(food, quantity));
        holder.itemView.setOnClickListener(v -> listener.onSwapClick(food, quantity));
    }

    @Override
    public int getItemCount() {
        return foodList.size();
    }

    private double getValue(Double value) {
        return value != null ? value : 0.0;
    }

    private String getQuantityText(double quantity) {
        return quantity == Math.floor(quantity) ? String.valueOf((int) quantity) : String.valueOf(quantity);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFoodNameList;
        TextView tvServingSize;
        TextView tvMacroP;
        TextView tvMacroC;
        TextView tvMacroF;
        TextView tvFoodCaloList;
        ImageView imgFoodList;
        View btnAddFoodList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFoodNameList = itemView.findViewById(R.id.tvFoodNameList);
            tvServingSize = itemView.findViewById(R.id.tvServingSize);
            tvMacroP = itemView.findViewById(R.id.tvMacroP);
            tvMacroC = itemView.findViewById(R.id.tvMacroC);
            tvMacroF = itemView.findViewById(R.id.tvMacroF);
            tvFoodCaloList = itemView.findViewById(R.id.tvFoodCaloList);
            imgFoodList = itemView.findViewById(R.id.imgFoodList);
            btnAddFoodList = itemView.findViewById(R.id.btnAddFoodList);
        }
    }
}
