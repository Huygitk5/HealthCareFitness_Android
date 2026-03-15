package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class Food {
    @SerializedName("id")
    private String id;
    @SerializedName("name")
    private String name;
    @SerializedName("category_id")
    private Integer categoryId;
    @SerializedName("serving_size")
    private String servingSize;
    @SerializedName("calories")
    private Double calories;
    @SerializedName("protein_g")
    private Double proteinG;
    @SerializedName("carb_g")
    private Double carbG;
    @SerializedName("fiber_g")
    private Double fiberG;
    @SerializedName("fat_g")
    private Double fatG;
    @SerializedName("image_url")
    private String imageUrl;

    public Food(String id, String name, Integer categoryId, String servingSize, Double calories, Double proteinG, Double carbG, Double fiberG, Double fatG) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.servingSize = servingSize;
        this.calories = calories;
        this.proteinG = proteinG;
        this.carbG = carbG;
        this.fiberG = fiberG;
        this.fatG = fatG;
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

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getServingSize() {
        return servingSize;
    }

    public void setServingSize(String servingSize) {
        this.servingSize = servingSize;
    }

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getProteinG() {
        return proteinG;
    }

    public void setProteinG(Double proteinG) {
        this.proteinG = proteinG;
    }

    public Double getCarbG() {
        return carbG;
    }

    public void setCarbG(Double carbG) {
        this.carbG = carbG;
    }

    public Double getFiberG() {
        return fiberG;
    }

    public void setFiberG(Double fiberG) {
        this.fiberG = fiberG;
    }

    public Double getFatG() {
        return fatG;
    }

    public void setFatG(Double fatG) {
        this.fatG = fatG;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
