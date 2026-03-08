package com.hcmute.edu.vn.home.model;

public class News {
    private String title;
    private String subtitle; // Thời gian đăng & Tác giả
    private int imageRes;

    public News(String title, String subtitle, int imageRes) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageRes = imageRes;
    }

    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public int getImageRes() { return imageRes; }
}