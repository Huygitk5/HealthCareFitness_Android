package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ExerciseType implements Serializable {

    @SerializedName("id")
    private Integer id;

    @SerializedName("type")
    private String type; // GYM, CARDIO, HIIT

    // Constructor rỗng cho Gson
    public ExerciseType() {
    }

    // Constructor đầy đủ
    public ExerciseType(Integer id, String type) {
        this.id = id;
        this.type = type;
    }

    // Getter và Setter
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}