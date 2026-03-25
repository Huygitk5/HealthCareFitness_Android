package com.hcmute.edu.vn.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.activity.ExerciseActivity;
import com.hcmute.edu.vn.model.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * MusicService — Hybrid Start + Bound Service.
 *
 * - START SERVICE: Gọi startService() để Service sống độc lập với Activity.
 *   Nhạc tiếp tục phát khi Activity xoay màn hình hoặc bị destroy.
 * - BOUND SERVICE: Activity bindService() để lấy MusicBinder và gọi play/pause/next...
 *   Khi không còn client nào bind, Service vẫn không bị kill (vì đã startService).
 *
 * Vòng đời:
 *   ExerciseActivity.onCreate → startService() + bindService()
 *   ExerciseActivity.onDestroy → unbindService() (Service vẫn sống)
 *   Người dùng bấm nút đóng Activity một cách có chủ ý → stopService()
 */
public class MusicService extends Service {

    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "music_service_channel";
    private static final int NOTIF_ID = 101;

    // ======================= Mock Data =======================
    /**
     * Tạo danh sách nhạc mẫu.
     * Thay rawResId bằng R.raw.ten_file khi bạn thêm file mp3 vào res/raw/.
     * Hiện tại dùng 0 → Service sẽ bỏ qua và không crash.
     */
    public static List<Song> getMockSongs() {
        List<Song> list = new ArrayList<>();
        list.add(new Song(1, "Fresh Day",       "Workout Beats",  0, 0));
        list.add(new Song(2, "Energy Boost",    "Gym Vibes",      0, 0));
        list.add(new Song(3, "Push The Limit",  "FitRhythm",      0, 0));
        list.add(new Song(4, "Morning Run",     "ActiveSound",    0, 0));
        list.add(new Song(5, "Power Up",        "BeatFit",        0, 0));
        return list;
    }
    // =========================================================

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private int currentIndex = 0;
    private boolean isPrepared = false; // true sau khi MediaPlayer sẵn sàng

    // ======================= Binder =======================

    /**
     * Inner Binder class — Activity lấy đối tượng này qua ServiceConnection
     * để gọi trực tiếp các phương thức điều khiển nhạc.
     */
    public class MusicBinder extends Binder {
        /** Trả về instance của Service để Activity gọi các phương thức public. */
        public MusicService getService() {
            return MusicService.this;
        }
    }

    // ======================= Lifecycle =======================

    @Override
    public void onCreate() {
        super.onCreate();
        songList = getMockSongs();
        createNotificationChannel();
        Log.d(TAG, "onCreate: MusicService created");
    }

    /**
     * Gọi khi Activity gọi startService().
     * Return START_STICKY để Android tự khởi động lại Service nếu bị kill.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Service started");
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Client bound");
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // Trả về true → onRebind() sẽ được gọi khi client bind lại (sau xoay màn hình)
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
        Log.d(TAG, "onDestroy: MusicService destroyed");
    }

    // ======================= Public API (gọi qua Binder) =======================

    /**
     * Phát bài hát tại currentIndex.
     * Giải phóng MediaPlayer cũ nếu có, tạo mới và bắt đầu phát.
     */
    public void play() {
        if (songList == null || songList.isEmpty()) return;
        Song song = songList.get(currentIndex);

        // Nếu bài hát không có rawResId hợp lệ, bỏ qua (tránh crash khi mock)
        if (song.getRawResId() == 0) {
            Log.w(TAG, "play: rawResId = 0, bỏ qua bài '" + song.getTitle() + "'");
            isPrepared = false;
            return;
        }

        releasePlayer();
        mediaPlayer = MediaPlayer.create(this, song.getRawResId());
        if (mediaPlayer != null) {
            isPrepared = true;
            mediaPlayer.start();
            // Tự động chuyển bài khi kết thúc
            mediaPlayer.setOnCompletionListener(mp -> next());
            updateNotification();
            Log.d(TAG, "play: đang phát '" + song.getTitle() + "'");
        } else {
            isPrepared = false;
            Log.e(TAG, "play: MediaPlayer.create() trả về null cho rawResId=" + song.getRawResId());
        }
    }

    /** Tạm dừng nếu đang phát. */
    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "pause: tạm dừng");
        }
    }

    /** Tiếp tục phát nếu đang pause. */
    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            Log.d(TAG, "resume: tiếp tục phát");
        }
    }

    /** Chuyển sang bài tiếp theo (vòng tròn). */
    public void next() {
        currentIndex = (currentIndex + 1) % songList.size();
        play();
    }

    /** Quay lại bài trước (vòng tròn). */
    public void previous() {
        currentIndex = (currentIndex - 1 + songList.size()) % songList.size();
        play();
    }

    /**
     * Phát bài hát tại vị trí index bất kỳ.
     * Dùng khi người dùng bấm trực tiếp vào item trong RecyclerView.
     */
    public void playAt(int index) {
        if (index < 0 || index >= songList.size()) return;
        currentIndex = index;
        play();
    }

    /** Di chuyển vị trí phát (ms). */
    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    /** Trả về true nếu nhạc đang phát. */
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    /** Vị trí hiện tại (ms). Trả 0 nếu chưa ready. */
    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    /** Tổng thời gian bài hát (ms). Trả 0 nếu chưa ready. */
    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    /** Index bài đang phát trong songList. */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /** Bài hát đang phát. */
    public Song getCurrentSong() {
        if (songList != null && !songList.isEmpty()) {
            return songList.get(currentIndex);
        }
        return null;
    }

    /** Toàn bộ danh sách bài hát. */
    public List<Song> getSongList() {
        return songList;
    }

    // ======================= Helpers =======================

    /** Giải phóng MediaPlayer an toàn. */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    /** Tạo NotificationChannel (bắt buộc với Android 8+). */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhạc tập luyện",
                    NotificationManager.IMPORTANCE_LOW  // LOW = không phát âm thanh thông báo
            );
            channel.setDescription("Hiển thị bài hát đang phát trong lúc tập luyện");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    /** Build Notification hiển thị tên bài đang phát. */
    private Notification buildNotification() {
        Song current = getCurrentSong();
        String title = (current != null) ? current.getTitle() : "Đang phát nhạc";
        String artist = (current != null) ? current.getArtist() : "";

        // Tap notification → mở lại ExerciseActivity
        Intent openIntent = new Intent(this, ExerciseActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_workout_music)
                .setContentIntent(pendingIntent)
                .setOngoing(true)   // Người dùng không thể vuốt xoá
                .setSilent(true)
                .build();
    }

    /** Cập nhật nội dung Notification khi chuyển bài. */
    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification());
    }
}
