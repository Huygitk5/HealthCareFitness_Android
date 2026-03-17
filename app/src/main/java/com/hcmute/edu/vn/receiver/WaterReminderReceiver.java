package com.hcmute.edu.vn.receiver; // Sửa lại package cho đúng với thư mục của bạn

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

public class WaterReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "WATER_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Lấy giờ hiện tại
        Calendar calendar = Calendar.getInstance();
        int currentHour = calendar.get(Calendar.HOUR_OF_DAY);

        // THUẬT TOÁN BẢO VỆ GIẤC NGỦ: Chỉ bắn thông báo từ 6h sáng đến 22h đêm
        if (currentHour >= 6 && currentHour <= 22) {
            showNotification(context);
        }
    }

    private void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo Channel (Bắt buộc cho Android 8.0 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở uống nước",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Kênh thông báo nhắc nhở uống nước mỗi 2 tiếng");
            notificationManager.createNotificationChannel(channel);
        }

        // Bấm vào thông báo sẽ mở lại app
        Intent tapIntent = new Intent(context, HomeActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE);

        // Xây dựng Giao diện Thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_nutriton) // Đổi thành icon giọt nước của bạn nếu có
                .setContentTitle("Đến giờ uống nước rồi! 💧")
                .setContentText("Uống ngay một cốc nước để cơ thể luôn khỏe mạnh nhé!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Bắn thông báo (Dùng ID 1001 để dễ quản lý)
        notificationManager.notify(1001, builder.build());
    }
}