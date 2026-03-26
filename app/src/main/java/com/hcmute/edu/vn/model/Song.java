package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class Song {

    // ── Dùng cho nhạc mặc định (local raw resource) ──────────────────────────
    private int id;
    private int rawResId;
    private int coverResId;

    // ── Dùng cho nhạc từ Supabase (map từ JSON trả về) ───────────────────────
    @SerializedName("id")
    private long dbId;          // id BIGSERIAL trong bảng songs

    @SerializedName("title")
    private String title;

    @SerializedName("artist")
    private String artist;

    @SerializedName("url")
    private String url;         // Public URL trên Supabase Storage

    @SerializedName("is_local")
    private boolean isLocal;    // true = nhạc hệ thống, false = user upload

    // ── Constructor cho nhạc mặc định (raw resource) ─────────────────────────
    public Song(int id, String title, String artist, int rawResId, int coverResId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.rawResId = rawResId;
        this.coverResId = coverResId;
        this.url       = null;
        this.isLocal   = true;
    }

    // ── Constructor cho nhạc từ Supabase (URL) ───────────────────────────────
    public Song(long dbId, String title, String artist, String url) {
        this.dbId      = dbId;
        this.id        = (int) dbId;
        this.title     = title;
        this.artist    = artist;
        this.url       = url;
        this.rawResId  = 0;
        this.coverResId = 0;
        this.isLocal   = false;
    }

    // ── Constructor rỗng cho Gson deserialize ────────────────────────────────
    public Song() {}

    // ── Helper: Kiểm tra nguồn nhạc ──────────────────────────────────────────

    /** true nếu bài hát phát từ URL (Supabase hoặc file local path) */
    public boolean isUrlBased() {
        return url != null && !url.isEmpty();
    }

    /** true nếu bài hát phát từ raw resource đóng gói trong app */
    public boolean isRawResource() {
        return rawResId != 0;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public int getId()          { return id; }
    public long getDbId()       { return dbId; }
    public String getTitle()    { return title != null ? title : ""; }
    public String getArtist()   { return artist != null ? artist : ""; }
    public int getRawResId()    { return rawResId; }
    public int getCoverResId()  { return coverResId; }
    public String getUrl()      { return url; }
    public boolean isLocal()    { return isLocal; }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setId(int id)               { this.id = id; }
    public void setDbId(long dbId)          { this.dbId = dbId; }
    public void setTitle(String title)      { this.title = title; }
    public void setArtist(String artist)    { this.artist = artist; }
    public void setUrl(String url)          { this.url = url; }
    public void setLocal(boolean local)     { isLocal = local; }
    public void setRawResId(int rawResId)   { this.rawResId = rawResId; }
    public void setCoverResId(int id)       { this.coverResId = id; }
}
