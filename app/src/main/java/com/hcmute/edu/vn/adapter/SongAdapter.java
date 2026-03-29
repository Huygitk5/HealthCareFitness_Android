package com.hcmute.edu.vn.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Song;

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    /** Interface callback khi người dùng chọn một bài hát. */
    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    private final List<Song> songs;
    private int currentPlayingIndex = -1;   // index bài đang phát; -1 = chưa có
    private final OnSongClickListener listener;

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        // Set ảnh bìa nếu có, dùng placeholder nếu không
        if (song.getCoverResId() != 0) {
            holder.ivCover.setImageResource(song.getCoverResId());
        } else {
            holder.ivCover.setImageResource(R.drawable.workout_1);
        }

        // Hiển thị icon nếu đây là bài đang phát
        boolean isPlaying = (position == currentPlayingIndex);
        holder.ivPlayingIndicator.setVisibility(isPlaying ? View.VISIBLE : View.INVISIBLE);
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (listener != null && pos != RecyclerView.NO_POSITION) listener.onSongClick(pos);
        });
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    /**
     * Cập nhật bài đang phát và làm mới UI các dòng liên quan.
     * Gọi từ MusicBottomSheetFragment mỗi khi bài hát thay đổi.
     */
    public void setCurrentPlayingIndex(int newIndex) {
        int oldIndex = currentPlayingIndex;
        currentPlayingIndex = newIndex;
        if (oldIndex >= 0) notifyItemChanged(oldIndex);
        if (newIndex >= 0) notifyItemChanged(newIndex);
    }

    public int getCurrentPlayingIndex() {
        return currentPlayingIndex;
    }

    // ---- ViewHolder ----

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCover;
        TextView tvTitle;
        TextView tvArtist;
        ImageView ivPlayingIndicator;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivSongCover);
            tvTitle = itemView.findViewById(R.id.tvSongTitle);
            tvArtist = itemView.findViewById(R.id.tvSongArtist);
            ivPlayingIndicator = itemView.findViewById(R.id.ivPlayingIndicator);
        }
    }
}
