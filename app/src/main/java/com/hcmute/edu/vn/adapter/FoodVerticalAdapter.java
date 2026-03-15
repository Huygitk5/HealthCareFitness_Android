package com.hcmute.edu.vn.adapter;

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
import com.hcmute.edu.vn.model.Food;

import java.util.ArrayList;
import java.util.List;

public class FoodVerticalAdapter extends RecyclerView.Adapter<FoodVerticalAdapter.ViewHolder> {

    private List<Food> foodList;
    private List<Food> selectedFoods = new ArrayList<>();
    private OnFoodSelectionListener listener;

    // Interface để báo cho Activity biết số lượng món ăn đang được chọn
    public interface OnFoodSelectionListener {
        void onSelectionChanged(int selectedCount);
    }

    public FoodVerticalAdapter(List<Food> foodList, OnFoodSelectionListener listener) {
        this.foodList = foodList;
        this.listener = listener;
    }

    // Hàm để lấy danh sách các món ăn đã được người dùng tick chọn
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
        // 1. Reset kích thước về 1f để chống lỗi kẹt giao diện khi cuộn
        holder.itemView.setScaleX(1f);
        holder.itemView.setScaleY(1f);

        Food food = foodList.get(position);

        // Gắn dữ liệu văn bản
        holder.tvFoodNameList.setText(food.getName());
        holder.tvServingSize.setText(food.getServingSize() != null ? food.getServingSize() : "1 phần");
        holder.tvFoodCaloList.setText(String.valueOf(Math.round(food.getCalories())));

        holder.tvMacroP.setText("P: " + Math.round(food.getProteinG()) + "g");
        holder.tvMacroC.setText("C: " + Math.round(food.getCarbG()) + "g");
        holder.tvMacroF.setText("F: " + Math.round(food.getFatG()) + "g");

        // Gắn ảnh mặc định (sau này bạn thay bằng Glide/Picasso)
        holder.imgFoodList.setImageResource(R.mipmap.ic_launcher_round);

        // Kiểm tra xem món này có đang nằm trong danh sách "được tick chọn" không
        boolean isSelected = selectedFoods.contains(food);

        // 2. LOGIC HIỂN THỊ HIGHLIGHT (Y hệt màn hình Workout)
        if (isSelected) {
            holder.cardFoodVertical.setStrokeWidth(4);
            holder.cardFoodVertical.setStrokeColor(Color.parseColor("#589A8D")); // Xanh đậm
            holder.cardFoodVertical.setCardBackgroundColor(Color.parseColor("#F1F8F7")); // Nền xanh nhạt
            holder.tvFoodNameList.setTextColor(Color.parseColor("#589A8D"));
        } else {
            holder.cardFoodVertical.setStrokeWidth(2);
            holder.cardFoodVertical.setStrokeColor(Color.parseColor("#E0E0E0")); // Xám nhạt
            holder.cardFoodVertical.setCardBackgroundColor(Color.WHITE);
            holder.tvFoodNameList.setTextColor(Color.parseColor("#212121"));
        }

        // 3. Hiệu ứng click Nhún - Nảy
        holder.itemView.setOnClickListener(v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction(() -> {

                v.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction(() -> {

                    // ĐÃ SỬA: Xóa sạch danh sách cũ trước khi thêm món mới vào (Chỉ chọn 1)
                    selectedFoods.clear();
                    selectedFoods.add(food);

                    notifyDataSetChanged();

                    if (listener != null) {
                        listener.onSelectionChanged(selectedFoods.size());
                    }

                }).start();
            }).start();
        });
    }

    @Override
    public int getItemCount() {
        return foodList != null ? foodList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFoodVertical;
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
        }
    }
}