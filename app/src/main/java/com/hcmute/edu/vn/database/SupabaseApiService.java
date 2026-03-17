package com.hcmute.edu.vn.database;

import com.hcmute.edu.vn.model.SignInRequest;
import com.hcmute.edu.vn.model.SignInResponse;
import com.hcmute.edu.vn.model.BmiLog;
import com.hcmute.edu.vn.model.Equipment;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.Food;
import com.hcmute.edu.vn.model.MealRecommendedFood;
import com.hcmute.edu.vn.model.MedicalCondition;
import com.hcmute.edu.vn.model.MuscleGroup;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.SignUpRequest;
import com.hcmute.edu.vn.model.SignUpResponse;
import com.hcmute.edu.vn.model.UserMedicalConditionInsert;
import com.hcmute.edu.vn.model.UserPersonalRecord;
import com.hcmute.edu.vn.model.UserWorkoutExerciseLog;
import com.hcmute.edu.vn.model.UserWorkoutSession;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.DELETE; // Nhớ import thư viện này ở trên cùng nhé
public interface SupabaseApiService {

    // =================================================================================
    // REGISTER & LOGIN (AUTH)
    // =================================================================================

    // Đăng ký tài khoản bảo mật
    @POST("/auth/v1/signup")
    Call<SignUpResponse> signUpAuth(@Body SignUpRequest request);

    // Lưu hồ sơ User lúc đăng ký
    @POST("users")
    Call<Void> registerUser(@Body User user);

    // Đăng nhập bảo mật qua Supabase Auth
    @POST("/auth/v1/token?grant_type=password")
    Call<SignInResponse> signInAuth(@Body SignInRequest request);


    // =================================================================================
    // USER PROFILE
    // =================================================================================

    // Tìm user dựa theo username (Lấy thông tin hiển thị, lấy email đăng nhập, check trùng)
    @GET("users")
    Call<List<User>> getUserByUsername(
            @Query("username") String eqUsername,
            @Query("select") String selectAll
    );

    // Cập nhật thông tin Profile
    @PATCH("users")
    Call<Void> updateUserProfile(
            @Query("username") String eqUsername,
            @Body User user
    );


    // =================================================================================
    // WORKOUT PLANS & EXERCISES (DỮ LIỆU GỐC)
    // =================================================================================

    // Lấy danh sách tất cả các Gói tập (Workout Plans)
    @GET("workout_plans")
    Call<List<WorkoutPlan>> getAllWorkoutPlans(
            @Query("select") String select
    );

    // Lọc Gói tập (Workout Plan) theo Độ khó (Difficulty Level)
    @GET("workout_plans")
    Call<List<WorkoutPlan>> getWorkoutPlansByDifficulty(
            @Query("difficulty_level_id") String eqDifficultyId,
            @Query("select") String select
    );

    // Lấy chi tiết 1 Gói tập theo ID (Bao gồm Ngày tập -> Bài tập -> Chi tiết bài tập)
    @GET("workout_plans")
    Call<List<WorkoutPlan>> getWorkoutPlanById(
            @Query("id") String eqId,
            @Query("select") String select
    );

