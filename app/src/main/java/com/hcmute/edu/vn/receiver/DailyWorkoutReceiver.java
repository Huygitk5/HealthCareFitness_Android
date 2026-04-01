package com.hcmute.edu.vn.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.HomeActivity;

import java.util.Calendar;

public class DailyWorkoutReceiver extends BroadcastReceiver {

    // Tạo một ID kênh thông báo riêng biệt, không đụng hàng với AI
    private static final String CHANNEL_ID = "DAILY_WORKOUT_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        String title;
        String message;

        // Nếu là CHỦ NHẬT
        if (dayOfWeek == Calendar.SUNDAY) {
            title = "Kiểm tra vóc dáng thôi nào! ⚖️";
            message = "Đã 5h chiều Chủ Nhật. Hãy bước lên cân và cập nhật BMI để xem thành quả tuần qua nhé!";
        }
        // Nếu là CÁC NGÀY TRONG TUẦN
        else {
            title = "Tới giờ tập luyện rồi! 🏋️‍♂️";
            message = "Đã 5h chiều, hãy dành chút thời gian vận động để hoàn thành mục tiêu hôm nay nào!";
        }

        Intent tapIntent = new Intent(context, HomeActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_workout)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Dùng ID 1002 để không đè lên thông báo số 1001 của AI Chatbot
            notificationManager.notify(1002, builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lịch tập & BMI hàng ngày";
            String description = "Nhắc nhở tập luyện lúc 5h chiều và đo BMI vào Chủ Nhật";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}