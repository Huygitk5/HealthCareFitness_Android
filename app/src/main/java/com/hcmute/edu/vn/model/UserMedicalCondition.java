package com.hcmute.edu.vn.model;
import com.google.gson.annotations.SerializedName;

public class UserMedicalCondition {
    @SerializedName("condition_id")
    private Integer conditionId;
    @SerializedName("medical_conditions")
    private MedicalCondition medicalCondition;

    public MedicalCondition getMedicalCondition() { return medicalCondition; }
    public void setMedicalCondition(MedicalCondition medicalCondition) { this.medicalCondition = medicalCondition; }

    public Integer getConditionId() {
        return conditionId;
    }

    public void setConditionId(Integer conditionId) {
        this.conditionId = conditionId;
    }
}