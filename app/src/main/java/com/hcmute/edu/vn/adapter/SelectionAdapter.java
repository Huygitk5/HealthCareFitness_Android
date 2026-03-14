package com.hcmute.edu.vn.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.SelectionItem;
import java.util.List;

public class SelectionAdapter extends RecyclerView.Adapter<SelectionAdapter.ViewHolder> {

    private List<SelectionItem> items;
    private OnItemSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnItemSelectedListener {
        void onSelected(String selectedName);
    }

    public SelectionAdapter(List<SelectionItem> items, OnItemSelectedListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selection_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SelectionItem item = items.get(position);
        holder.tvName.setText(item.getName());

        // Logic UI thật đẹp: Nếu được chọn -> Nền xanh nhạt, viền Teal đậm, chữ Teal
        if (selectedPosition == position) {
            holder.cardContainer.setStrokeColor(Color.parseColor("#009688"));
            holder.cardContainer.setCardBackgroundColor(Color.parseColor("#E0F2F1"));
            holder.tvName.setTextColor(Color.parseColor("#009688"));
        } else {
            holder.cardContainer.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardContainer.setCardBackgroundColor(Color.WHITE);
            holder.tvName.setTextColor(Color.BLACK);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged(); // Cập nhật lại màu sắc toàn list
            listener.onSelected(item.getName()); // Báo về Activity
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardContainer;
        TextView tvName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardContainer = itemView.findViewById(R.id.cardContainer);
            tvName = itemView.findViewById(R.id.tvName);
        }
    }
}