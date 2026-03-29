package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class UserDailyWorkout implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("date")
    private String date;

    @SerializedName("exercise_id")
    private String exerciseId;

    @SerializedName("sets")
    private Integer sets;

    @SerializedName("reps")
    private String reps;

    @SerializedName("rest_time_seconds")
    private Integer restTimeSeconds;

    @SerializedName("exercise_order")
    private Integer exerciseOrder;

    @SerializedName("plan_id")
    private String planId;

    @SerializedName("day_id")
    private String dayId;

    // Chứa thông tin chi tiết bài tập khi truy vấn JOIN (select=*,exercises(*))
    @SerializedName("exercises")
    private Exercise exercise;

    public UserDailyWorkout() {}

    public UserDailyWorkout(String userId, String date, String exerciseId, Integer sets, String reps, Integer restTimeSeconds, Integer exerciseOrder, String planId) {
        this.userId = userId;
        this.date = date;
        this.exerciseId = exerciseId;
        this.sets = sets;
        this.reps = reps;
        this.restTimeSeconds = restTimeSeconds;
        this.exerciseOrder = exerciseOrder;
        this.planId = planId;
    }

    public UserDailyWorkout(String userId, String date, String exerciseId, Integer sets, String reps, Integer restTimeSeconds, Integer exerciseOrder, String planId, String dayId) {
        this(userId, date, exerciseId, sets, reps, restTimeSeconds, exerciseOrder, planId);
        this.dayId = dayId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getExerciseId() { return exerciseId; }
    public void setExerciseId(String exerciseId) { this.exerciseId = exerciseId; }

    public Integer getSets() { return sets; }
    public void setSets(Integer sets) { this.sets = sets; }

    public String getReps() { return reps; }
    public void setReps(String reps) { this.reps = reps; }

    public Integer getRestTimeSeconds() { return restTimeSeconds; }
    public void setRestTimeSeconds(Integer restTimeSeconds) { this.restTimeSeconds = restTimeSeconds; }

    public Integer getExerciseOrder() { return exerciseOrder; }
    public void setExerciseOrder(Integer exerciseOrder) { this.exerciseOrder = exerciseOrder; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getDayId() { return dayId; }
    public void setDayId(String dayId) { this.dayId = dayId; }

    public Exercise getExercise() { return exercise; }
    public void setExercise(Exercise exercise) { this.exercise = exercise; }
}
