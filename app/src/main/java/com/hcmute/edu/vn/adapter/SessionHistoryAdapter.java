package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.UserWorkoutSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SessionHistoryAdapter extends RecyclerView.Adapter<SessionHistoryAdapter.ViewHolder> {

    private final List<UserWorkoutSession> sessions;

    public SessionHistoryAdapter(List<UserWorkoutSession> sessions) {
        this.sessions = sessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_session_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserWorkoutSession session = sessions.get(position);

        // 1. Tên buổi tập
        String name = "Buổi tập";
        if (session.getWorkoutDay() != null && session.getWorkoutDay().getName() != null) {
            name = session.getWorkoutDay().getName();
            int dayOrder = session.getWorkoutDay().getDayOrder() != null
                ? session.getWorkoutDay().getDayOrder() : 0;
            if (dayOrder > 0) name = "Ngày " + dayOrder + ": " + name;
        }
        holder.tvSessionName.setText(name);

        // 2. Thời gian
        long secs = session.getDurationSeconds();
        String duration;
        if (secs < 60) {
            duration = secs + "s";
        } else {
            long mins = secs / 60;
            long remainSecs = secs % 60;
            duration = mins + "m " + (remainSecs > 0 ? remainSecs + "s" : "");
        }
        holder.tvSessionDuration.setText(duration);

        // 3. Kcal
        double kcal = session.getEstimatedKcal();
        holder.tvSessionKcal.setText(String.format(Locale.getDefault(), "%.1f Kcal", kcal));

        // 4. Ngày + giờ
        String timestamp = session.getFinishedAt() != null
            ? session.getFinishedAt() : session.getStartedAt();
        holder.tvSessionDate.setText(formatDisplayDate(timestamp));

        // 5. Thumbnail — dùng Glide
        Glide.with(holder.itemView.getContext())
            .load(R.drawable.workout_1)
            .placeholder(R.drawable.workout_1)
            .error(R.drawable.workout_1)
            .transform(new com.bumptech.glide.load.resource.bitmap.RoundedCorners(24))
            .into(holder.ivSessionThumb);
    }

    private String formatDisplayDate(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.length() < 16) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(isoTimestamp.substring(0, 19));
            if (date == null) return "";

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            int day   = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int month = cal.get(java.util.Calendar.MONTH) + 1;
            int hour  = cal.get(java.util.Calendar.HOUR_OF_DAY);
            int min   = cal.get(java.util.Calendar.MINUTE);

            String ampm = hour >= 12 ? "CH" : "SA";
            int hour12  = hour % 12;
            if (hour12 == 0) hour12 = 12;

            return String.format(Locale.getDefault(),
                "%d thg %d\n%d:%02d %s", day, month, hour12, min, ampm);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivSessionThumb;
        TextView tvSessionName;
        TextView tvSessionDuration;
        TextView tvSessionKcal;
        TextView tvSessionDate;

        ViewHolder(View view) {
            super(view);
            ivSessionThumb = view.findViewById(R.id.ivSessionThumb);
            tvSessionName = view.findViewById(R.id.tvSessionName);
            tvSessionDuration = view.findViewById(R.id.tvSessionDuration);
            tvSessionKcal = view.findViewById(R.id.tvSessionKcal);
            tvSessionDate = view.findViewById(R.id.tvSessionDate);
        }
    }
}
