package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.hcmute.edu.vn.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private Context context;
    private List<Date> dateList;
    private int selectedPosition = 0; // Mặc định chọn ngày đầu tiên (Hôm nay)
    private OnDateSelectedListener listener;

    // Interface để báo cho Activity biết khi user đổi ngày
    public interface OnDateSelectedListener {
        void onDateSelected(Date date);
    }

    public CalendarAdapter(Context context, List<Date> dateList, OnDateSelectedListener listener) {
        this.context = context;
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Date date = dateList.get(position);

        // Format ngày (Ví dụ: "16") và Thứ (Ví dụ: "T2")
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat weekDayFormat = new SimpleDateFormat("E", new Locale("vi", "VN")); // Tiếng Việt

        holder.tvDate.setText(dayFormat.format(date));

        // Cắt bớt chữ "Thứ " nếu có, chỉ lấy "T2", "T3" cho gọn
        String weekDay = weekDayFormat.format(date).replace("Thứ ", "T").replace("Chủ nhật", "CN");
        holder.tvDayOfWeek.setText(weekDay);

        // Xử lý đổi màu khi được chọn
        if (selectedPosition == position) {
            holder.cardCalendarDay.setCardBackgroundColor(Color.parseColor("#4DAA9A")); // Nền xanh
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#E0F2F1")); // Chữ xanh nhạt
            holder.tvDate.setTextColor(Color.WHITE); // Chữ trắng
            holder.cardCalendarDay.setStrokeWidth(0);
        } else {
            holder.cardCalendarDay.setCardBackgroundColor(Color.WHITE); // Nền trắng
            holder.tvDayOfWeek.setTextColor(Color.parseColor("#757575")); // Chữ xám
            holder.tvDate.setTextColor(Color.parseColor("#212121")); // Chữ đen
            holder.cardCalendarDay.setStrokeWidth(dpToPx(1));
        }

        // Sự kiện Click chọn ngày
        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition != holder.getAdapterPosition()) {
                int previousPosition = selectedPosition;
                selectedPosition = holder.getAdapterPosition();

                // Vẽ lại giao diện 2 ô (ô cũ mất màu, ô mới lên màu)
                notifyItemChanged(previousPosition);
                notifyItemChanged(selectedPosition);

                // Báo ra ngoài Activity
                if (listener != null) {
                    listener.onDateSelected(date);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList != null ? dateList.size() : 0;
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCalendarDay;
        TextView tvDayOfWeek, tvDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCalendarDay = itemView.findViewById(R.id.cardCalendarDay);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}