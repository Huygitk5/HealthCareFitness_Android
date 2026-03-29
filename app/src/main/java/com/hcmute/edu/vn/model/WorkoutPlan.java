package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WorkoutPlan {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("difficulty_level_id")
    private Integer difficultyLevelId;
    @SerializedName("workout_days")
    private List<WorkoutDay> days;

    public WorkoutPlan() {}

    public WorkoutPlan(String id, String name, Integer difficultyLevelId, List<WorkoutDay> days) {
        this.id = id;
        this.name = name;
        this.difficultyLevelId = difficultyLevelId;
        this.days = days;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDifficultyLevelId() {
        return difficultyLevelId;
    }

    public void setDifficultyLevelId(Integer difficultyLevelId) {
        this.difficultyLevelId = difficultyLevelId;
    }

    public List<WorkoutDay> getDays() {
        return days;
    }

    public void setDays(List<WorkoutDay> days) {
        this.days = days;
    }
}
