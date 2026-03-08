package com.hcmute.edu.vn.workout.model;

public class WorkoutDay {
    private String dayName;
    private String subTitle;
    private boolean isRestDay;

    public WorkoutDay(String dayName, String subTitle, boolean isRestDay) {
        this.dayName = dayName;
        this.subTitle = subTitle;
        this.isRestDay = isRestDay;
    }

    public String getDayName() { return dayName; }
    public String getSubTitle() { return subTitle; }
    public boolean isRestDay() { return isRestDay; }
}