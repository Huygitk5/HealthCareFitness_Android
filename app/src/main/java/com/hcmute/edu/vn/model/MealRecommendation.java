package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MealRecommendation {
    @SerializedName("id")
    private String id;
    @SerializedName("fitness_goal_id")
    private Integer fitnessGoalId;
    @SerializedName("meal_type_id")
    private Integer mealTypeId;
    @SerializedName("recommended_foods")
    private List<MealRecommendedFood> recommendedFoods;

    public MealRecommendation(String id, Integer fitnessGoalId, Integer mealTypeId, List<MealRecommendedFood> recommendedFoods) {
        this.id = id;
        this.fitnessGoalId = fitnessGoalId;
        this.mealTypeId = mealTypeId;
        this.recommendedFoods = recommendedFoods;
    }
}
