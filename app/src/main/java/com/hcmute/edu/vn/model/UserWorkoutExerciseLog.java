package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserWorkoutExerciseLog {
    @SerializedName("id")
    private String id;
    @SerializedName("session_id")
    private String sessionId;
    @SerializedName("exercise_id")
    private String exerciseId;
    @SerializedName("set_number")
    private Integer setNumber;
    @SerializedName("reps")
    private Integer reps;
    @SerializedName("weight")
    private Double weight;
    @SerializedName("logged_at")
    private String loggedAt;

    public UserWorkoutExerciseLog(String id, String sessionId, String exerciseId, Integer setNumber, Integer reps, Double weight, String loggedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.setNumber = setNumber;
        this.reps = reps;
        this.weight = weight;
        this.loggedAt = loggedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(Integer setNumber) {
        this.setNumber = setNumber;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public String getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(String loggedAt) {
        this.loggedAt = loggedAt;
    }
}

