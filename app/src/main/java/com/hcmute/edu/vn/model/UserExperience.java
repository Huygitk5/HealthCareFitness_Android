package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserExperience {

    @SerializedName("id")
    private Integer id;

    @SerializedName("user_type")
    private String userType;

    public UserExperience(Integer id, String userType) {
        this.id = id;
        this.userType = userType;
    }

    public Integer getId() {
        return id;
    }

    public String getUserType() {
        return userType;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }
}
