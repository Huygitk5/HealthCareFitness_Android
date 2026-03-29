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
import com.hcmute.edu.vn.model.UserDailyMeal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DailyMealAdapter extends RecyclerView.Adapter<DailyMealAdapter.ViewHolder> {

    private Context context;
    private List<UserDailyMeal> mealList;
    private OnMealItemListener listener;
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

        if (meal.getFood() != null) {
            holder.tvMealFoodName.setText(meal.getFood().getName());

            double totalCalo = meal.getFood().getCalories() * meal.getQuantityMultiplier();
            holder.tvMealFoodCalo.setText(String.valueOf(Math.round(totalCalo)));

            Glide.with(context)
                    .load(meal.getFood().getImageUrl())
                    .placeholder(R.mipmap.ic_launcher_round)
                    .error(R.mipmap.ic_launcher_round)
                    .into(holder.imgMealFood);
        } else {
            holder.tvMealFoodName.setText("Món ăn không xác định");
            holder.tvMealFoodCalo.setText("0");
        }

        double qty = meal.getQuantityMultiplier();
        String quantityText = (qty == Math.floor(qty))
                ? String.valueOf((int)qty)
                : String.valueOf(qty);
        holder.tvMealFoodQuantity.setText(quantityText + " phần");

        holder.itemView.setAlpha(isMealLogged ? 0.55f : 1.0f);

        // Sự kiện xóa món ăn
        holder.btnDeleteMealFood.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(meal);
        });

        holder.btnSwapMealFood.setOnClickListener(v -> {
            if (listener != null) listener.onSwapClick(meal);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.hcmute.edu.vn.activity.FoodDetailActivity.class);
            intent.putExtra("FOOD_ID", meal.getFoodId());
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView imgMealFood;
        TextView tvMealFoodName, tvMealFoodQuantity, tvMealFoodCalo;
        ImageButton btnDeleteMealFood, btnSwapMealFood;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgMealFood = itemView.findViewById(R.id.imgMealFood);
            tvMealFoodName = itemView.findViewById(R.id.tvMealFoodName);
            tvMealFoodQuantity = itemView.findViewById(R.id.tvMealFoodQuantity);
            tvMealFoodCalo = itemView.findViewById(R.id.tvMealFoodCalo);
            btnDeleteMealFood = itemView.findViewById(R.id.btnDeleteMealFood);
            btnSwapMealFood = itemView.findViewById(R.id.btnSwapMealFood);
        }
    }
}