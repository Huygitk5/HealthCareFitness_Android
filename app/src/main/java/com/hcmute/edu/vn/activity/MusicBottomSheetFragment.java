package com.hcmute.edu.vn.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.SongAdapter;
import com.hcmute.edu.vn.model.Song;
import com.hcmute.edu.vn.service.MusicService;

import java.util.List;
import java.util.Locale;

/**
 * MusicBottomSheetFragment — BottomSheetDialogFragment hiển thị giao diện điều khiển nhạc.
 *
 * Cách dùng:
 *   MusicBottomSheetFragment sheet = MusicBottomSheetFragment.newInstance(musicBinder);
 *   sheet.show(getSupportFragmentManager(), sheet.getTag());
 *
 * Fragment nhận MusicService.MusicBinder từ ExerciseActivity (đã bound) và dùng
 * Handler để cập nhật SeekBar + thời gian mỗi 500ms.
 */
public class MusicBottomSheetFragment extends BottomSheetDialogFragment {

    // Binder lấy từ ServiceConnection trong ExerciseActivity
    private MusicService.MusicBinder musicBinder;

    // ── System service ─────────────────────────────────────────────────────────
    private AudioManager audioManager;

    // ── Views — Mini Player (luôn hiển thị) ───────────────────────────────────
    private LinearLayout rootSheetLayout;
    private TextView     tvSheetTitle;
    private ImageView    ivAlbumArt, ivArrowState;
    private TextView     tvCurrentSongTitle, tvSongTime;
    private ImageButton  btnPlayPause, btnMusicNext, btnMusicPrevious;
    private ImageButton  btnPlaylist, btnRepeat;
    private SwitchCompat switchMusic;
    private RecyclerView rvSongList;
    private SongAdapter songAdapter;

    // Handler cập nhật SeekBar định kỳ (mỗi 500ms)
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekBarRunnable = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    private boolean isRepeat = false;
    private boolean userIsSeeking = false; // true khi người dùng đang kéo SeekBar

    /**
     * Factory method: tạo instance với MusicBinder.
     * Binder được truyền qua setter vì Fragment không nên nhận object qua Bundle.
     */
    public static MusicBottomSheetFragment newInstance(MusicService.MusicBinder binder) {
        MusicBottomSheetFragment fragment = new MusicBottomSheetFragment();
        fragment.musicBinder = binder;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_music, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupRecyclerView();
        setupListeners();
        refreshUI();                        // Cập nhật UI ngay lần đầu
        handler.post(updateSeekBarRunnable); // Bắt đầu vòng lặp cập nhật SeekBar
    }

    // ======================= View Binding =======================

    private void bindViews(View v) {
        ivAlbumArt       = v.findViewById(R.id.ivAlbumArt);
        tvSongTitle      = v.findViewById(R.id.tvCurrentSongTitle);
        tvSongTime       = v.findViewById(R.id.tvSongTime);
        btnPlayPause     = v.findViewById(R.id.btnPlayPause);
        btnNext          = v.findViewById(R.id.btnMusicNext);
        btnPrevious      = v.findViewById(R.id.btnMusicPrevious);
        btnPlaylist      = v.findViewById(R.id.btnPlaylist);
        btnRepeat        = v.findViewById(R.id.btnRepeat);
        seekBar          = v.findViewById(R.id.seekBarMusic);
        switchMusic      = v.findViewById(R.id.switchMusic);
        rvSongList       = v.findViewById(R.id.rvSongList);
    }

    // ======================= RecyclerView =======================

    private void setupRecyclerView() {
        MusicService service = getService();
        if (service == null) return;

        List<Song> songs = service.getSongList();
        songAdapter = new SongAdapter(songs, position -> {
            // Người dùng chọn bài → phát luôn
            service.playAt(position);
            songAdapter.setCurrentPlayingIndex(position);
            refreshUI();
        });
        songAdapter.setCurrentPlayingIndex(service.getCurrentIndex());

        rvSongList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongList.setAdapter(songAdapter);
        rvSongList.setHasFixedSize(true);
    }

