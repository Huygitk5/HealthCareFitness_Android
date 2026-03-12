package com.hcmute.edu.vn.login;
import com.google.gson.annotations.SerializedName;

public class SignInResponse {
    @SerializedName("access_token")
    private String accessToken;

    public String getAccessToken() { return accessToken; }
}