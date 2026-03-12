package com.hcmute.edu.vn.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;

import java.util.ArrayList;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    private Context context;
    private ArrayList<Exercise> activityList;

    public ActivityAdapter(Context context, ArrayList<Exercise> activityList) {
        this.context = context;
        this.activityList = activityList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise activity = activityList.get(position);

        holder.tvActivityName.setText(activity.getName());
        holder.imgActivity.setImageResource(activity.getImage());
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgActivity;
        TextView tvActivityName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgActivity = itemView.findViewById(R.id.imgActivity);
            tvActivityName = itemView.findViewById(R.id.tvActivityName);
        }
    }
}