package com.hcmute.edu.vn.activity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.hcmute.edu.vn.BuildConfig;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.MessageAdapter;
import com.hcmute.edu.vn.api.GeminiService;
import com.hcmute.edu.vn.database.SupabaseApiService;
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.model.Message;
import com.hcmute.edu.vn.model.User;
import com.hcmute.edu.vn.model.gemini.GeminiRequest;
import com.hcmute.edu.vn.model.gemini.GeminiResponse;
import com.hcmute.edu.vn.util.PromptBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageView btnSend, ivBack;
    private ProgressBar progressBar;
    
    private TextView chipBMI, chipMeals, chipMotivate, chipFAQ, chipReminder;

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private GeminiService geminiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        View topBar = findViewById(R.id.topBar);
        View inputBox = findViewById(R.id.bottomContainer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Lấy kích thước của cả System Bars (thanh trạng thái) VÀ Bàn phím (ime)
            androidx.core.graphics.Insets insetsToApply = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());

            if (topBar != null) {
                // Né camera (tai thỏ) ở phía trên
                topBar.setPadding(0, insetsToApply.top, 0, 0);
            }
            if (inputBox != null) {
                // Đẩy toàn bộ khu vực nhập chat lên đúng bằng độ cao của bàn phím
                inputBox.setPadding(0, 0, 0, insetsToApply.bottom);
            }

            // Tự động cuộn RecyclerView xuống tin nhắn cuối cùng khi bàn phím bật lên
            if (messageAdapter != null && messageList.size() > 0) {
                rvMessages.scrollToPosition(messageList.size() - 1);
            }

            return insets;
        });

        androidx.core.view.WindowInsetsControllerCompat controller = new androidx.core.view.WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();
        setupRecyclerView();
        setupRetrofit();
        setupClickListeners();

        // Initial greeting
        addBotMessage("Hi! I'm your AI Fitness Coach. How can I help you today? You can ask me questions or tap a shortcut below.");
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        ivBack = findViewById(R.id.ivBack);
        progressBar = findViewById(R.id.progressBar);

        chipBMI = findViewById(R.id.chipBMI);
        chipMeals = findViewById(R.id.chipMeals);
        chipMotivate = findViewById(R.id.chipMotivate);
        chipFAQ = findViewById(R.id.chipFAQ);
        chipReminder = findViewById(R.id.chipReminder);
    }

    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        geminiService = retrofit.create(GeminiService.class);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessageToGemini(PromptBuilder.SYSTEM_HEALTH_COACH, text, false);
                etMessage.setText("");
            }
        });

        chipBMI.setOnClickListener(v -> {
            addUserMessage("Đánh giá BMI của tôi");
            progressBar.setVisibility(View.VISIBLE);
            SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            String username = pref.getString("KEY_USER", null);
            if (username == null) {
                addBotMessage("Xin lỗi, tôi không tìm thấy phiên đăng nhập của bạn.");
                progressBar.setVisibility(View.GONE);
                return;
            }

            // Gọi API lấy thông tin User từ Supabase
            SupabaseApiService apiService = SupabaseClient.getClient().create(SupabaseApiService.class);
            apiService.getUserByUsername("eq." + username, "*").enqueue(new Callback<List<User>>() {
                @Override
                public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                    if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                        User currentUser = response.body().get(0);

                        Double weight = currentUser.getWeight();
                        Double height = currentUser.getHeight();

                        // Kiểm tra xem user đã nhập chiều cao, cân nặng chưa
                        if (weight == null || height == null || weight <= 0 || height <= 0) {
                            addBotMessage("Tôi chưa có thông tin chiều cao và cân nặng của bạn. Bạn vui lòng cập nhật trong mục Hồ sơ (Profile) nhé!");
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        // Tính toán BMI và phân loại
                        double heightM = height / 100.0;
                        double bmi = weight / (heightM * heightM);

                        String category = "";
                        if (bmi < 18.5) category = "Thiếu cân";
                        else if (bmi < 23) category = "Bình thường";
                        else if (bmi < 25) category = "Thừa cân";
                        else category = "Béo phì";

                        // Tính tuổi
                        int age = calculateAge(currentUser.getDateOfBirth());
                        String gender = (currentUser.getGender() != null) ? currentUser.getGender() : "Không xác định";

                        String prompt  = PromptBuilder.buildBmiPrompt(bmi, category, age, gender);

                        // Đóng vai SYSTEM_BMI_ADVISOR
                        sendMessageToGemini(PromptBuilder.SYSTEM_BMI_ADVISOR, prompt, true);

                    } else {
                        addBotMessage("Xin lỗi, tôi không thể lấy dữ liệu cơ thể của bạn lúc này.");
                        progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onFailure(Call<List<User>> call, Throwable t) {
                    // Ép in ra lỗi chi tiết từ hệ thống
                    addBotMessage("Lỗi hệ thống: " + t.getMessage());

                    // Dòng này giúp in lỗi chi tiết chữ đỏ trong tab Logcat của Android Studio
                    android.util.Log.e("LOI_API_BMI", "Chi tiết nguyên nhân rớt:", t);

                    progressBar.setVisibility(View.GONE);
                }
            });
        });

//        chipMeals.setOnClickListener(v -> {
//            addUserMessage("Gợi ý bữa ăn");
//            // Giả sử mục tiêu là Giảm cân (có thể lấy từ database user sau này), lượng calo 1500
//            String prompt = PromptBuilder.buildMealPrompt("Giảm cân, mỡ thừa", "Bình thường", 1500);
//            sendMessageToGemini(PromptBuilder.SYSTEM_MEAL_PLANNER, prompt, true);
//        });

        chipMotivate.setOnClickListener(v -> {
            addUserMessage("Truyền động lực cho tôi");

            // Lấy buổi trong ngày tự động
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            String timeOfDay = (hour < 12) ? "buổi sáng" : (hour < 18 ? "buổi chiều" : "buổi tối");

            String prompt = PromptBuilder.buildMotivationPrompt("Đốt mỡ toàn thân", timeOfDay);
            sendMessageToGemini(PromptBuilder.SYSTEM_WORKOUT_MOTIVATION, prompt, true);
        });

        chipFAQ.setOnClickListener(v -> {
            addUserMessage("Hỏi đáp kiến thức");
            // Tạo sẵn một câu hỏi phổ biến để mồi cho bot
            String prompt = PromptBuilder.buildFaqPrompt("Làm sao để tránh đau cơ sau khi tập nặng?");
            sendMessageToGemini(PromptBuilder.SYSTEM_FAQ, prompt, true);
        });

        chipReminder.setOnClickListener(v -> {
            showTimePickerForReminder();
        });
    }

    private void addUserMessage(String text) {
        messageList.add(new Message(text, false));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
    }

    private void addBotMessage(String text) {
        messageList.add(new Message(text, true));
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        rvMessages.scrollToPosition(messageList.size() - 1);
    }

    private void sendMessageToGemini(String systemPrompt, String input, boolean isShortcut) {
        if (!isShortcut) {
            addUserMessage(input);
        }
        
        progressBar.setVisibility(View.VISIBLE);

        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("null")) {
            addBotMessage("Sorry, the Gemini API key is missing. Please check your local.properties file.");
            progressBar.setVisibility(View.GONE);
            return;
        }

        String fullPrompt = systemPrompt + "\nUser says: " + input;
        
        List<GeminiRequest.Part> parts = new ArrayList<>();
        parts.add(new GeminiRequest.Part(fullPrompt));
        
        List<GeminiRequest.Content> contents = new ArrayList<>();
        contents.add(new GeminiRequest.Content("user", parts));
        
        GeminiRequest request = new GeminiRequest(contents);

        geminiService.generateContent(apiKey, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String botResponse = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();
                        addBotMessage(botResponse);
                    } catch (Exception e) {
                        addBotMessage("Sorry, I couldn't understand the response from the server.");
                    }
                } else {
                    try {
                        String errorDetail = response.errorBody() != null ? response.errorBody().string() : "Không rõ nguyên nhân";
                        addBotMessage("Lỗi API " + response.code() + ": " + errorDetail);
                    } catch (Exception e) {
                        addBotMessage("Lỗi kết nối: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                addBotMessage("Network error. Please try again later.");
            }
        });
    }

    private void showTimePickerForReminder() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minuteOfHour) -> setReminder(hourOfDay, minuteOfHour),
                hour, minute, true);
        timePickerDialog.show();
    }

    private void setReminder(int hourOfDay, int minute) {
        // We will implement the BroadcastReceiver next
        // For now, let's just show a toast and pretend it's scheduled.
        // We need to write the receiver to finish this logic.
        
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        
        Intent intent = new Intent(this, com.hcmute.edu.vn.receiver.WorkoutReminderReceiver.class);
        
        // Use FLAG_IMMUTABLE as required by newer Android versions
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
                
                String timeStr = String.format("%02d:%02d", hourOfDay, minute);
                addUserMessage("Set reminder for " + timeStr);
                addBotMessage("Awesome! I've set a workout reminder for " + timeStr + ". Let's crush those goals!");
                
            } catch (SecurityException e) {
                addBotMessage("Wait, I need permission to set exact alarms. Please enable it in the settings.");
            }
        }
    }

    private int calculateAge(String dobString) {
        if (dobString == null || dobString.isEmpty()) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date birthDate = sdf.parse(dobString);
            if (birthDate == null) return 0;

            Calendar dob = Calendar.getInstance();
            dob.setTime(birthDate);
            Calendar today = Calendar.getInstance();

            int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
            if (today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
                    (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))) {
                age--;
            }
            return age;
        } catch (Exception e) {
            return 0;
        }
    }
}
