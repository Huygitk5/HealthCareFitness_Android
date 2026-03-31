package com.hcmute.edu.vn.util;

import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.model.UserDailyWorkout;
import com.hcmute.edu.vn.model.WorkoutDay;
import com.hcmute.edu.vn.model.WorkoutDayExercise;
import com.hcmute.edu.vn.model.WorkoutPlan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class WorkoutGenerator {

    // Trích xuất số Reps từ chuỗi trên DB (VD: "12-15" -> 12, "30 secs" -> 30)
    private static int parseReps(String repsStr) {
        if (repsStr == null || repsStr.isEmpty()) return 12;
        try {
            String numberOnly = repsStr.replaceAll("[^0-9]", "");
            return numberOnly.isEmpty() ? 12 : Integer.parseInt(numberOnly);
        } catch (Exception e) {
            return 12;
        }
    }

    // Tính thời gian THỰC TẬP của 1 bài tập (Time Under Tension)
    private static int calculateTotalExerciseDurationInSeconds(Exercise ex, int reps, int restTimeSeconds) {
        int sets = ex.getBaseRecommendedSets() != null ? ex.getBaseRecommendedSets() : 3;
        int timePerRep = ex.getTimePerRep() != null ? ex.getTimePerRep() : 3;

        // Thời gian tập thực tế
        int activeTime = sets * reps * timePerRep;
        // Thời gian nghỉ giữa các hiệp = (Số hiệp - 1) * thời gian nghỉ
        int restTime = (sets > 0 ? sets - 1 : 0) * restTimeSeconds;

        return activeTime + restTime;
    }

    /**
     * Hàm sinh lịch tập Giảm Mỡ 7 ngày cá nhân hóa, lưu vào bảng user_daily_workouts
     */
    public static List<UserDailyWorkout> generateWeeklyWeightLossWorkouts(
            String userId,
            WorkoutPlan basePlan,
            Date startDate,
            List<Exercise> allExercises,
            List<Integer> restrictedMuscleIds,
            boolean isBeginner,
            double dailyCaloriesToBurn,
            double weightKg) {

        List<UserDailyWorkout> weeklyWorkouts = new ArrayList<>();
        if (dailyCaloriesToBurn <= 0 || basePlan == null || basePlan.getDays() == null) return weeklyWorkouts;

        String planId = basePlan.getId();

        // 1. Lọc bài tập theo bệnh lý và độ khó
        List<Exercise> gymPool = new ArrayList<>();
        List<Exercise> cardioPool = new ArrayList<>();
        List<Exercise> hiitPool = new ArrayList<>();

        for (Exercise ex : allExercises) {
            int diff = ex.getDifficultyLevelId() != null ? ex.getDifficultyLevelId() : 1;
            int type = ex.getExerciseTypeId() != null ? ex.getExerciseTypeId() : 1;
            int muscleId = ex.getMuscleGroupId() != null ? ex.getMuscleGroupId() : 0;

            boolean isRestricted = restrictedMuscleIds.contains(muscleId);
            boolean isAllowed = false;

            if (isRestricted) { if (diff == 1) isAllowed = true; }
            else {
                if (isBeginner && (diff == 1 || diff == 2)) isAllowed = true;
                if (!isBeginner && (diff == 2 || diff == 3)) isAllowed = true;
            }

            if (isAllowed) {
                if (type == 1) gymPool.add(ex);
                else if (type == 2) cardioPool.add(ex);
                else if (type == 3) hiitPool.add(ex);
            }
        }

        if (cardioPool.isEmpty()) cardioPool.addAll(gymPool);
        if (hiitPool.isEmpty()) hiitPool.addAll(cardioPool);

        // 2. Tính Calo/Giây
        double weeklyBurnTarget = dailyCaloriesToBurn * 7.0;
        int numSessions = isBeginner ? 4 : 5;
        double burnPerSession = weeklyBurnTarget / numSessions;

        int gymTargetSeconds = (int) ((burnPerSession / (FitnessCalculator.MET_GYM * weightKg)) * 3600);
        int cardioTargetSeconds = (int) ((burnPerSession / (FitnessCalculator.MET_CARDIO * weightKg)) * 3600);
        int hiitTargetSeconds = (int) ((burnPerSession / (FitnessCalculator.MET_HIIT * weightKg)) * 3600);

        String[] schedule = isBeginner ?
                new String[]{"GYM", "REST", "CARDIO", "REST", "GYM", "CARDIO", "REST"} :
                new String[]{"GYM", "HIIT", "GYM", "REST", "GYM", "HIIT", "REST"};

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // LẤY CÁC NGÀY THẬT (REAL DAY IDS) TỪ DATABASE ĐỂ ĐÁP ỨNG KHÓA NGOẠI
        List<WorkoutDay> validDays = new ArrayList<>(basePlan.getDays());
        Collections.sort(validDays, (d1, d2) -> Integer.compare(
                d1.getDayOrder() != null ? d1.getDayOrder() : 0,
                d2.getDayOrder() != null ? d2.getDayOrder() : 0));

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            String dayType = schedule[i];

            // Trích xuất ID thật (Nếu gói có đủ 7 ngày)
            String realDayId = (i < validDays.size()) ? validDays.get(i).getId() : null;
            if (realDayId == null) continue; // Bỏ qua nếu ID không tồn tại

            if (!dayType.equals("REST")) {
                int targetSeconds = 0;
                List<Exercise> currentPool = new ArrayList<>();
                if (dayType.equals("GYM")) { targetSeconds = gymTargetSeconds; currentPool = new ArrayList<>(gymPool); }
                else if (dayType.equals("CARDIO")) { targetSeconds = cardioTargetSeconds; currentPool = new ArrayList<>(cardioPool); }
                else if (dayType.equals("HIIT")) { targetSeconds = hiitTargetSeconds; currentPool = new ArrayList<>(hiitPool); }

                Collections.shuffle(currentPool);
                int currentDayActiveSeconds = 0;
                int exerciseOrder = 0;

                for (Exercise ex : currentPool) {
                    if (currentDayActiveSeconds >= targetSeconds) break;

                    int sets = ex.getBaseRecommendedSets() != null ? ex.getBaseRecommendedSets() : 3;
                    int reps = parseReps(ex.getBaseRecommendedReps());
                    int restTimeSeconds = 60;

                    UserDailyWorkout udw = new UserDailyWorkout(
                            userId, dateStr, ex.getId(), sets, String.valueOf(reps), restTimeSeconds, exerciseOrder++, planId,
                            realDayId // <--- GÁN KHÓA NGOẠI HỢP LỆ VÀO ĐÂY ĐỂ TRÁNH LỖI LƯU
                    );
                    weeklyWorkouts.add(udw);
                    currentDayActiveSeconds += calculateTotalExerciseDurationInSeconds(ex, reps, restTimeSeconds);
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return weeklyWorkouts;
    }
    // =====================================================================
    // HÀM COPY LỊCH TẬP TĨNH (TĂNG CƠ / GIỮ DÁNG) VÀO USER_DAILY_WORKOUTS
    // =====================================================================
    public static List<UserDailyWorkout> copyStaticPlanToDailyWorkouts(
            String userId, Date startDate, WorkoutPlan staticPlan) {

        List<UserDailyWorkout> weeklyWorkouts = new ArrayList<>();
        if (staticPlan == null || staticPlan.getDays() == null) return weeklyWorkouts;

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Đảm bảo các ngày được sắp xếp đúng thứ tự (Day 1 -> Day 7)
        List<WorkoutDay> days = new ArrayList<>();
        for(WorkoutDay d : staticPlan.getDays()) {
            if (d != null) days.add(d);
        }

        Collections.sort(days, (d1, d2) -> {
            int order1 = d1.getDayOrder() != null ? d1.getDayOrder() : 0;
            int order2 = d2.getDayOrder() != null ? d2.getDayOrder() : 0;
            return Integer.compare(order1, order2);
        });

        for (WorkoutDay day : days) {
            String dateStr = sdf.format(cal.getTime());

            if (day.getExercises() != null && !day.getExercises().isEmpty()) {
                int order = 0;
                for (com.hcmute.edu.vn.model.WorkoutDayExercise wde : day.getExercises()) {
                    if (wde == null) continue;

                    Integer sets = wde.getSets() != null ? wde.getSets() : 3;
                    String reps = wde.getReps() != null ? String.valueOf(wde.getReps()) : "12";
                    Integer restTime = wde.getRestTimeSeconds() != null ? wde.getRestTimeSeconds() : 60;

                    // Bảo vệ chống Null Exercise ID
                    String exerciseId = wde.getExerciseId();
                    if (exerciseId == null && wde.getExercise() != null) {
                        exerciseId = wde.getExercise().getId();
                    }

                    if (exerciseId != null) {
                        UserDailyWorkout udw = new UserDailyWorkout(
                                userId,
                                dateStr,
                                exerciseId,
                                sets,
                                reps,
                                restTime,
                                order++,
                                staticPlan.getId(),
                                day.getId()
                        );
                        weeklyWorkouts.add(udw);
                    }
                }
            }
            // Tăng lên 1 ngày
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return weeklyWorkouts;
    }
}