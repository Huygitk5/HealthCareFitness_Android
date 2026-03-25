package com.hcmute.edu.vn.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.adapter.SongAdapter;
import com.hcmute.edu.vn.model.Song;
import com.hcmute.edu.vn.service.MusicService;

import java.util.List;
import java.util.Locale;

public class MusicBottomSheetFragment extends BottomSheetDialogFragment {

    // ── Binder từ ServiceConnection của ExerciseActivity ──────────────────────
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

    // ── Views — State 1: Volume ────────────────────────────────────────────────
    private LinearLayout layoutVolume;
    private SeekBar      seekBarVolume;

    // ── Views — State 2: Music SeekBar + Song List ────────────────────────────
    private LinearLayout layoutSeekbar;
    private LinearLayout layoutSongList;
    private SeekBar      seekBarMusic;
    private TextView     tvSeekStart, tvSeekEnd;
    private RecyclerView rvSongList;
    private SongAdapter  songAdapter;

    // ── State flag ─────────────────────────────────────────────────────────────
    private boolean isFullPlayer = false;
    private int lastKnownSongIndex = Integer.MIN_VALUE;
    private int lastKnownRepeatMode = Integer.MIN_VALUE;
    private boolean lastKnownPlaying = false;
    private boolean userIsSeeking = false; // true khi người dùng đang kéo seekBarMusic

