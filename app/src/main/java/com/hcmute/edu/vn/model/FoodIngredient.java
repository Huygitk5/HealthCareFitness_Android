package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class FoodIngredient {

    @SerializedName("food_id")
    private String foodId;

    @SerializedName("ingredient_id")
    private String ingredientId;

    @SerializedName("quantity")
    private double quantity;

    @SerializedName("unit")
    private String unit;

    // Chứa thông tin nguyên liệu khi JOIN bảng
    @SerializedName("ingredients")
    private Ingredient ingredient;

    public FoodIngredient() {}

    public String getFoodId() { return foodId; }
    public String getIngredientId() { return ingredientId; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Ingredient getIngredient() { return ingredient; }

    // Hàm tiện ích: Trả về chuỗi đẹp. VD: "100 g", "2 muỗng"
    public String getFormattedQuantity() {
        String qtyString = (quantity == Math.floor(quantity))
                ? String.valueOf((int) quantity)
                : String.valueOf(quantity);
        return qtyString + " " + (unit != null ? unit : "g");
    }
}