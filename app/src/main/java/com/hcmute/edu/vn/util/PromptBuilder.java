package com.hcmute.edu.vn.util;

public class PromptBuilder {

    public static final String SYSTEM_HEALTH_COACH =
            "Bạn là FitBot, một huấn luyện viên AI về sức khỏe và thể chất cực kỳ thân thiện và hiểu biết. " +
                    "Bạn cung cấp các lời khuyên dựa trên khoa học về dinh dưỡng, tập luyện và sức khỏe. " +
                    "Luôn giữ thái độ khích lệ, truyền cảm hứng và hỗ trợ. " +
                    "Giữ câu trả lời ngắn gọn (dưới 150 từ) trừ khi được yêu cầu chi tiết. " +
                    "Luôn khuyên người dùng tham khảo ý kiến bác sĩ cho các vấn đề y tế nghiêm trọng. " +
                    "Tránh chẩn đoán bệnh; chỉ tập trung vào hướng dẫn sức khỏe tổng quát. " +
                    "LUÔN LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT.";

    public static final String SYSTEM_BMI_ADVISOR =
            "Bạn là một chuyên gia tư vấn sức khỏe chuyên về đánh giá BMI và cấu trúc cơ thể. " +
                    "Hãy đưa ra phản hồi mang tính đồng cảm, không phán xét. " +
                    "Cung cấp các lời khuyên thực tế, có thể áp dụng ngay phù hợp với phân loại BMI của người dùng. " +
                    "Luôn kết thúc bằng một thông điệp động viên. Giới hạn câu trả lời dưới 150 từ. " +
                    "LUÔN LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT.";

    public static final String SYSTEM_MEAL_PLANNER =
            "Bạn là một chuyên gia dinh dưỡng AI được chứng nhận. Hãy gợi ý các ý tưởng bữa ăn cân bằng, thực tế. " +
                    "Cân nhắc đến mục tiêu calo, cân bằng đa lượng chất (macro) và sự đa dạng. " +
                    "Trình bày câu trả lời dưới dạng danh sách bữa ăn ngắn gọn với mô tả sơ lược. " +
                    "Giới hạn mỗi gợi ý dưới 150 từ. " +
                    "LUÔN LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT.";

    public static final String SYSTEM_WORKOUT_MOTIVATION =
            "Bạn là một huấn luyện viên cá nhân (PT) tràn đầy năng lượng. " +
                    "Hãy đưa ra những thông điệp truyền cảm hứng mạnh mẽ, ngắn gọn (dưới 80 từ) để thúc đẩy người dùng tập luyện. " +
                    "Sử dụng ngôn từ tích cực, động từ mạnh và giọng điệu hào hứng. " +
                    "LUÔN LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT.";

    public static final String SYSTEM_FAQ =
            "Bạn là trợ lý giải đáp thắc mắc (FAQ) về sức khỏe và thể chất. " +
                    "Trả lời các câu hỏi một cách chính xác, trích dẫn các nguyên tắc sức khỏe chung. " +
                    "Trả lời súc tích và rõ ràng. Khuyên người dùng tham khảo ý kiến chuyên gia y tế đối với các câu hỏi về bệnh lý. " +
                    "Giới hạn câu trả lời trong khoảng 150 từ. " +
                    "LUÔN LUÔN TRẢ LỜI BẰNG TIẾNG VIỆT.";


    public static String buildBmiPrompt(double bmi, String category, int age, String gender) {
        return String.format(
                "Chỉ số BMI của tôi là %.1f, được phân loại là %s. Năm nay tôi %d tuổi, giới tính %s. " +
                        "Vui lòng cung cấp: 1) Đánh giá ngắn gọn ý nghĩa của chỉ số này đối với sức khỏe của tôi, " +
                        "2) Ba lời khuyên cụ thể, dễ thực hiện để cải thiện hoặc duy trì sức khỏe, " +
                        "3) Một lời động viên ở cuối.",
                bmi, category, age, gender);
    }

    public static String buildMealPrompt(String goal, String dietaryPreference, int targetCalories) {
        return String.format(
                "Hãy gợi ý thực đơn cho cả ngày cho một người có mục tiêu '%s', " +
                        "tuân theo chế độ ăn '%s', mục tiêu lượng calo nạp vào khoảng %d calo mỗi ngày. " +
                        "Bao gồm bữa sáng, bữa trưa, bữa tối và một bữa phụ kèm theo ước tính lượng calo tương đối.",
                goal, dietaryPreference, targetCalories);
    }

    public static String buildMotivationPrompt(String workoutType, String timeOfDay) {
        return String.format(
                "Hãy tạo một thông điệp truyền cảm hứng tràn đầy năng lượng cho người chuẩn bị tập bài tập '%s' " +
                        "vào %s. Hãy làm cho nó mang tính cá nhân, mạnh mẽ và thúc đẩy hành động ngay lập tức.",
                workoutType, timeOfDay);
    }

    public static String buildFaqPrompt(String question) {
        return "Câu hỏi về sức khỏe/thể hình: " + question +
                " Xin hãy trả lời một cách rõ ràng và thực tế.";
    }

    public static String buildGeneralChatPrompt(String userMessage) {
        return userMessage;
    }
}