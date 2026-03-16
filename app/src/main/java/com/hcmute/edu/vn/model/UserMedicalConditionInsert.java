package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class UserMedicalConditionInsert {
    @SerializedName("user_id")
    private String userId;

    // ĐÃ SỬA Ở ĐÂY: Đổi tên cho khớp đúng 100% với cột trên Supabase của bạn
    @SerializedName("condition_id")
    private Integer medicalConditionId;

    public UserMedicalConditionInsert(String userId, Integer medicalConditionId) {
        this.userId = userId;
        this.medicalConditionId = medicalConditionId;
    }

    public String getUserId() { return userId; }
    public Integer getMedicalConditionId() { return medicalConditionId; }
}