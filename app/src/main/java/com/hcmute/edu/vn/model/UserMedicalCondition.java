package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class UserMedicalCondition {
    // ĐÃ SỬA: Đổi thành medical_conditions (có s) cho khớp đúng tên bảng trên Supabase
    @SerializedName("medical_conditions")
    private MedicalCondition medicalCondition;

    public MedicalCondition getMedicalCondition() { return medicalCondition; }
    public void setMedicalCondition(MedicalCondition medicalCondition) { this.medicalCondition = medicalCondition; }
}