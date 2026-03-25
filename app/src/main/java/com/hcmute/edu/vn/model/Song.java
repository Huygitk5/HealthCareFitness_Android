package com.hcmute.edu.vn.model;

/**
 * Model đại diện cho một bài hát trong danh sách phát nhạc nền tập luyện.
 *
 * @param id        ID duy nhất của bài hát
 * @param title     Tên bài hát hiển thị trên UI
 * @param artist    Tên nghệ sĩ
 * @param rawResId  Raw resource ID (R.raw.ten_bai_hat) — dùng cho mock data local
 * @param coverResId  Drawable resource ID ảnh bìa (R.drawable.*). Gán 0 để dùng ảnh mặc định.
 */
public class Song {

    private int id;
    private String title;
    private String artist;
    private int rawResId;      // ví dụ: R.raw.fresh_day
    private int coverResId;   // ví dụ: R.drawable.cover_fresh_day; 0 = dùng placeholder

    public Song(int id, String title, String artist, int rawResId, int coverResId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.rawResId = rawResId;
        this.coverResId = coverResId;
    }

    // ---- Getters ----

    public int getId() { return id; }

    public String getTitle() { return title; }

    public String getArtist() { return artist; }

    /** Raw resource ID dùng để khởi tạo MediaPlayer.create(context, rawResId) */
    public int getRawResId() { return rawResId; }

    /** Drawable resource ID ảnh bìa bài hát. 0 = dùng ảnh placeholder mặc định. */
    public int getCoverResId() { return coverResId; }
}
