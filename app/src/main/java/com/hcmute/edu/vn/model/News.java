package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class News implements Serializable {
    @SerializedName("id")
    private Integer id;

    @SerializedName("title")
    private String title;

    @SerializedName("image")
    private String image;

    @SerializedName("url")
    private String url;

    @SerializedName("condition_id")
    private Integer conditionId;

    public Integer getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getUrl() { return url; }
    public Integer getConditionId() { return conditionId; }
}