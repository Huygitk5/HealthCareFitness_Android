package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hcmute.edu.vn.R;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CompletedExerciseAdapter extends RecyclerView.Adapter<CompletedExerciseAdapter.ViewHolder> {
    private ArrayList<String> names;
    private long[] durations;

    public CompletedExerciseAdapter(ArrayList<String> names, long[] durations) {
        this.names = names;
        this.durations = durations;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_completed_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvName.setText(names.get(position));

        // Format thời gian ms thành dạng MM:SS
        long timeInMillis = durations[position];
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) - TimeUnit.MINUTES.toSeconds(minutes);
        holder.tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    @Override
    public int getItemCount() {
        return names.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemExName);
            tvTime = itemView.findViewById(R.id.tvItemExTime);
        }
    }
}