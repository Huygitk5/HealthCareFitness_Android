package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class UserWorkoutSession {
    @SerializedName("id")
    private String id;
    @SerializedName("user_id")
    private String userId;
    @SerializedName("plan_id")
    private String planId;
    @SerializedName("day_id")
    private String dayId;
    @SerializedName("started_at")
    private String startedAt;
    @SerializedName("finished_at")
    private String finishedAt;
    @SerializedName("logs")
    private List<UserWorkoutExerciseLog> logs;

    public UserWorkoutSession(String id, String userId, String planId, String dayId, String startedAt, String finishedAt, List<UserWorkoutExerciseLog> logs) {
        this.id = id;
        this.userId = userId;
        this.planId = planId;
        this.dayId = dayId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.logs = logs;
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

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getDayId() {
        return dayId;
    }

    public void setDayId(String dayId) {
        this.dayId = dayId;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(String startedAt) {
        this.startedAt = startedAt;
    }

    public String getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(String finishedAt) {
        this.finishedAt = finishedAt;
    }

    public List<UserWorkoutExerciseLog> getLogs() {
        return logs;
    }

    public void setLogs(List<UserWorkoutExerciseLog> logs) {
        this.logs = logs;
    }
}

