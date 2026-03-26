package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body khi INSERT vào bảng `user_songs` trên Supabase.
 * Liên kết một user với một bài hát họ đã upload.
 */
public class UserSongInsert {

    @SerializedName("user_id")
    private String userId;

    @SerializedName("song_id")
    private long songId;

    public UserSongInsert(String userId, long songId) {
        this.userId = userId;
        this.songId = songId;
    }

    public String getUserId() { return userId; }
    public long getSongId()   { return songId; }
}