    // ── Handler cập nhật SeekBar nhạc định kỳ (500ms) ─────────────────────────
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seekBarRunnable = new Runnable() {
        @Override
        public void run() {
            updateMusicSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    // ══════════════════════════════════════════════════════════════════════════
    // Factory
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Factory method — truyền MusicBinder qua setter thay vì Bundle
     * vì IBinder không implement Parcelable/Serializable.
     */
    public static MusicBottomSheetFragment newInstance(MusicService.MusicBinder binder) {
        MusicBottomSheetFragment f = new MusicBottomSheetFragment();
        f.musicBinder = binder;
        return f;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

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

        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        bindViews(view);
        setupRecyclerView();
        setupListeners();
        refreshUI();                         // Sync UI với trạng thái service ngay lần đầu
        setupVolumeControl();                // Khởi tạo Volume SeekBar
        handler.post(seekBarRunnable);       // Bắt đầu vòng lặp cập nhật SeekBar nhạc
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        handler.removeCallbacks(seekBarRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(seekBarRunnable);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // View Binding
    // ══════════════════════════════════════════════════════════════════════════

    private void bindViews(View v) {
        rootSheetLayout   = v.findViewById(R.id.rootSheetLayout);
        tvSheetTitle      = v.findViewById(R.id.tvSheetTitle);
        ivAlbumArt        = v.findViewById(R.id.ivAlbumArt);
        ivArrowState      = v.findViewById(R.id.ivArrowState);
        tvCurrentSongTitle = v.findViewById(R.id.tvCurrentSongTitle);
        tvSongTime        = v.findViewById(R.id.tvSongTime);
        btnPlayPause      = v.findViewById(R.id.btnPlayPause);
        btnMusicNext      = v.findViewById(R.id.btnMusicNext);
        btnMusicPrevious  = v.findViewById(R.id.btnMusicPrevious);
        btnPlaylist       = v.findViewById(R.id.btnPlaylist);
        btnRepeat         = v.findViewById(R.id.btnRepeat);
        switchMusic       = v.findViewById(R.id.switchMusic);

        // State 1
        layoutVolume      = v.findViewById(R.id.layoutVolume);
        seekBarVolume     = v.findViewById(R.id.seekBarVolume);

        // State 2
        layoutSeekbar     = v.findViewById(R.id.layoutSeekbar);
        layoutSongList    = v.findViewById(R.id.layoutSongList);
        seekBarMusic      = v.findViewById(R.id.seekBarMusic);
        tvSeekStart       = v.findViewById(R.id.tvSeekStart);
        tvSeekEnd         = v.findViewById(R.id.tvSeekEnd);
        rvSongList        = v.findViewById(R.id.rvSongList);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RecyclerView
    // ══════════════════════════════════════════════════════════════════════════

    private void setupRecyclerView() {
        MusicService service = getService();
        if (service == null) return;

        List<Song> songs = service.getSongList();
        songAdapter = new SongAdapter(songs, position -> {
            service.playAt(position);
            songAdapter.setCurrentPlayingIndex(position);
            refreshUI();
        });
        songAdapter.setCurrentPlayingIndex(service.getCurrentIndex());

        rvSongList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSongList.setAdapter(songAdapter);
        rvSongList.setHasFixedSize(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Volume Control (State 1)
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Khởi tạo Volume SeekBar dùng AudioManager.STREAM_MUSIC.
     *
     * - max  = audioManager.getStreamMaxVolume(STREAM_MUSIC)
     * - progress hiện tại = getStreamVolume(STREAM_MUSIC)
     * - onStopTrackingTouch → setStreamVolume() để thay đổi âm lượng thực của thiết bị
     */
    private void setupVolumeControl() {
        if (audioManager == null) return;

        int maxVol     = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        seekBarVolume.setMax(maxVol);
        seekBarVolume.setProgress(currentVol);

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Thay đổi âm lượng STREAM_MUSIC ngay khi kéo (real-time feedback)
                    audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            progress,
                            0 // không hiển thị UI volume system
                    );
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar)  { }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // State Switching
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Chuyển sang State 2 (Full Player).
     * - Ẩn volume slider, hiện music seekbar + danh sách.
     * - Đổi tiêu đề header.
     * - Mở rộng BottomSheet lên toàn màn hình.
     * - Dùng TransitionManager để animate thay đổi visibility.
     */
    private void switchToFullPlayer() {
        if (isFullPlayer) return;
        isFullPlayer = true;

        // Animate transition
        TransitionManager.beginDelayedTransition(rootSheetLayout);

        layoutVolume.setVisibility(View.GONE);
        layoutSeekbar.setVisibility(View.VISIBLE);
        layoutSongList.setVisibility(View.VISIBLE);

        tvSheetTitle.setText("Danh sách phát");
        ivArrowState.setImageResource(android.R.drawable.arrow_up_float);

        // Mở rộng BottomSheet
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // Cập nhật nhãn thời gian seekbar ngay lập tức
        updateMusicSeekBar();
        if (songAdapter != null) {
            songAdapter.setCurrentPlayingIndex(getService() != null
                    ? getService().getCurrentIndex() : -1);
        }
    }

    /**
     * Quay về State 1 (Mini Player).
     * - Ẩn music seekbar + danh sách, hiện volume slider.
     * - Khôi phục tiêu đề header.
     * - Thu nhỏ BottomSheet.
     */
    private void switchToMiniPlayer() {
        if (!isFullPlayer) return;
        isFullPlayer = false;

        TransitionManager.beginDelayedTransition(rootSheetLayout);

        layoutVolume.setVisibility(View.VISIBLE);
        layoutSeekbar.setVisibility(View.GONE);
        layoutSongList.setVisibility(View.GONE);

        tvSheetTitle.setText("Âm nhạc ");
        ivArrowState.setImageResource(android.R.drawable.arrow_down_float);

        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        BottomSheetBehavior<?> behavior = dialog.getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Listeners
    // ══════════════════════════════════════════════════════════════════════════

    private void setupListeners() {
        // Đóng BottomSheet
        requireView().findViewById(R.id.btnCloseSheet).setOnClickListener(v -> {
            if (isFullPlayer) {
                switchToMiniPlayer();   // Nhấn X ở State 2 → về State 1 trước
            } else {
                dismiss();             // Nhấn X ở State 1 → đóng hẳn
            }
        });

        // Toggle giữa State 1 và State 2
        View.OnClickListener toggleStateListener = v -> {
            if (isFullPlayer) {
                switchToMiniPlayer();
            } else {
                switchToFullPlayer();
            }
        };

        // Song info row: nhấn → toggle trạng thái
        requireView().findViewById(R.id.layoutSongInfo)
                .setOnClickListener(toggleStateListener);

        // Nút Playlist → toggle trạng thái
        btnPlaylist.setOnClickListener(toggleStateListener);

        // Play / Pause (toggle)
        btnPlayPause.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc == null) return;
            if (svc.isPlaying()) {
                svc.pause();
            } else {
                // Nếu chưa có file nhạc hợp lệ → play(); nếu đang pause → resume()
                if (svc.getDuration() == 0) {
                    svc.play();
                } else {
                    svc.resume();
                }
            }
            refreshUI();
        });

        // Next
        btnMusicNext.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc == null) return;
            svc.next();
            refreshUI();
        });

        // Previous
        btnMusicPrevious.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc == null) return;
            svc.previous();
            refreshUI();
        });

        // Repeat toggle (UI only — lưu state isRepeat để dùng sau)
        btnRepeat.setOnClickListener(v -> {
            // Tag dùng làm flag: null = off, non-null = on
            MusicService svc = getService();
            if (svc == null) return;
            int repeatMode = svc.cycleRepeatMode();
            showRepeatModeToast(repeatMode);
            refreshUI();
        });
        btnRepeat.setAlpha(0.4f); // Mặc định: repeat off

        // Music SeekBar (State 2): người dùng kéo → seekTo()
        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                // Callback từ code → bỏ qua
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
                userIsSeeking = true; // Tạm dừng update tự động khi đang kéo
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                userIsSeeking = false;
                MusicService svc = getService();
                if (svc != null && svc.getDuration() > 0) {
                    int seekMs = (int) ((sb.getProgress() / 100.0f) * svc.getDuration());
                    svc.seekTo(seekMs);
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI Update
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Làm mới toàn bộ Mini Player UI theo trạng thái service hiện tại.
     * Gọi sau mỗi thao tác play/pause/next/previous.
     */
    private void refreshUI() {
        MusicService svc = getService();
        if (svc == null) return;

        // Album art + tên bài
        Song song = svc.getCurrentSong();
        if (song != null) {
            tvCurrentSongTitle.setText(song.getTitle());
            ivAlbumArt.setImageResource(
                    song.getCoverResId() != 0 ? song.getCoverResId() : R.drawable.workout_1
            );
        }

        // Thời gian hiển thị ở song info row
        int pos = svc.getCurrentPosition();
        int dur = svc.getDuration();
        tvSongTime.setText(formatTime(pos) + "/" + formatTime(dur));

        // Icon Play/Pause
        boolean playing = svc.isPlaying();
        btnPlayPause.setImageResource(
                playing ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );
        updateRepeatModeUi(svc.getRepeatMode());
        updatePreviousButtonUi(svc);

        // Switch: đồng bộ với trạng thái service, tắt listener trước để tránh vòng lặp
        switchMusic.setOnCheckedChangeListener(null);
        switchMusic.setChecked(svc.isBackgroundPlaybackEnabled());
        switchMusic.setOnCheckedChangeListener((btn, isChecked) -> {
            MusicService s = getService();
            if (s == null) return;
            s.setBackgroundPlaybackEnabled(isChecked);
            showBackgroundPlaybackToast(isChecked);
            refreshUI();
        });

        // Highlight bài đang phát trong danh sách (nếu adapter đã khởi tạo)
        if (songAdapter != null) {
            songAdapter.setCurrentPlayingIndex(svc.getCurrentIndex());
        }
        capturePlaybackSnapshot(svc);
    }

    /**
     * Cập nhật Music SeekBar (State 2) và nhãn thời gian.
     * Chạy mỗi 500ms qua Handler, bỏ qua khi người dùng đang kéo.
     */
    private void updateMusicSeekBar() {
        if (userIsSeeking || !isAdded()) return;
        MusicService svc = getService();
        if (svc == null) return;

        if (hasPlaybackSnapshotChanged(svc)) {
            refreshUI();
            svc = getService();
            if (svc == null) return;
        }

        int position = svc.getCurrentPosition();
        int duration = svc.getDuration();

        if (duration > 0) {
            int progress = (int) ((position / (float) duration) * 100);
            seekBarMusic.setProgress(progress);
        }

        tvSeekStart.setText(formatTime(position));
        tvSeekEnd.setText(formatTime(duration));

        // Cập nhật luôn tvSongTime ở song info row (dù đang ở state nào)
        tvSongTime.setText(formatTime(position) + "/" + formatTime(duration));
        capturePlaybackSnapshot(svc);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Chuyển milliseconds → chuỗi "mm:ss". */
    private void updateRepeatModeUi(int repeatMode) {
        if (!isAdded()) return;

        int tintResId;
        int iconResId = android.R.drawable.ic_menu_rotate;
        String contentDescription;

        switch (repeatMode) {
            case MusicService.REPEAT_MODE_ONE:
                tintResId = R.color.orange_warning;
                iconResId = android.R.drawable.ic_menu_revert;
                contentDescription = "Lặp lại bài hiện tại";
                break;
            case MusicService.REPEAT_MODE_ALL:
                tintResId = R.color.primary_green;
                contentDescription = "Lặp lại toàn bộ danh sách";
                break;
            case MusicService.REPEAT_MODE_OFF:
            default:
                tintResId = R.color.grey_locked;
                contentDescription = "Tắt lặp lại";
                break;
        }

        int tintColor = ContextCompat.getColor(requireContext(), tintResId);
        btnRepeat.setImageResource(iconResId);
        btnRepeat.setImageTintList(ColorStateList.valueOf(tintColor));
        btnRepeat.setAlpha(repeatMode == MusicService.REPEAT_MODE_OFF ? 0.45f : 1.0f);
        btnRepeat.setContentDescription(contentDescription);
    }

    private void updatePreviousButtonUi(MusicService svc) {
        if (!isAdded()) return;

        boolean canGoPrevious = false;
        List<Song> songs = svc.getSongList();
        if (songs != null && !songs.isEmpty()) {
            canGoPrevious = svc.getCurrentIndex() > 0
                    || svc.getRepeatMode() == MusicService.REPEAT_MODE_ALL;
        }

        int tintColor = ContextCompat.getColor(
                requireContext(),
                canGoPrevious ? android.R.color.black : R.color.grey_locked
        );

        btnMusicPrevious.setEnabled(canGoPrevious);
        btnMusicPrevious.setAlpha(canGoPrevious ? 1.0f : 0.35f);
        btnMusicPrevious.setImageTintList(ColorStateList.valueOf(tintColor));
    }

    private void showRepeatModeToast(int repeatMode) {
        if (!isAdded()) return;

        String message;
        switch (repeatMode) {
            case MusicService.REPEAT_MODE_ONE:
                message = "Đang lặp lại bài hiện tại";
                break;
            case MusicService.REPEAT_MODE_ALL:
                message = "Đang lặp lại toàn bộ danh sách";
                break;
            case MusicService.REPEAT_MODE_OFF:
            default:
                message = "Đã tắt lặp lại";
                break;
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showBackgroundPlaybackToast(boolean enabled) {
        if (!isAdded()) return;

        String message = enabled
                ? "Đã bật chạy nhạc nền"
                : "Đã tắt chạy nhạc nền";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasPlaybackSnapshotChanged(MusicService svc) {
        return lastKnownSongIndex != svc.getCurrentIndex()
                || lastKnownPlaying != svc.isPlaying()
                || lastKnownRepeatMode != svc.getRepeatMode();
    }

    private void capturePlaybackSnapshot(MusicService svc) {
        lastKnownSongIndex = svc.getCurrentIndex();
        lastKnownPlaying = svc.isPlaying();
        lastKnownRepeatMode = svc.getRepeatMode();
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int totalSec = ms / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d",
                totalSec / 60, totalSec % 60);
    }

    /** Lấy MusicService an toàn từ Binder (null-safe). */
    @Nullable
    private MusicService getService() {
        if (musicBinder == null) return null;
        return musicBinder.getService();
    }
}
