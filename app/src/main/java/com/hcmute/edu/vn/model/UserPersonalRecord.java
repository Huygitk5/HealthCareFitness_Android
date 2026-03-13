package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserPersonalRecord {
    @SerializedName("id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("exercise_id")
    private String exerciseId;
    @SerializedName("max_weight")
    private Double maxWeight;
    @SerializedName("achieved_at")
    private String achievedAt;

    public UserPersonalRecord(String id, String achievedAt, Double maxWeight, String exerciseId, String userId) {
        this.id = id;
        this.achievedAt = achievedAt;
        this.maxWeight = maxWeight;
        this.exerciseId = exerciseId;
        this.userId = userId;
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

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public Double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public String getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(String achievedAt) {
        this.achievedAt = achievedAt;
    }
}
