package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserDailyMeal {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("date")
    private String date;

    @SerializedName("meal_type")
    private String mealType;

    @SerializedName("food_id")
    private String foodId;

    // Số phần ăn (Mặc định là 1.0)
    @SerializedName("quantity_multiplier")
    private double quantityMultiplier = 1.0;

    @SerializedName("logged_at")
    private String loggedAt;

    // Nơi chứa thông tin chi tiết của món ăn (Khi dùng JOIN: select=*,foods(*))
    @SerializedName("foods")
    private Food food;

    public UserDailyMeal() {}

    // Constructor dùng khi thêm món lên Database
    public UserDailyMeal(String userId, String date, String mealType, String foodId, double quantityMultiplier) {
        this.userId = userId;
        this.date = date;
        this.mealType = mealType;
        this.foodId = foodId;
        this.quantityMultiplier = quantityMultiplier;
    }

    // --- Getter & Setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public String getDate() { return date; }
    public String getMealType() { return mealType; }
    public String getFoodId() { return foodId; }

    public double getQuantityMultiplier() { return quantityMultiplier; }
    public void setQuantityMultiplier(double quantityMultiplier) { this.quantityMultiplier = quantityMultiplier; }

    public String getLoggedAt() { return loggedAt; }
    public Food getFood() { return food; }
    public void setFood(Food food) { this.food = food; }
}