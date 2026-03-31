package com.hcmute.edu.vn.model;

public class ExerciseHistoryItem {

    private final String imageUrl;
    private final String name;
    private final String repsText;
    private final double burnedKcal;

    public ExerciseHistoryItem(String imageUrl, String name, String repsText, double burnedKcal) {
        this.imageUrl = imageUrl;
        this.name = name;
        this.repsText = repsText;
        this.burnedKcal = burnedKcal;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getRepsText() {
        return repsText;
    }

    public double getBurnedKcal() {
        return burnedKcal;
    }
}
