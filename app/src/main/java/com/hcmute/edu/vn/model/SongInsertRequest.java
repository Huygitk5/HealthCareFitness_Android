package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

/**
 * Request body khi INSERT một bài hát mới vào bảng `songs` trên Supabase.
 * Không bao gồm `id` vì Supabase tự tạo (BIGSERIAL).
 */
public class SongInsertRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("url")
    private String url;

    @SerializedName("is_local")
    private boolean isLocal;

    public SongInsertRequest(String title, String artist, String url, boolean isLocal) {
        this.title   = title;
        this.artist  = artist;
        this.url     = url;
        this.isLocal = isLocal;
    }

    public String getTitle()    { return title; }
    public String getArtist()   { return artist; }
    public String getUrl()      { return url; }
    public boolean isLocal()    { return isLocal; }
}