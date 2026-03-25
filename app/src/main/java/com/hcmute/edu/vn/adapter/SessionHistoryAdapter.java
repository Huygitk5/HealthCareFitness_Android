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

        String name = (session.getWorkoutDay() != null && session.getWorkoutDay().getName() != null)
                ? session.getWorkoutDay().getName() : "Buổi tập";
        holder.tvSessionName.setText(name);

        long duration = session.getDurationSeconds();
        if (duration < 60) {
            holder.tvDuration.setText(duration + "s");
        } else {
            holder.tvDuration.setText((duration / 60) + "m " + (duration % 60) + "s");
        }

        holder.tvKcal.setText(String.format(Locale.getDefault(), "%.1f Kcal", session.getEstimatedKcal()));

        String displayDate = session.getFinishedAt() != null ? session.getFinishedAt() : session.getStartedAt();
        if (displayDate != null && displayDate.length() >= 16) {
            try {
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                Date date = inFormat.parse(displayDate.substring(0, 16));
                SimpleDateFormat outFormat = new SimpleDateFormat("dd 'thg' MM, h:mm a", Locale.getDefault());
                if (date != null) {
                    holder.tvSessionDate.setText(outFormat.format(date));
                }
            } catch (Exception e) {
                holder.tvSessionDate.setText(displayDate);
            }
        }

        Glide.with(holder.itemView.getContext())
             .load(R.drawable.workout_1) // Placeholder/default for now
             .placeholder(R.drawable.bg_circle_primary)
             .transform(new RoundedCorners(16))
             .into(holder.ivPlanThumb);
    }

    @Override
    public int getItemCount() {
        return sessions != null ? sessions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlanThumb;
        TextView tvSessionName;
        TextView tvDuration;
        TextView tvKcal;
        TextView tvSessionDate;

        ViewHolder(View view) {
            super(view);
            ivPlanThumb = view.findViewById(R.id.ivPlanThumb);
            tvSessionName = view.findViewById(R.id.tvSessionName);
            tvDuration = view.findViewById(R.id.tvDuration);
            tvKcal = view.findViewById(R.id.tvKcal);
            tvSessionDate = view.findViewById(R.id.tvSessionDate);
        }
    }
}
