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
    // call func
    // Tăng cơ
    public static final String PLAN_GAIN_BEGINNER     = "554fb805-1136-4976-b39b-e196bcf5a3af";
    public static final String PLAN_GAIN_INTERMEDIATE = "c70b2bb2-a370-4393-96dd-854fa35a4480"; // PPL
    // Giữ dáng
    public static final String PLAN_MAINTAIN_BEGINNER         = "7266ec21-85a2-4295-9f14-3232b5d26864";
    public static final String PLAN_MAINTAIN_INTERMEDIATE     = "22d13d52-3461-4da0-9bd8-f4be811cb25f";

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
     * @param gender         giới tính lấy từ đối tượng User ("Male" / "Female")
     * @param isBeginner     trình độ tập luyện lấy từ đối tượng User (true = Người mới)
     */
    public static FitnessResult calculate(String goalName, double currentWeight,
                                          double targetWeight, double tdee, String gender, boolean isBeginner) {
        FitnessResult r = new FitnessResult();
        boolean isLose     = goalName.toLowerCase().contains("giảm");
        boolean isGain     = goalName.toLowerCase().contains("tăng");
        boolean isMaintain = !isLose && !isGain;

        if (isMaintain) {
            // ---- GIỮ DÁNG ----
            r.dailyCalories      = tdee;
            r.dailyCaloriesToBurn = 0;
            r.workoutPlanId      = isBeginner ? PLAN_MAINTAIN_BEGINNER : PLAN_MAINTAIN_INTERMEDIATE;
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
            boolean isMale = "Male".equalsIgnoreCase(gender);
            double minCalories = isMale ? 1500 : 1200;
            r.dailyCalories = Math.max(r.dailyCalories, minCalories);
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

    // Điều chỉnh kcal sau mỗi tuần
    public static double adjustCaloriesWeekly(double currentDailyCalories, double tdee, int goalId,
                                              double oldWeight, double newWeight, String gender) {

        double actualDiff = newWeight - oldWeight; // Âm là giảm cân, Dương là tăng cân
        double newCalories = currentDailyCalories;
        double minCalories = "Male".equalsIgnoreCase(gender) ? 1500 : 1200;

        if (goalId == 1) {
            // ----------------------------------------------------
            // GIẢM MỠ: giảm 0.75% cân nặng, tối đa 1%
            // ----------------------------------------------------
            double expectedLoss = -(oldWeight * 0.0075);
            double maxSafeLoss = -(oldWeight * 0.01);

            // Nếu giảm chậm hơn kỳ vọng (Ví dụ: thực tế chỉ giảm -0.2kg, thiếu 0.4kg)
            if (actualDiff > expectedLoss) {
                double missedKg = actualDiff - expectedLoss; // Số kg chưa đốt được

                // Tính toán chính xác số Calo cần cắt thêm mỗi ngày
                double kcalAdjustment = (missedKg * 7700.0) / 7.0;

                // Chốt chặn an toàn: Không cắt cái rụp quá 300 kcal/ngày để tránh stress
                kcalAdjustment = Math.min(kcalAdjustment, 300);

                newCalories = currentDailyCalories - kcalAdjustment;
            } else if (actualDiff < maxSafeLoss) {
                // GIẢM QUÁ NHANH (Nguy hiểm) -> CỘNG THÊM CALO VÀO ĐỂ HÃM PHANH
                double overLossKg = maxSafeLoss - actualDiff; // Số kg giảm lố

                // Tính số Calo cần trả lại cho cơ thể
                double kcalToReadd = (overLossKg * 7700.0) / 7.0;

                // Không nhồi lại quá nhiều một lúc gây sốc hệ tiêu hóa (tối đa trả lại 500 kcal)
                kcalToReadd = Math.min(kcalToReadd, 500);

                // Tạm tính mức Calo mới sau khi bơm thêm
                double tempCalories = currentDailyCalories + kcalToReadd;

                // CHỐT CHẶN AN TOÀN TỐI ĐA (CEILING):
                // Không được vượt quá TDEE. Ta giữ lại khoản thâm hụt 250 kcal để vẫn tiếp tục giảm mỡ.
                double maxAllowedCalories = tdee - 250;

                newCalories = Math.min(tempCalories, maxAllowedCalories);
            }

        } else if (goalId == 2) {
            // ----------------------------------------------------
            // TĂNG CƠ: tăng 0.4% cân nặng
            // ----------------------------------------------------
            double expectedGain = oldWeight * 0.004;

            // Nếu tăng chậm hơn kỳ vọng (Ví dụ: thực tế chỉ tăng +0.1kg, thiếu 0.14kg)
            if (actualDiff < expectedGain) {
                double missedKg = expectedGain - actualDiff; // Số kg chưa đắp được

                // Tính toán chính xác số Calo cần nhồi thêm mỗi ngày
                double kcalAdjustment = (missedKg * 7700.0) / 7.0;

                // Chốt chặn an toàn: Không nhồi quá 300 kcal/ngày để tránh tích mỡ thừa
                kcalAdjustment = Math.min(kcalAdjustment, 300);

                newCalories = currentDailyCalories + kcalAdjustment;
            }

        } else {
            // ----------------------------------------------------
            // GIỮ DÁNG: trong khoảng 0.5kg
            // ----------------------------------------------------
            if (actualDiff > 0.5) { // Đang béo lên
                double overKg = actualDiff - 0.5;
                double kcalAdjustment = Math.min((overKg * 7700.0) / 7.0, 300);
                newCalories = currentDailyCalories - kcalAdjustment;

            } else if (actualDiff < -0.5) { // Đang gầy đi
                double underKg = Math.abs(actualDiff) - 0.5;
                double kcalAdjustment = Math.min((underKg * 7700.0) / 7.0, 300);
                newCalories = currentDailyCalories + kcalAdjustment;
            }
        }

        return Math.max(newCalories, minCalories);
    }
}