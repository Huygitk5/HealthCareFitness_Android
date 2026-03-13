package com.hcmute.edu.vn.activity;

import com.google.gson.annotations.SerializedName;

public class SignInResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() { return accessToken; }
}