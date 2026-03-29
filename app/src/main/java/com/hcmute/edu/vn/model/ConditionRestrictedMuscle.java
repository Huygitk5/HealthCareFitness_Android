package com.hcmute.edu.vn.model;

import com.google.gson.annotations.SerializedName;

public class ConditionRestrictedMuscle {

    @SerializedName("condition_id")
    private Integer medicalConditionId;

    @SerializedName("muscle_group_id")
    private Integer muscleGroupId;

    public ConditionRestrictedMuscle() {}

    public Integer getMedicalConditionId() { return medicalConditionId; }
    public Integer getMuscleGroupId() { return muscleGroupId; }
}