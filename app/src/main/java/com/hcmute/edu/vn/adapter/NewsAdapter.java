package com.hcmute.edu.vn.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.News;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<News> newsList;

    public NewsAdapter(List<News> newsList) {
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        News news = newsList.get(position);
        holder.tvTitle.setText(news.getTitle());

        // Load ảnh bằng Glide
        Glide.with(holder.itemView.getContext())
                .load(news.getImage())
                .centerCrop()
                .into(holder.ivImage);

        // Sự kiện click mở link bài báo
        holder.itemView.setOnClickListener(v -> {
            if (news.getUrl() != null && !news.getUrl().isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(news.getUrl()));
                holder.itemView.getContext().startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return newsList != null ? newsList.size() : 0;
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivNewsImage);
            tvTitle = itemView.findViewById(R.id.tvNewsTitle);
        }
    }
}