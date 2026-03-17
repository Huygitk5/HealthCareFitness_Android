package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class ConditionRestrictedIngredient {

    @SerializedName("condition_id")
    private String medicalConditionId;

    @SerializedName("ingredient_id")
    private String ingredientId;

    public ConditionRestrictedIngredient() {}

    public String getMedicalConditionId() { return medicalConditionId; }
    public String getIngredientId() { return ingredientId; }
}