    // Lọc chi tiết các bài tập cho 1 ngày cụ thể trong Gói tập
    @GET("workout_days")
    Call<List<WorkoutDay>> getExercisesForSpecificDay(
            @Query("id") String eqDayId,
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

    // LỌC BÀI TẬP LINH HOẠT (Theo Nhóm cơ, Dụng cụ...)
    @GET("exercises")
    Call<List<Exercise>> getFilteredExercises(
            @QueryMap Map<String, String> filters
    );


    // =================================================================================
    // WORKOUT SESSIONS & LOGS (NHẬT KÝ TẬP LUYỆN CỦA USER)
    // =================================================================================

    // Lấy lịch sử các buổi tập của 1 User
    @GET("user_workout_sessions")
    Call<List<UserWorkoutSession>> getUserWorkoutHistory(
            @Query("user_id") String eqUserId,
            @Query("select") String select
    );

    // Tìm buổi tập cho "Today plan"
    @GET("workout_days")
    Call<List<WorkoutDay>> getNextWorkoutDay(
            @Query("plan_id") String eqPlanId,
            @Query("day_order") String eqDayOrder,
            @Query("select") String select
    );


    // Bắt đầu một buổi tập mới của User
    @POST("user_workout_sessions")
    Call<Void> saveWorkoutSession(
            @Body UserWorkoutSession session
    );

    // Cập nhật kết thúc buổi tập (Chỉ ghi đè trường finished_at)
    @PATCH("user_workout_sessions")
    Call<Void> endWorkoutSession(
            @Query("id") String eqSessionId,
            @Body UserWorkoutSession updateData
    );

    // Lưu chi tiết các set bài tập trong buổi tập đó (Batch Insert)
    @POST("user_workout_exercise_logs")
    Call<Void> saveWorkoutExerciseLogs(
            @Body List<UserWorkoutExerciseLog> logs
    );


    // =================================================================================
    // PERSONAL RECORDS (KỶ LỤC CÁ NHÂN)
    // =================================================================================

    // Lấy danh sách kỷ lục cá nhân (PR) của 1 User
    @GET("user_personal_records")
    Call<List<UserPersonalRecord>> getUserPersonalRecords(
            @Query("user_id") String eqUserId,
            @Query("select") String select
    );

    // Lưu kỷ lục cá nhân mới
    @POST("user_personal_records")
    Call<Void> savePersonalRecord(
            @Body UserPersonalRecord record
    );

    // Cập nhật kỷ lục cá nhân nếu user phá kỷ lục cũ
    @PATCH("user_personal_records")
    Call<Void> updatePersonalRecord(
            @Query("id") String eqRecordId,
            @Body UserPersonalRecord record
    );

    // =================================================================================
    // BMI LOGS (THEO DÕI CHỈ SỐ CƠ THỂ)
    // =================================================================================

    // Lấy danh sách all BMI của 1 User
    @GET("bmi_logs")
    Call<List<BmiLog>> getUserBmiLogs(
            @Query("user_id") String eqUserId,      // VD: "eq.user-uuid-1234"
            @Query("select") String select,         // VD: "*"
            @Query("order") String orderBy          // VD: "recorded_at.desc" (Để xếp ngày mới nhất lên đầu)
    );

    // Lấy danh sách BMI theo khoảng thời gian (Từ ngày A -> Đến ngày B)
    @GET("bmi_logs")
    Call<List<BmiLog>> getUserBmiLogsByDateRange(
            @Query("user_id") String eqUserId,
            @Query("recorded_at") String gteDate,   // Lớn hơn hoặc bằng (Từ ngày) -> VD: "gte.2026-03-01T00:00:00"
            @Query("recorded_at") String lteDate,   // Nhỏ hơn hoặc bằng (Đến ngày) -> VD: "lte.2026-03-31T23:59:59"
            @Query("select") String select,
            @Query("order") String orderBy
    );

    // Lưu một bản ghi BMI mới (Khi user nhập cân nặng hôm nay)
    @POST("bmi_logs")
    Call<Void> saveBmiLog(
            @Body BmiLog bmiLog
    );

    // Cập nhật một bản ghi BMI (Trong trường hợp user nhập sai và muốn sửa lại)
    @PATCH("bmi_logs")
    Call<Void> updateBmiLog(
            @Query("id") String eqLogId,            // VD: "eq.log-uuid-1234"
            @Body BmiLog bmiLog                     // Object chứa cân nặng/chiều cao mới
    );

    // =================================================================================
    // DỤNG CỤ, NHÓM CƠ, ĐỘ KHÓ
    // =================================================================================

    // Lấy danh sách toàn bộ Dụng cụ tập (Equipments)
    @GET("equipment")
    Call<List<Equipment>> getAllEquipments(
            @Query("select") String select
    );

    // Lấy danh sách Nhóm cơ (Muscle Groups)
    @GET("muscle_groups")
    Call<List<MuscleGroup>> getMuscleGroups(
            @QueryMap Map<String, String> filters
    );

    // =================================================================================
    // FOODS & NUTRITION
    // =================================================================================

    // Lấy danh sách thức ăn theo Nhóm
    @GET("foods")
    Call<List<Food>> getFoodsByCategory(
            @Query("category_id") String eqCategoryId, // VD: "eq.1"
            @Query("select") String select
    );

    // Tìm kiếm thức ăn theo Tên (Có thể kết hợp lọc theo Nhóm)
    // - Tìm theo tên: map.put("name", "ilike.%gà%"); (Note: dùng ilike để tìm kiếm không phân biệt hoa thường)
    // - Tìm theo tên + nhóm: map.put("name", "ilike.%gà%"); map.put("category_id", "eq.1");
    @GET("foods")
    Call<List<Food>> searchFoods(
            @QueryMap Map<String, String> filters
    );


    // =================================================================================
    // MEDICAL CONDITIONS (TIỀN SỬ BỆNH & DỊ ỨNG)
    // =================================================================================

    // Lấy danh sách TOÀN BỘ tiền sử bệnh
    // Dùng cho lúc user khai báo tiền sử bệnh của mình vào hệ thống
    @GET("medical_conditions")
    Call<List<MedicalCondition>> getAllMedicalConditions(
            @Query("select") String select
    );

    // Lấy danh sách Tiền sử bệnh CỦA RIÊNG 1 USER (Có thể lọc theo loại bệnh)
    @GET("users")
    Call<List<User>> getUserSpecificMedicalConditions(
            @Query("id") String eqUserId,                    // VD: "eq.user-uuid-1234"
            @Query("medical_conditions.type") String eqType, // VD: "eq.history" (Chỉ lấy loại history)
            @Query("select") String select                   // VD: "id, medical_conditions(*)"
    );

    // Lấy danh sách Thức ăn cần tránh (Dựa trên danh sách ID bệnh của user)
    @GET("condition_restricted_foods")
    Call<List<MealRecommendedFood>> getRestrictedFoodsByCondition(
            @Query("medical_condition_id") String inConditionIds, // VD: "in.(1,2,3)"
            @Query("select") String select // VD: "*, food:foods(*)"
    );
    // Xóa bệnh cũ của User
    @DELETE("user_medical_conditions")
    Call<Void> deleteUserMedicalConditions(@Query("user_id") String eqUserId);

    // Lưu bệnh mới
    @POST("user_medical_conditions")
    Call<Void> saveUserMedicalConditions(@Body java.util.List<UserMedicalConditionInsert> conditions);

    // =================================================================================
    // KẾ HOẠCH DINH DƯỠNG HÀNG NGÀY (DAILY MEAL PLANNER)
    // =================================================================================

    // 1. LẤY THỰC ĐƠN CỦA 1 NGÀY:
    // Trả về tất cả các món ăn user đã chọn trong 1 ngày cụ thể (Bao gồm Sáng, Trưa, Tối).
    // Có join với bảng foods để lấy luôn tên, calo, hình ảnh của món ăn.
    @GET("user_daily_meals")
    Call<List<com.hcmute.edu.vn.model.UserDailyMeal>> getDailyMeals(
            @Query("user_id") String eqUserId, // Cú pháp: "eq.ID_CUA_USER"
            @Query("date") String eqDate,      // Cú pháp: "eq.2026-03-16"
            @Query("select") String select     // Bắt buộc truyền: "*, foods(*)"
    );

    // 2. THÊM MÓN ĂN VÀO BỮA:
    // Thêm 1 món mới vào bữa Sáng/Trưa/Tối của 1 ngày cụ thể.
    @POST("user_daily_meals")
    Call<Void> addDailyMeal(
            @Body com.hcmute.edu.vn.model.UserDailyMeal meal
    );

    // 3. XÓA MÓN ĂN KHỎI BỮA:
    // Xóa món ăn dựa vào cái ID (uuid) của dòng dữ liệu trong bảng user_daily_meals.
    @DELETE("user_daily_meals")
    Call<Void> deleteDailyMeal(
            @Query("id") String eqId // Cú pháp: "eq.ID_CUA_BẢN_GHI_MEAL"
    );

    // Lấy fitness goal
    @GET("fitness_goals")
    Call<List<com.hcmute.edu.vn.model.FitnessGoal>> getAllFitnessGoals(
            @Query("select") String select
    );
}