package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class User {
    @SerializedName("id")
    private String id;
    @SerializedName("username")
    private String username;
    @SerializedName("name")
    private String name;
    @SerializedName("date_of_birth")
    private String dateOfBirth;
    @SerializedName("gender")
    private String gender;
    @SerializedName("height")
    private Double height;
    @SerializedName("weight")
    private Double weight;
    @SerializedName("fitness_goal_id")
    private Integer fitnessGoalId;
    @SerializedName("created_at")
    private String createdAt;
    @SerializedName("medical_conditions")
    private List<MedicalCondition> medicalConditions;

    public User(String id, String username, String name, String dateOfBirth, String gender, Double height, Double weight, Integer fitnessGoalId, String createdAt, List<MedicalCondition> medicalConditions) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.fitnessGoalId = fitnessGoalId;
        this.createdAt = createdAt;
        this.medicalConditions = medicalConditions;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getFitnessGoalId() {
        return fitnessGoalId;
    }

    public void setFitnessGoalId(Integer fitnessGoalId) {
        this.fitnessGoalId = fitnessGoalId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public List<MedicalCondition> getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(List<MedicalCondition> medicalConditions) {
        this.medicalConditions = medicalConditions;
    }
}
