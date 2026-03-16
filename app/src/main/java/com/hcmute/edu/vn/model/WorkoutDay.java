package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class WorkoutDay {
    @SerializedName("id")
    private String id;
    @SerializedName("plan_id")
    private String planId;
    @SerializedName("name")
    private String name;
    @SerializedName("day_order")
    private Integer dayOrder;
    @SerializedName("workout_day_exercises")
    private List<WorkoutDayExercise> exercises;

    public WorkoutDay(String id, String planId, String name, Integer dayOrder, List<WorkoutDayExercise> exercises) {
        this.id = id;
        this.planId = planId;
        this.name = name;
        this.dayOrder = dayOrder;
        this.exercises = exercises;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getDayOrder() {
        return dayOrder;
    }

    public void setDayOrder(Integer dayOrder) {
        this.dayOrder = dayOrder;
    }

    public List<WorkoutDayExercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<WorkoutDayExercise> exercises) {
        this.exercises = exercises;
    }
}
