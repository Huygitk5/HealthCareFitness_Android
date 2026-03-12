package com.hcmute.edu.vn;

import com.hcmute.edu.vn.home.model.User;
import com.hcmute.edu.vn.login.SignUpRequest;
import com.hcmute.edu.vn.login.SignUpResponse;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApiService {

    // API Đăng ký tài khoản bảo mật (Chọc vào hệ thống Auth)
    @POST("/auth/v1/signup")
    Call<SignUpResponse> signUpAuth(@Body SignUpRequest request);

    // API Lưu hồ sơ User (Chọc vào Database public.users)
    @POST("users")
    Call<Void> registerUser(@Body User user);

    // Kiểm tra Đăng nhập (Tìm user có khớp username VÀ password không)
    @GET("users")
    Call<List<User>> loginUser(
            @Query("username") String eqUsername, // VD: "eq.tuan123"
            @Query("password") String eqPassword, // VD: "eq.pass123"
            @Query("select") String selectAll
    );

    // Lấy thông tin User theo Username
    @GET("users")
    Call<List<User>> getUserDetails(
            @Query("username") String eqUsername,
            @Query("select") String selectAll
    );

    // Cập nhật Profile
    @PATCH("users")
    Call<Void> updateUserProfile(
            @Query("username") String eqUsername,
            @Body User user
    );
}