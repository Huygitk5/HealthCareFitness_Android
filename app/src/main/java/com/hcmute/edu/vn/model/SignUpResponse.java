package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class SignUpResponse {
    @SerializedName("user")
    private AuthUser user;

    public AuthUser getUser() { return user; }
}