package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class Ingredient {
    @SerializedName("id")
    private Integer id;

    @SerializedName("name")
    private String name;

    // Có thể thêm Macro của riêng nguyên liệu nếu cần

    public Ingredient(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
    public String getName() { return name; }
}