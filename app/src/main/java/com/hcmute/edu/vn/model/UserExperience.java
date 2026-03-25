package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserExperience {

    @SerializedName("id")
    private Integer id;

    @SerializedName("name")
    private String name;

    public UserExperience(Integer id, String name) {
        this.id = id;
        this.name =name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
