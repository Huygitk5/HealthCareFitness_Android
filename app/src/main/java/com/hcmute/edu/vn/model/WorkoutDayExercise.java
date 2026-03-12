package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class WorkoutDayExercise {
    @SerializedName("exercise")
    private Exercise exercise;
    @SerializedName("sets")
    private Integer sets;
    @SerializedName("reps")
    private String reps;
    @SerializedName("rest_time_seconds")
    private Integer restTimeSeconds;

    public WorkoutDayExercise(Exercise exercise, Integer sets, String reps, Integer restTimeSeconds) {
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.restTimeSeconds = restTimeSeconds;
    }

    public Integer getSets() {
        return sets;
    }

    public void setSets(Integer sets) {
        this.sets = sets;
    }

    public String getReps() {
        return reps;
    }

    public void setReps(String reps) {
        this.reps = reps;
    }

    public Integer getRestTimeSeconds() {
        return restTimeSeconds;
    }

    public void setRestTimeSeconds(Integer restTimeSeconds) {
        this.restTimeSeconds = restTimeSeconds;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }
}