    // ======================= Listeners =======================

    private void setupListeners() {
        // Nút đóng BottomSheet
        requireView().findViewById(R.id.btnCloseSheet).setOnClickListener(v -> dismiss());

        // Play / Pause
        btnPlayPause.setOnClickListener(v -> {
            MusicService service = getService();
            if (service == null) return;
            if (service.isPlaying()) {
                service.pause();
            } else {
                // Nếu chưa có bài nào → phát bài đầu
                if (service.getCurrentSong() == null || service.getDuration() == 0) {
                    service.play();
                } else {
                    service.resume();
                }
            }
            updatePlayPauseIcon(service.isPlaying());
        });

        // Next
        btnNext.setOnClickListener(v -> {
            MusicService service = getService();
            if (service == null) return;
            service.next();
            refreshUI();
        });

        // Previous
        btnPrevious.setOnClickListener(v -> {
            MusicService service = getService();
            if (service == null) return;
            service.previous();
            refreshUI();
        });

        // Repeat toggle
        btnRepeat.setOnClickListener(v -> {
            isRepeat = !isRepeat;
            btnRepeat.setAlpha(isRepeat ? 1.0f : 0.4f);
        });

        // SeekBar: người dùng kéo → seekTo()
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Không làm gì khi update từ code
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userIsSeeking = true; // Tạm dừng update tự động
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userIsSeeking = false;
                MusicService service = getService();
                if (service != null) {
                    int duration = service.getDuration();
                    int seekMs = (int) ((seekBar.getProgress() / 100.0f) * duration);
                    service.seekTo(seekMs);
                }
            }
        });
    }

    // ======================= UI Updates =======================

    /**
     * Làm mới toàn bộ UI dựa trên trạng thái Service.
     * Gọi sau mỗi thao tác play/next/previous.
     */
    private void refreshUI() {
        MusicService service = getService();
        if (service == null) return;

        Song song = service.getCurrentSong();
        if (song != null) {
            tvSongTitle.setText(song.getTitle());
            if (song.getCoverResId() != 0) {
                ivAlbumArt.setImageResource(song.getCoverResId());
            } else {
                ivAlbumArt.setImageResource(R.drawable.workout_1);
            }
        }

        boolean playing = service.isPlaying();
        updatePlayPauseIcon(playing);
        // Đồng bộ trạng thái switch với service — tắt listener trước để tránh vòng lặp vô hạn
        switchMusic.setOnCheckedChangeListener(null);
        switchMusic.setChecked(playing);
        switchMusic.setOnCheckedChangeListener((btn, isChecked) -> {
            MusicService svc = getService();
            if (svc == null) return;
            if (isChecked) {
                svc.resume();
            } else {
                svc.pause();
            }
            updatePlayPauseIcon(isChecked);
        });
        if (songAdapter != null) {
            songAdapter.setCurrentPlayingIndex(service.getCurrentIndex());
        }
    }

    /**
     * Cập nhật SeekBar và nhãn thời gian. Chạy mỗi 500ms qua Handler.
     */
    private void updateSeekBar() {
        if (userIsSeeking) return;
        MusicService service = getService();
        if (service == null || !isAdded()) return;

        int duration  = service.getDuration();
        int position  = service.getCurrentPosition();

        if (duration > 0) {
            int progress = (int) ((position / (float) duration) * 100);
            seekBar.setProgress(progress);
        }

        tvSongTime.setText(formatTime(position) + "/" + formatTime(duration));
    }

    /** Đổi icon play/pause theo trạng thái. */
    private void updatePlayPauseIcon(boolean playing) {
        if (playing) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    /** Format milliseconds → "mm:ss". */
    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }

    /** Helper trả về MusicService từ Binder an toàn (null-safe). */
    @Nullable
    private MusicService getService() {
        if (musicBinder == null) return null;
        return musicBinder.getService();
    }

    // ======================= Lifecycle =======================

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Dừng vòng lặp cập nhật khi BottomSheet đóng
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateSeekBarRunnable);
    }
}
