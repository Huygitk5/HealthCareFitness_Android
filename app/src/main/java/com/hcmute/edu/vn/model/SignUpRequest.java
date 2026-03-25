package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class SignUpRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    public SignUpRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}