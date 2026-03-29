package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Exercise implements Serializable {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("muscle_group_id")

    private Integer muscleGroupId;
    @SerializedName("difficulty_level_id")
    private Integer difficultyLevelId;
    @SerializedName("base_recommended_sets")
    private Integer baseRecommendedSets;
    @SerializedName("base_recommended_reps")
    private String baseRecommendedReps;
    @SerializedName("video_url")
    private String videoUrl;
    @SerializedName("image_url")
    private String imageUrl;
    @SerializedName("equipments")
    private List<Equipment> equipments;
    @SerializedName("time_per_rep")
    private Integer timePerRep;

    public Exercise(String id, String name, String description, Integer muscleGroupId, Integer difficultyLevelId, Integer baseRecommendedSets, String baseRecommendedReps, String videoUrl, String imageUrl, List<Equipment> equipments) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.muscleGroupId = muscleGroupId;
        this.difficultyLevelId = difficultyLevelId;
        this.baseRecommendedSets = baseRecommendedSets;
        this.baseRecommendedReps = baseRecommendedReps;
        this.videoUrl = videoUrl;
        this.imageUrl = imageUrl;
        this.equipments = equipments;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getMuscleGroupId() {
        return muscleGroupId;
    }

    public void setMuscleGroupId(Integer muscleGroupId) {
        this.muscleGroupId = muscleGroupId;
    }

    public Integer getDifficultyLevelId() {
        return difficultyLevelId;
    }

    public void setDifficultyLevelId(Integer difficultyLevelId) {
        this.difficultyLevelId = difficultyLevelId;
    }

    public Integer getBaseRecommendedSets() {
        return baseRecommendedSets;
    }

    public void setBaseRecommendedSets(Integer baseRecommendedSets) {
        this.baseRecommendedSets = baseRecommendedSets;
    }

    public String getBaseRecommendedReps() {
        return baseRecommendedReps;
    }

    public void setBaseRecommendedReps(String baseRecommendedReps) {
        this.baseRecommendedReps = baseRecommendedReps;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Equipment> getEquipments() {
        return equipments;
    }

    public void setEquipments(List<Equipment> equipments) {
        this.equipments = equipments;
    }

    public Integer getTimePerRep() {
        return timePerRep;
    }

    public void setTimePerRep(Integer timePerRep) {
        this.timePerRep = timePerRep;
    }
}
