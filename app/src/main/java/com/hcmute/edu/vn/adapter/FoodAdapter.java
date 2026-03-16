package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Food;

import java.util.List;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private Context context;
    private List<Food> foodList;
    private int selectedPosition = 0;

    // Tạo "Còi báo động" khi user bấm món mới
    private OnFoodSelectedListener listener;
    public interface OnFoodSelectedListener {
        void onFoodSelected(Food food);
    }

    // Nhớ truyền còi báo động vào qua Constructor
    public FoodAdapter(Context context, List<Food> foodList, OnFoodSelectedListener listener) {
        this.context = context;
        this.foodList = foodList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_food_card, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = foodList.get(position);

        holder.tvFoodName.setText(food.getName());
        holder.tvFoodCalories.setText("(" + Math.round(food.getCalories()) + " Kcal)");

        // ==========================================================
        // ĐÃ SỬA: Dùng Glide để tải ảnh từ URL thay vì gán chết icon
        // ==========================================================
        Glide.with(context)
                .load(food.getImageUrl()) // Hàm này lấy đường link ảnh từ model Food của bạn. (Nếu biến của bạn tên khác như getImage() thì sửa lại cho khớp nhé).
                .placeholder(R.mipmap.ic_launcher_round) // Ảnh hiển thị TẠM THỜI trong lúc chờ mạng tải ảnh thật
                .error(R.mipmap.ic_launcher_round)       // Ảnh hiển thị NẾU link ảnh bị lỗi hoặc không có mạng
                .into(holder.imgFood);

        // Hiệu ứng màu sắc
        if (selectedPosition == position) {
            holder.cardFood.setCardBackgroundColor(Color.parseColor("#F2F9F8"));
            holder.cardFood.setStrokeColor(Color.parseColor("#4DAA9A"));
            holder.cardFood.setStrokeWidth(dpToPx(2));
        } else {
            holder.cardFood.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
            holder.cardFood.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardFood.setStrokeWidth(dpToPx(1));
        }

        // Bắt sự kiện Click đổi món
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Vẽ lại UI cho cái cũ (mất màu) và cái mới (lên màu)
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);

            // Kích hoạt còi báo động đẩy Data về Activity
            if (listener != null) {
                listener.onFoodSelected(foodList.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return foodList != null ? foodList.size() : 0;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static class FoodViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardFood;
        ImageView imgFood;
        TextView tvFoodName, tvFoodCalories;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFood = itemView.findViewById(R.id.cardFood);
            imgFood = itemView.findViewById(R.id.imgFood);
            tvFoodName = itemView.findViewById(R.id.tvFoodName);
            tvFoodCalories = itemView.findViewById(R.id.tvFoodCalories);
        }
    }
}