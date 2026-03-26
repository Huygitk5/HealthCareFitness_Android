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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String TAG = "MusicService";
    private static final String CHANNEL_ID = "music_service_channel";
    private static final int NOTIF_ID = 101;

    public static final int REPEAT_MODE_OFF = 0;
    public static final int REPEAT_MODE_ONE = 1;
    public static final int REPEAT_MODE_ALL = 2;

    public static List<Song> getMockSongs() {
        List<Song> list = new ArrayList<>();
        list.add(new Song(1, "Fresh Day", "Workout Beats", R.raw.fresh_day, 0));
        list.add(new Song(2, "Energy Boost", "Gym Vibes", R.raw.energy_boost, 0));
        list.add(new Song(3, "Push The Limit", "FitRhythm", R.raw.push_the_limit, 0));
        list.add(new Song(4, "Morning Run", "ActiveSound", R.raw.morning_run, 0));
        list.add(new Song(5, "Power Up", "BeatFit", R.raw.power_up, 0));
        return list;
    }

    private final IBinder binder = new MusicBinder();
    private MediaPlayer mediaPlayer;
    private List<Song> songList;
    private int currentIndex = 0;
    private int repeatMode = REPEAT_MODE_OFF;
    private boolean backgroundPlaybackEnabled = false;
    private boolean isPrepared = false;

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onCreate() {
        super.onCreate();
        songList = getMockSongs();
        createNotificationChannel();
        Log.d(TAG, "onCreate: MusicService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true; // onRebind được gọi khi client bind lại
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releasePlayer();
        Log.d(TAG, "onDestroy: MusicService destroyed");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Playback — CORE (hỗ trợ cả rawResId và URL)
    // ══════════════════════════════════════════════════════════════════════════
    /**
     * Phát bài hát tại currentIndex.
     * - Nếu bài hát có URL (Supabase / file local) → dùng setDataSource()
     * - Nếu bài hát có rawResId               → dùng MediaPlayer.create()
     */
    public void play() {
        if (songList == null || songList.isEmpty()) return;

        Song song = songList.get(currentIndex);

        // Giải phóng player cũ trước khi tạo mới — tránh Memory Leak
        releasePlayer();

        if (song.isUrlBased()) {
            // ── Nguồn URL (Supabase Storage hoặc đường dẫn file) ─────────────
            playFromUrl(song);
        } else if (song.isRawResource()) {
            // ── Nguồn Raw Resource (nhạc đóng gói sẵn trong app) ─────────────
            playFromRaw(song);
        } else {
            Log.w(TAG, "play: bài '" + song.getTitle() + "' không có nguồn nhạc hợp lệ");
            isPrepared = false;
        }
    }

    /** Phát từ URL — dùng setDataSource + prepareAsync để không block Main Thread */
    private void playFromUrl(Song song) {
        try {
            mediaPlayer = new MediaPlayer();

            // QUAN TRỌNG: phải reset() trước setDataSource để tránh IllegalStateException
            mediaPlayer.reset();
            mediaPlayer.setDataSource(song.getUrl());

            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                mp.start();
                updateNotification();
                Log.d(TAG, "playFromUrl: đang phát '" + song.getTitle() + "'");
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "playFromUrl: lỗi phát nhạc what=" + what + " extra=" + extra);
                isPrepared = false;
                return true; // true = đã xử lý lỗi, không gọi OnCompletionListener
            });

            mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());

            // prepareAsync() — không block UI thread
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "playFromUrl: IOException - " + e.getMessage());
            isPrepared = false;
            releasePlayer();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "playFromUrl: URL không hợp lệ - " + e.getMessage());
            isPrepared = false;
            releasePlayer();
        }
    }

    /** Phát từ Raw Resource — dùng MediaPlayer.create() (sync, chỉ dùng cho file nhỏ local) */
    private void playFromRaw(Song song) {
        mediaPlayer = MediaPlayer.create(this, song.getRawResId());
        if (mediaPlayer == null) {
            Log.e(TAG, "playFromRaw: MediaPlayer.create() trả về null cho rawResId=" + song.getRawResId());
            isPrepared = false;
            return;
        }
        isPrepared = true;
        mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
        mediaPlayer.start();
        updateNotification();
        Log.d(TAG, "playFromRaw: đang phát '" + song.getTitle() + "'");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Playback Controls
    // ══════════════════════════════════════════════════════════════════════════

    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d(TAG, "pause: paused");
        }
    }

    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            int duration = mediaPlayer.getDuration();
            if (duration > 0 && mediaPlayer.getCurrentPosition() >= Math.max(0, duration - 500)) {
                mediaPlayer.seekTo(0);
            }
            mediaPlayer.start();
            updateNotification();
            Log.d(TAG, "resume: resumed");
        }
    }

    public void next() {
        if (songList == null || songList.isEmpty()) return;
        currentIndex = (currentIndex + 1) % songList.size();
        play();
    }

    public void previous() {
        if (songList == null || songList.isEmpty()) return;

        if (currentIndex > 0) {
            currentIndex--;
            play();
            return;
        }

        if (repeatMode == REPEAT_MODE_ALL) {
            currentIndex = songList.size() - 1;
            play();
            return;
        }

        restartCurrentSong();
    }

    public void playAt(int index) {
        if (songList == null || index < 0 || index >= songList.size()) return;
        currentIndex = index;
        play();
    }

    public void seekTo(int positionMs) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(positionMs);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Song List Management
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Thêm một bài hát upload từ Supabase vào cuối danh sách.
     * Gọi từ MusicBottomSheetFragment sau khi upload thành công.
     */
    public void addSong(Song song) {
        if (songList == null) songList = new ArrayList<>();
        songList.add(song);
        Log.d(TAG, "addSong: đã thêm '" + song.getTitle() + "', tổng=" + songList.size());
    }

    /**
     * Thay thế toàn bộ danh sách nhạc (ví dụ sau khi load từ Supabase).
     * Giữ nguyên bài đang phát nếu vẫn còn trong list mới.
     */
    public void setSongList(List<Song> newList) {
        if (newList == null || newList.isEmpty()) return;
        songList = newList;
        // Reset index về 0 nếu index hiện tại vượt quá kích thước list mới
        if (currentIndex >= songList.size()) {
            currentIndex = 0;
        }
        Log.d(TAG, "setSongList: tổng=" + songList.size() + " bài");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Getters
    // ══════════════════════════════════════════════════════════════════════════

    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            try { return mediaPlayer.getCurrentPosition(); }
            catch (IllegalStateException e) { return 0; }
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            try { return mediaPlayer.getDuration(); }
            catch (IllegalStateException e) { return 0; }
        }
        return 0;
    }

    public int getCurrentIndex()    { return currentIndex; }
    public List<Song> getSongList() { return songList; }
    public int getRepeatMode()      { return repeatMode; }

    public Song getCurrentSong() {
        if (songList != null && !songList.isEmpty() && currentIndex < songList.size()) {
            return songList.get(currentIndex);
        }
        return null;
    }

    public int cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        return repeatMode;
    }

    public boolean isBackgroundPlaybackEnabled()            { return backgroundPlaybackEnabled; }
    public void setBackgroundPlaybackEnabled(boolean val)   { backgroundPlaybackEnabled = val; }

    // ══════════════════════════════════════════════════════════════════════════
    // Internal Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void handleSongCompletion() {
        if (songList == null || songList.isEmpty()) return;

        if (repeatMode == REPEAT_MODE_ONE) {
            play();
            return;
        }

        if (currentIndex < songList.size() - 1) {
            currentIndex++;
            play();
            return;
        }

        if (repeatMode == REPEAT_MODE_ALL) {
            currentIndex = 0;
            play();
            return;
        }

        updateNotification();
        Log.d(TAG, "handleSongCompletion: reached last song, repeat is off");
    }

    private void restartCurrentSong() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(0);
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
            updateNotification();
            return;
        }
        play();
    }

    /** Giải phóng MediaPlayer an toàn — ngăn Memory Leak */
    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            } catch (IllegalStateException ignored) {}
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhạc tập luyện",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hiển thị bài hát đang phát trong lúc tập luyện");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Song current = getCurrentSong();
        String title = current != null ? current.getTitle() : "Đang phát nhạcen";
        String artist = current != null ? current.getArtist() : "";

        Intent openIntent = new Intent(this, ExerciseActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(artist)
                .setSmallIcon(R.drawable.ic_workout_music)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID, buildNotification());
        }
    }
}
