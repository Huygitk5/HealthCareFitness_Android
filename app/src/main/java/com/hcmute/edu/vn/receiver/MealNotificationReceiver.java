package com.hcmute.edu.vn.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.core.app.NotificationCompat;

import com.hcmute.edu.vn.activity.NutritionActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MealNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Kiểm tra xem user đã tick bữa nào chưa trước khi gửi thông báo
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String userId = userPrefs.getString("KEY_USER_ID", "");
        String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        SharedPreferences prefLogs = context.getSharedPreferences("MealLogs", Context.MODE_PRIVATE);
        boolean bLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_BREAKFAST", false);
        boolean lLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_LUNCH", false);
        boolean dLog = prefLogs.getBoolean("MEAL_LOG_" + userId + "_" + dateStr + "_DINNER", false);

        // Nếu CHƯA tick bữa nào, bắn Notification
        if (!bLog && !lLog && !dLog) {
            showNotification(context);
        }
    }

    private void showNotification(Context context) {
        String channelId = "MEAL_REMINDER_CHANNEL";
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Nhắc nhở bữa ăn", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        // Mở lại NutritionActivity khi bấm vào thông báo
        Intent openIntent = new Intent(context, NutritionActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // Đổi thành icon app của bạn
                .setContentTitle("Bạn quên ghi nhận bữa ăn? 🍽️")
                .setContentText("Đã 20:00 rồi, hãy vào ứng dụng để hoàn thành khảo sát dinh dưỡng hôm nay nhé!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        manager.notify(2000, builder.build());
    }
}
