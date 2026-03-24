package com.hcmute.edu.vn.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class FitnessCalculator {

    // ACTIVITY LEVEL MULTIPLIERS
    public static final String[] ACTIVITY_LEVEL_LABELS = {
            "Ít vận động (ngồi nhiều)",           // 1.2
            "Vận động nhẹ (1-3 buổi/tuần)",       // 1.375
            "Vận động vừa (3-5 buổi/tuần)",       // 1.55
            "Vận động nhiều (6-7 buổi/tuần)",     // 1.725
            "Vận động rất nhiều (2 lần/ngày)"     // 1.9
    };
    public static final double[] ACTIVITY_MULTIPLIERS = {1.2, 1.375, 1.55, 1.725, 1.9};

    // WORKOUT PLAN IDs (hard-coded theo Supabase)
    // Giảm mỡ
    public static final String PLAN_LOSE_BEGINNER     = "05fea3e9-377e-4108-bee3-15a78150dc43"; // Advanced/HIIT
    public static final String PLAN_LOSE_INTERMEDIATE = "05fea3e9-377e-4108-bee3-15a78150dc43";
    // Tăng cơ
    public static final String PLAN_GAIN_BEGINNER     = "a1111111-1111-1111-1111-111111111111"; // Beginner full-body
    public static final String PLAN_GAIN_INTERMEDIATE = "a2222222-2222-2222-2222-222222222222"; // PPL
    // Giữ dáng
    public static final String PLAN_MAINTAIN          = "a1111111-1111-1111-1111-111111111111";

    // BMR (Mifflin-St Jeor)
    public static double calcBMR(double weightKg, double heightCm, int age, String gender) {
        double base = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        return "Male".equalsIgnoreCase(gender) ? base + 5 : base - 161;
    }

    // TDEE
    public static double calcTDEE(double bmr, int activityIndex) {
        double multiplier = (activityIndex >= 0 && activityIndex < ACTIVITY_MULTIPLIERS.length)
                ? ACTIVITY_MULTIPLIERS[activityIndex] : 1.55;
        return bmr * multiplier;
    }

    // RESULT HOLDER
    public static class FitnessResult {
        public double dailyCalories;
        public double dailyCaloriesToBurn; // chỉ có ý nghĩa khi giảm mỡ
        public String workoutPlanId;
        public int weeksToTarget;
        public String targetDate;          // yyyy-MM-dd
    }

    // MAIN CALCULATION
    /**
     * @param goalName       tên goal (chứa "giảm" / "tăng" / "giữ")
     * @param currentWeight  kg
     * @param targetWeight   kg  (truyền currentWeight nếu giữ dáng)
     * @param tdee           kcal/ngày
     * @param bmi            chỉ số BMI để phân loại beginner/intermediate
     */
    public static FitnessResult calculate(String goalName, double currentWeight,
                                          double targetWeight, double tdee, double bmi) {
        FitnessResult r = new FitnessResult();
        boolean isLose     = goalName.toLowerCase().contains("giảm");
        boolean isGain     = goalName.toLowerCase().contains("tăng");
        boolean isMaintain = !isLose && !isGain;

        // Phân loại beginner / intermediate dựa trên BMI
        boolean isBeginner = (bmi < 25);   // Đơn giản hoá: BMI bình thường = beginner

        if (isMaintain) {
            // ---- GIỮ DÁNG ----
            r.dailyCalories      = tdee;
            r.dailyCaloriesToBurn = 0;
            r.workoutPlanId      = PLAN_MAINTAIN;
            r.weeksToTarget      = 0;

        } else if (isLose) {
            // ---- GIẢM MỠ ----
            double weightDiff    = currentWeight - targetWeight;        // kg cần giảm
            double weeklyLoseKg  = currentWeight * 0.0075;              // 0.75% cân nặng/tuần (an toàn)
            weeklyLoseKg = Math.min(currentWeight * 0.01, weeklyLoseKg);
            r.weeksToTarget      = (int) Math.ceil(weightDiff / weeklyLoseKg);

            double dailyDeficit  = (weeklyLoseKg * 7700.0) / 7.0;      // kcal/ngày cần thiếu hụt
            dailyDeficit = Math.min(dailyDeficit, 1000);               // tránh giảm quá nhanh
            // heuristic split: 70% diet, 30% exercise (common fitness practice)
            r.dailyCalories      = tdee - (0.7 * dailyDeficit);        // 70% deficit từ ăn ít
            r.dailyCaloriesToBurn = 0.3 * dailyDeficit;                // 30% deficit từ tập

            // Không ăn dưới 1200 kcal (nữ) / 1500 kcal (nam)
            double minCalories = isMale ? 1500 : 1200;
            r.dailyCalories = Math.max(r.dailyCalories, minCalories);

            r.workoutPlanId      = isBeginner ? PLAN_LOSE_BEGINNER : PLAN_LOSE_INTERMEDIATE;

        } else {
            // ---- TĂNG CƠ ----
            double weightDiff    = targetWeight - currentWeight;
            double weeklyGainKg  = currentWeight * 0.004;               // 0.4%/tuần
            weeklyGainKg         = Math.max(weeklyGainKg, 0.2);
            weeklyGainKg         = Math.min(weeklyGainKg, 0.5);
            r.weeksToTarget      = (int) Math.ceil(weightDiff / weeklyGainKg);

            double weeklySurplus = weeklyGainKg * 7700.0;               // kcal thặng dư cần/tuần
            double dailySurplus  = weeklySurplus / 7.0;
            dailySurplus = Math.min(dailySurplus, 500);                 // tránh tăng mỡ
            r.dailyCalories      = tdee + dailySurplus;
            r.dailyCaloriesToBurn = 0;

            r.workoutPlanId      = isBeginner ? PLAN_GAIN_BEGINNER : PLAN_GAIN_INTERMEDIATE;
        }

        // Tính ngày đạt mục tiêu
        if (r.weeksToTarget > 0) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.WEEK_OF_YEAR, r.weeksToTarget);
            r.targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(cal.getTime());
        }

        return r;
    }
}