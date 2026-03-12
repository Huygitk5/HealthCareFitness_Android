package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class MealRecommendedFood {
    @SerializedName("food")
    private Food food;
    @SerializedName("recommended_grams")
    private Double recommendedGrams;

    public MealRecommendedFood(Food food, Double recommendedGrams) {
        this.food = food;
        this.recommendedGrams = recommendedGrams;
    }

    public Food getFood() {
        return food;
    }

    public void setFood(Food food) {
        this.food = food;
    }

    public Double getRecommendedGrams() {
        return recommendedGrams;
    }

    public void setRecommendedGrams(Double recommendedGrams) {
        this.recommendedGrams = recommendedGrams;
    }
}
