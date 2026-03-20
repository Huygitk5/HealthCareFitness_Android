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

public class WeightReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "WEIGHT_REMINDER_CHANNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo Channel cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở cân nặng",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Kênh thông báo nhắc nhở cập nhật cân nặng mỗi sáng");
            notificationManager.createNotificationChannel(channel);
        }

        // Bấm vào thông báo sẽ mở HomeActivity
        Intent tapIntent = new Intent(context, HomeActivity.class);
        // TUYỆT CHIÊU: Gửi kèm 1 tín hiệu bí mật để HomeActivity biết mà tự động mở Dialog
        tapIntent.putExtra("OPEN_UPDATE_BMI", true);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 200, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Xây dựng Giao diện Thông báo
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // Thay bằng icon của app bạn
                .setContentTitle("Chào buổi sáng! ⚖️")
                .setContentText("Đừng quên cập nhật cân nặng hôm nay để AI Coach theo dõi tiến độ của bạn nhé!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Bắn thông báo
        if (notificationManager != null) {
            notificationManager.notify(1002, builder.build());
        }
    }
}