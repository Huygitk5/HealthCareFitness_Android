package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class BmiLog {
    @SerializedName("id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("weight")
    private Double weight;
    @SerializedName("height")
    private Double height;
    @SerializedName("bmi_value")
    private Double bmiValue;
    @SerializedName("recorded_at")
    private String recordedAt;

    public BmiLog(String id, String userId, Double weight, Double height, Double bmiValue, String recordedAt) {
        this.id = id;
        this.userId = userId;
        this.weight = weight;
        this.height = height;
        this.bmiValue = bmiValue;
        this.recordedAt = recordedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getBmiValue() {
        return bmiValue;
    }

    public void setBmiValue(Double bmiValue) {
        this.bmiValue = bmiValue;
    }

    public String getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(String recordedAt) {
        this.recordedAt = recordedAt;
    }
}

