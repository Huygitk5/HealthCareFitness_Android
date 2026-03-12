package com.hcmute.edu.vn.login;
import com.google.gson.annotations.SerializedName;

public class SignInRequest {
    @SerializedName("email")
    private String email;
    @SerializedName("password")
    private String password;

    public SignInRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}