package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class AuthUser {
    @SerializedName("id")
    private String id;

    public String getId() { return id; }
}