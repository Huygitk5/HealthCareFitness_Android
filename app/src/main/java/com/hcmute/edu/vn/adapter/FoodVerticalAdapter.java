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

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.FoodDetailActivity;
import com.hcmute.edu.vn.model.Food;

import java.util.ArrayList;
import java.util.List;

public class FoodVerticalAdapter extends RecyclerView.Adapter<FoodVerticalAdapter.ViewHolder> {

    private List<Food> foodList;
    private List<Food> selectedFoods = new ArrayList<>();
    private OnFoodSelectionListener listener;

    public interface OnFoodSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public FoodVerticalAdapter(List<Food> foodList, OnFoodSelectionListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    public List<Food> getSelectedFoods() {
        return selectedFoods;
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

        holder.tvFoodNameList.setText(food.getName());
        holder.tvServingSize.setText(food.getServingSize() != null ? food.getServingSize() : "1 phần");
        holder.tvFoodCaloList.setText(String.valueOf(Math.round(food.getCalories())));

        holder.tvMacroP.setText("P: " + Math.round(food.getProteinG()) + "g");
        holder.tvMacroC.setText("C: " + Math.round(food.getCarbG()) + "g");
        holder.tvMacroF.setText("F: " + Math.round(food.getFatG()) + "g");

        com.bumptech.glide.Glide.with(holder.itemView.getContext())
                .load(food.getImageUrl())
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(holder.imgFoodList);

        boolean isSelected = selectedFoods.contains(food);

        // LOGIC HIỂN THỊ HIGHLIGHT
        if (isSelected) {
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

        // =======================================================
        // 1. SỰ KIỆN: BẤM VÀO NÚT [+] VỚI HIỆU ỨNG NHÚN NẢY
        // =======================================================
        holder.btnAddFoodList.setOnClickListener(v -> {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(() -> {
                    if (selectedFoods.contains(food)) {
                        selectedFoods.remove(food);
                    } else {
                        selectedFoods.add(food);
                    }
                    notifyDataSetChanged();

                    if (listener != null) {
                        listener.onSelectionChanged(selectedFoods.size());
                    }
                }).start();
            }).start();
        });

        // =======================================================
        // 2. SỰ KIỆN: BẤM VÀO THẺ ĐỂ XEM CHI TIẾT
        // =======================================================
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, FoodDetailActivity.class);
            intent.putExtra("FOOD_ID", food.getId()); // Gửi ID món ăn
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return foodList != null ? foodList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFoodVertical, btnAddFoodList; // Khai báo thêm nút [+]
        ShapeableImageView imgFoodList;
        TextView tvFoodNameList, tvServingSize, tvMacroP, tvMacroC, tvMacroF, tvFoodCaloList;

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
            btnAddFoodList = itemView.findViewById(R.id.btnAddFoodList); // Ánh xạ nút [+]
        }
    }
}