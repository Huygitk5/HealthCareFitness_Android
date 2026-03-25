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

    @Override
    public void onCreate() {
        super.onCreate();
        songList = getMockSongs();
        createNotificationChannel();
        Log.d(TAG, "onCreate: MusicService created");
    }

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
        return true;
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

    public void play() {
        if (songList == null || songList.isEmpty()) return;

        Song song = songList.get(currentIndex);
        if (song.getRawResId() == 0) {
            Log.w(TAG, "play: rawResId = 0, skip song '" + song.getTitle() + "'");
            isPrepared = false;
            return;
        }

        releasePlayer();
        mediaPlayer = MediaPlayer.create(this, song.getRawResId());
        if (mediaPlayer == null) {
            isPrepared = false;
            Log.e(TAG, "play: MediaPlayer.create() returned null for rawResId=" + song.getRawResId());
            return;
        }

        isPrepared = true;
        mediaPlayer.setOnCompletionListener(mp -> handleSongCompletion());
        mediaPlayer.start();
        updateNotification();
        Log.d(TAG, "play: playing '" + song.getTitle() + "'");
    }

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

    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null && isPrepared) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public Song getCurrentSong() {
        if (songList != null && !songList.isEmpty()) {
            return songList.get(currentIndex);
        }
        return null;
    }

    public List<Song> getSongList() {
        return songList;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public int cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        Log.d(TAG, "cycleRepeatMode: repeatMode=" + repeatMode);
        return repeatMode;
    }

    public boolean isBackgroundPlaybackEnabled() {
        return backgroundPlaybackEnabled;
    }

    public void setBackgroundPlaybackEnabled(boolean enabled) {
        backgroundPlaybackEnabled = enabled;
        Log.d(TAG, "setBackgroundPlaybackEnabled: " + enabled);
    }

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

    private void releasePlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhac tap luyen",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Hien thi bai hat dang phat trong luc tap luyen");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Song current = getCurrentSong();
        String title = current != null ? current.getTitle() : "Dang phat nhac";
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
