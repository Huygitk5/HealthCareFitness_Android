package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.FoodDetailActivity;
import com.hcmute.edu.vn.model.Food;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FoodVerticalAdapter extends RecyclerView.Adapter<FoodVerticalAdapter.ViewHolder> {

    private final List<Food> foodList;
    private final OnFoodSelectionListener listener;
    private final Map<Food, Double> selectedFoodsMap = new HashMap<>();

    public interface OnFoodSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public FoodVerticalAdapter(List<Food> foodList, OnFoodSelectionListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    public Map<Food, Double> getSelectedFoodsMap() {
        return selectedFoodsMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_vertical, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        Food food = foodList.get(position);
        boolean isSelected = selectedFoodsMap.containsKey(food);
        double quantity = isSelected ? selectedFoodsMap.get(food) : 1.0;

        holder.tvFoodNameList.setText(food.getName());
        holder.tvServingSize.setText(getServingText(food.getServingSize(), quantity));
        holder.tvFoodCaloList.setText(String.valueOf(Math.round(getValue(food.getCalories()) * quantity)));
        holder.tvMacroP.setText("P: " + Math.round(getValue(food.getProteinG()) * quantity) + "g");
        holder.tvMacroC.setText("C: " + Math.round(getValue(food.getCarbG()) * quantity) + "g");
        holder.tvMacroF.setText("F: " + Math.round(getValue(food.getFatG()) * quantity) + "g");

        Glide.with(holder.itemView.getContext())
                .load(food.getImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(holder.imgFoodList);

        if (isSelected) {
            holder.tvFoodNameList.setText(food.getName() + " (" + getQuantityText(quantity) + " phần)");
            holder.cardFoodVertical.setStrokeWidth(4);
            holder.cardFoodVertical.setStrokeColor(Color.parseColor("#589A8D"));
            holder.cardFoodVertical.setCardBackgroundColor(Color.parseColor("#F1F8F7"));
            holder.tvFoodNameList.setTextColor(Color.parseColor("#589A8D"));
        } else {
            holder.cardFoodVertical.setStrokeWidth(2);
            holder.cardFoodVertical.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardFoodVertical.setCardBackgroundColor(Color.WHITE);
            holder.tvFoodNameList.setTextColor(Color.parseColor("#212121"));
        }

        holder.btnAddFoodList.setOnClickListener(v -> {
            String[] options = {"0.5 phần", "1.0 phần", "1.5 phần", "2.0 phần", "Bỏ chọn món này"};

            new android.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Chọn số lượng cho " + food.getName())
                    .setItems(options, (dialog, which) -> {
                        if (which == options.length - 1) {
                            selectedFoodsMap.remove(food);
                        } else {
                            selectedFoodsMap.put(food, 0.5 * (which + 1));
                        }

                        notifyItemChanged(position);
                        if (listener != null) {
                            listener.onSelectionChanged(selectedFoodsMap.size());
                        }
                    })
                    .show();
        });

        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, FoodDetailActivity.class);
            intent.putExtra("FOOD_ID", food.getId());
            intent.putExtra("EXTRA_QUANTITY", quantity);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return foodList != null ? foodList.size() : 0;
    }

    private double getValue(Double value) {
        return value != null ? value : 0.0;
    }

    private String getServingText(String servingSize, double quantity) {
        if (quantity == 1.0 && servingSize != null && !servingSize.trim().isEmpty()) {
            return servingSize;
        }
        return getQuantityText(quantity) + " phần";
    }

    private String getQuantityText(double quantity) {
        return quantity == Math.floor(quantity) ? String.valueOf((int) quantity) : String.valueOf(quantity);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFoodVertical;
        MaterialCardView btnAddFoodList;
        ShapeableImageView imgFoodList;
        TextView tvFoodNameList;
        TextView tvServingSize;
        TextView tvMacroP;
        TextView tvMacroC;
        TextView tvMacroF;
        TextView tvFoodCaloList;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFoodVertical = itemView.findViewById(R.id.cardFoodVertical);
            imgFoodList = itemView.findViewById(R.id.imgFoodList);
            tvFoodNameList = itemView.findViewById(R.id.tvFoodNameList);
            tvServingSize = itemView.findViewById(R.id.tvServingSize);
            tvMacroP = itemView.findViewById(R.id.tvMacroP);
            tvMacroC = itemView.findViewById(R.id.tvMacroC);
            tvMacroF = itemView.findViewById(R.id.tvMacroF);
            tvFoodCaloList = itemView.findViewById(R.id.tvFoodCaloList);
            btnAddFoodList = itemView.findViewById(R.id.btnAddFoodList);
        }
    }
}
