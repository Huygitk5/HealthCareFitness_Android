package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class MealType {
    @SerializedName("id")
    private Integer id;
    @SerializedName("name")
    private String name;

    public MealType(Integer id, String name) {
        this.id = id;
        this.name = name;
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
}