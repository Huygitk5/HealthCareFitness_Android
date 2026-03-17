package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;
import java.util.List; // Thêm import List

public class User {
    @SerializedName("id")
    private String id;

    @SerializedName("username")
    private String username;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("date_of_birth")
    private String dateOfBirth; // Trả về dạng YYYY-MM-DD

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

    @SerializedName("target")
    private String target;


    // Thêm biến để chứa danh sách Bệnh / Dị ứng của User
    @SerializedName("user_medical_conditions")
    private List<UserMedicalCondition> userMedicalConditions;

    // --- CONSTRUCTOR ---
    public User() {}

    // Dành cho lúc cập nhật Profile
    public User(String name, String dateOfBirth, String gender, Double height, Double weight) {
        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
    }

    // Constructor dùng để Đăng ký (Chỉ cần truyền username và name, các cột khác để null)
    public User(String username, String name) {
        this.username = username;
        this.name = name;
    }

    public User(String id, String username, String name) {
        this.id = id;
        this.username = username;
        this.name = name;
    }

    // Constructor dùng lúc đăng ký
    public User(String id, String username, String email, String name) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.name = name;
    }

    // --- GETTER & SETTER ---
    public String getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // GETTER & SETTER CHO DANH SÁCH BỆNH (Vừa thêm)
    public List<UserMedicalCondition> getUserMedicalConditions() {
        return userMedicalConditions;
    }
    public void setUserMedicalConditions(List<UserMedicalCondition> userMedicalConditions) {
        this.userMedicalConditions = userMedicalConditions;
    }
}