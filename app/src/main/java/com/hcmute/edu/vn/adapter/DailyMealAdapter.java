package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.FoodDetailActivity;
import com.hcmute.edu.vn.model.UserDailyMeal;

import java.util.List;

public class DailyMealAdapter extends RecyclerView.Adapter<DailyMealAdapter.ViewHolder> {

    private final Context context;
    private List<UserDailyMeal> mealList;
    private final OnMealItemListener listener;
    private boolean isMealLogged;

    public interface OnMealItemListener {
        void onDeleteClick(UserDailyMeal meal);
        void onSwapClick(UserDailyMeal meal);
    }

    public DailyMealAdapter(Context context, List<UserDailyMeal> mealList, OnMealItemListener listener) {
        this.context = context;
        this.mealList = mealList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_meal_food, parent, false);
        return new ViewHolder(view);
    }

    public void setMealLogged(boolean logged) {
        this.isMealLogged = logged;
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserDailyMeal meal = mealList.get(position);
        double quantity = meal.getQuantityMultiplier();

        if (meal.getFood() != null) {
            holder.tvMealFoodName.setText(meal.getFood().getName());
            holder.tvMealFoodQuantity.setText(getQuantityText(quantity) + " phần");
            holder.tvMealFoodCalo.setText(String.valueOf(Math.round(getValue(meal.getFood().getCalories()) * quantity)));
            holder.tvMealFoodMacroP.setText("P: " + Math.round(getValue(meal.getFood().getProteinG()) * quantity) + "g");
            holder.tvMealFoodMacroC.setText("C: " + Math.round(getValue(meal.getFood().getCarbG()) * quantity) + "g");
            holder.tvMealFoodMacroF.setText("F: " + Math.round(getValue(meal.getFood().getFatG()) * quantity) + "g");

            Glide.with(context)
                    .load(meal.getFood().getImageUrl())
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(holder.imgMealFood);
        } else {
            holder.tvMealFoodName.setText("Món ăn không xác định");
            holder.tvMealFoodQuantity.setText(getQuantityText(quantity) + " phần");
            holder.tvMealFoodCalo.setText("0");
            holder.tvMealFoodMacroP.setText("P: 0g");
            holder.tvMealFoodMacroC.setText("C: 0g");
            holder.tvMealFoodMacroF.setText("F: 0g");
            holder.imgMealFood.setImageResource(R.mipmap.ic_launcher_round);
        }

        holder.itemView.setAlpha(isMealLogged ? 0.55f : 1.0f);

        holder.btnDeleteMealFood.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(meal);
            }
        });

        holder.btnSwapMealFood.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSwapClick(meal);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, FoodDetailActivity.class);
            intent.putExtra("FOOD_ID", meal.getFoodId());
            intent.putExtra("EXTRA_QUANTITY", quantity);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mealList != null ? mealList.size() : 0;
    }

    public void updateList(List<UserDailyMeal> newList) {
        this.mealList = newList;
        notifyDataSetChanged();
    }

    private double getValue(Double value) {
        return value != null ? value : 0.0;
    }

    private String getQuantityText(double quantity) {
        return quantity == Math.floor(quantity) ? String.valueOf((int) quantity) : String.valueOf(quantity);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgMealFood;
        TextView tvMealFoodName;
        TextView tvMealFoodQuantity;
        TextView tvMealFoodMacroP;
        TextView tvMealFoodMacroC;
        TextView tvMealFoodMacroF;
        TextView tvMealFoodCalo;
        ImageButton btnDeleteMealFood;
        ImageButton btnSwapMealFood;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealFood = itemView.findViewById(R.id.imgMealFood);
            tvMealFoodName = itemView.findViewById(R.id.tvMealFoodName);
            tvMealFoodQuantity = itemView.findViewById(R.id.tvMealFoodQuantity);
            tvMealFoodMacroP = itemView.findViewById(R.id.tvMealFoodMacroP);
            tvMealFoodMacroC = itemView.findViewById(R.id.tvMealFoodMacroC);
            tvMealFoodMacroF = itemView.findViewById(R.id.tvMealFoodMacroF);
            tvMealFoodCalo = itemView.findViewById(R.id.tvMealFoodCalo);
            btnDeleteMealFood = itemView.findViewById(R.id.btnDeleteMealFood);
            btnSwapMealFood = itemView.findViewById(R.id.btnSwapMealFood);
        }
    }
}
