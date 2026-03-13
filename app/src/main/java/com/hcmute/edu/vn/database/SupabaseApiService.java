package com.hcmute.edu.vn.database;

import com.hcmute.edu.vn.activity.SignInRequest;
import com.hcmute.edu.vn.activity.SignInResponse;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.activity.SignUpRequest;
import com.hcmute.edu.vn.activity.SignUpResponse;
import com.hcmute.edu.vn.model.UserPersonalRecord;
import com.hcmute.edu.vn.model.UserWorkoutExerciseLog;
import com.hcmute.edu.vn.model.UserWorkoutSession;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseApiService {

    // =================================================================================
    // REGISTER
    // =================================================================================

    // Đăng ký tài khoản bảo mật
    @POST("/auth/v1/signup")
    Call<SignUpResponse> signUpAuth(@Body SignUpRequest request);

    // Lưu hồ sơ User
    @POST("users")
    Call<Void> registerUser(@Body User user);

    // =================================================================================
    // LOGIN
    // =================================================================================

    // Đăng nhập bảo mật qua Supabase Auth
    @POST("/auth/v1/token?grant_type=password")
    Call<SignInResponse> signInAuth(@Body SignInRequest request);

    // Kiểm tra Đăng nhập (Tìm user có khớp username VÀ password không)
    @GET("users")
    Call<List<User>> loginUser(
            @Query("username") String eqUsername, // VD: "eq.tuan123"
            @Query("password") String eqPassword, // VD: "eq.pass123"
            @Query("select") String selectAll
    );


    // =================================================================================
    // USER PROFILE
    // =================================================================================

    // Dùng để tìm user dựa theo username (Lấy email ra để đăng nhập, hoặc check trùng lúc đăng ký)
    @GET("users")
    Call<List<User>> getUserByUsername(
            @Query("username") String eqUsername,
            @Query("select") String selectAll
    );

    @PATCH("users")
    Call<Void> updateUserProfile(
            @Query("username") String eqUsername,
            @Body User user
    );

    // =================================================================================
    // WORKOUT PLANS & EXERCISES
    // =================================================================================

    // Lấy danh sách tất cả các Gói tập (Workout Plans)
    @GET("workout_plans")
    Call<List<WorkoutPlan>> getAllWorkoutPlans(
            @Query("select") String select // Sẽ truyền chuỗi join bảng vào đây
    );

    // Lấy chi tiết 1 Gói tập theo ID (Bao gồm Ngày tập -> Bài tập -> Chi tiết bài tập)
    @GET("workout_plans")
    Call<List<WorkoutPlan>> getWorkoutPlanById(
            @Query("id") String eqId, // VD: "eq.plan-uuid-1234"
            @Query("select") String select
    );

    // Lấy danh sách toàn bộ Bài tập (Thư viện bài tập - Exercise Library)
    @GET("exercises")
    Call<List<Exercise>> getAllExercises(
            @Query("select") String select
    );

    // Lấy chi tiết 1 Bài tập cụ thể
    @GET("exercises")
    Call<List<Exercise>> getExerciseById(
            @Query("id") String eqId,
            @Query("select") String select
    );


    // =================================================================================
    // WORKOUT SESSIONS & LOGS
    // =================================================================================

    // Lưu một buổi tập mới của User (Khi user bấm "Hoàn thành buổi tập")
    @POST("user_workout_sessions")
    Call<Void> saveWorkoutSession(
            @Body UserWorkoutSession session
    );

    // Lưu chi tiết các bài tập trong buổi tập đó (Lưu nhiều bài cùng lúc - Batch Insert)
    @POST("user_workout_exercise_logs")
    Call<Void> saveWorkoutExerciseLogs(
            @Body List<UserWorkoutExerciseLog> logs
    );

    // Lấy lịch sử các buổi tập của 1 User
    @GET("user_workout_sessions")
    Call<List<UserWorkoutSession>> getUserWorkoutHistory(
            @Query("user_id") String eqUserId, // VD: "eq.user-uuid-1234"
            @Query("select") String select
    );


    // =================================================================================
    // PERSONAL RECORDS
    // =================================================================================

    // Lấy danh sách kỷ lục cá nhân (PR) của 1 User
    @GET("user_personal_records")
    Call<List<UserPersonalRecord>> getUserPersonalRecords(
            @Query("user_id") String eqUserId,
            @Query("select") String select
    );

    // Lưu kỷ lục cá nhân mới (POST)
    @POST("user_personal_records")
    Call<Void> savePersonalRecord(
            @Body UserPersonalRecord record
    );

    // Cập nhật kỷ lục cá nhân (PATCH) nếu user phá kỷ lục cũ
    @PATCH("user_personal_records")
    Call<Void> updatePersonalRecord(
            @Query("id") String eqRecordId,
            @Body UserPersonalRecord record
    );
}