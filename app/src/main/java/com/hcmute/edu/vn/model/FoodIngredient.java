package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class FoodIngredient {

    // ĐÂY RỒI! SỐ LƯỢNG NẰM Ở ĐÂY NÈ:
    @SerializedName("quantity")
    private String quantity; // Ví dụ: "100g", "2 muỗng", "1 vắt"

    // Thằng này sẽ "ôm" luôn cái chi tiết của nguyên liệu
    @SerializedName("ingredients") // Phải khớp với alias bảng nguyên liệu trên Supabase
    private Ingredient ingredient;

    public FoodIngredient(String quantity, Ingredient ingredient) {
        this.quantity = quantity;
        this.ingredient = ingredient;
    }

    public String getQuantity() { return quantity; }
    public Ingredient getIngredient() { return ingredient; }
}