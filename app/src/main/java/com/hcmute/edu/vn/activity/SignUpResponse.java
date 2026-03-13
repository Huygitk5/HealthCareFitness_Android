package com.hcmute.edu.vn.activity;
import com.google.gson.annotations.SerializedName;
import com.hcmute.edu.vn.model.AuthUser;

public class SignUpResponse {
    @SerializedName("user")
    private AuthUser user;

    public AuthUser getUser() { return user; }
}