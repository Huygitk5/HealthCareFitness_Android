package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MedicalCondition {
    @SerializedName("id")
    private Integer id;
    @SerializedName("name")
    private String name;
    @SerializedName("type")
    private String type; // 'allergy', 'disease', 'history'
    @SerializedName("restricted_foods")
    private List<Food> restrictedFoods;

    public MedicalCondition(Integer id, String name, String type, List<Food> restrictedFoods) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.restrictedFoods = restrictedFoods;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Food> getRestrictedFoods() {
        return restrictedFoods;
    }

    public void setRestrictedFoods(List<Food> restrictedFoods) {
        this.restrictedFoods = restrictedFoods;
    }
}
