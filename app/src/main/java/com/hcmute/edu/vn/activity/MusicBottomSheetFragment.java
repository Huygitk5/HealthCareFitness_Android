package com.hcmute.edu.vn.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.hcmute.edu.vn.database.SupabaseClient;
import com.hcmute.edu.vn.database.SupabaseMusicApiService;
import com.hcmute.edu.vn.model.Song;
import com.hcmute.edu.vn.model.SongInsertRequest;
import com.hcmute.edu.vn.model.UserSongInsert;
import com.hcmute.edu.vn.service.MusicService;
import com.hcmute.edu.vn.util.SupabaseSessionManager;
import com.hcmute.edu.vn.util.SupabaseStorageUploader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MusicBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "MusicBottomSheet";

    // ── Binder từ ExerciseActivity ────────────────────────────────────────────
    private MusicService.MusicBinder musicBinder;

    // ── System service ─────────────────────────────────────────────────────────
    private AudioManager audioManager;

    // ── User info (lấy từ SharedPreferences) ──────────────────────────────────
    private String userId;

    // ── Views — Mini Player ────────────────────────────────────────────────────
    private LinearLayout rootSheetLayout;
    private TextView     tvSheetTitle;
    private ImageView    ivAlbumArt, ivArrowState;
    private TextView     tvCurrentSongTitle, tvSongTime;
    private ImageButton  btnPlayPause, btnMusicNext, btnMusicPrevious;
    private ImageButton  btnPlaylist, btnRepeat;
    private SwitchCompat switchMusic;

    // ── Views — Volume ─────────────────────────────────────────────────────────
    private LinearLayout layoutVolume;
    private SeekBar      seekBarVolume;

    // ── Views — Full Player ────────────────────────────────────────────────────
    private LinearLayout layoutSeekbar;
    private LinearLayout layoutSongList;
    private SeekBar      seekBarMusic;
    private TextView     tvSeekStart, tvSeekEnd;
    private RecyclerView rvSongList;
    private SongAdapter  songAdapter;

    // ── Views — Upload ─────────────────────────────────────────────────────────
    private ImageButton  btnAddMusic;      // Nút "+" thêm nhạc
    private ProgressBar progressUpload;   // Hiện khi đang upload

    // ── State ──────────────────────────────────────────────────────────────────
    private boolean isFullPlayer       = false;
    private boolean userIsSeeking      = false;
    private int lastKnownSongIndex     = Integer.MIN_VALUE;
    private int lastKnownRepeatMode    = Integer.MIN_VALUE;
    private boolean lastKnownPlaying   = false;

    // ── Handler cập nhật SeekBar ───────────────────────────────────────────────
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seekBarRunnable = new Runnable() {
        @Override public void run() {
            updateMusicSeekBar();
            handler.postDelayed(this, 500);
        }
    };

    // ── File Picker Launcher ───────────────────────────────────────────────────
    private ActivityResultLauncher<String> pickAudioLauncher;

    // ══════════════════════════════════════════════════════════════════════════
    // Factory
    // ══════════════════════════════════════════════════════════════════════════

    public static MusicBottomSheetFragment newInstance(MusicService.MusicBinder binder) {
        MusicBottomSheetFragment f = new MusicBottomSheetFragment();
        f.musicBinder = binder;
        return f;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lấy userId từ SharedPreferences
        SharedPreferences pref = requireActivity()
                .getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        userId = pref.getString("KEY_USER_ID", "");

        // Đăng ký launcher chọn file audio
        pickAudioLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        handleSelectedAudioFile(uri);
                    }
                });
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
        audioManager = (AudioManager) requireContext().getSystemService(Context.AUDIO_SERVICE);

        bindViews(view);
        setupRecyclerView();
        setupListeners();
        setupVolumeControl();
        refreshUI();
        handler.post(seekBarRunnable);

        // Load danh sách nhạc của user từ Supabase
        loadUserSongsFromSupabase();
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
        rootSheetLayout    = v.findViewById(R.id.rootSheetLayout);
        tvSheetTitle       = v.findViewById(R.id.tvSheetTitle);
        ivAlbumArt         = v.findViewById(R.id.ivAlbumArt);
        ivArrowState       = v.findViewById(R.id.ivArrowState);
        tvCurrentSongTitle = v.findViewById(R.id.tvCurrentSongTitle);
        tvSongTime         = v.findViewById(R.id.tvSongTime);
        btnPlayPause       = v.findViewById(R.id.btnPlayPause);
        btnMusicNext       = v.findViewById(R.id.btnMusicNext);
        btnMusicPrevious   = v.findViewById(R.id.btnMusicPrevious);
        btnPlaylist        = v.findViewById(R.id.btnPlaylist);
        btnRepeat          = v.findViewById(R.id.btnRepeat);
        switchMusic        = v.findViewById(R.id.switchMusic);

        layoutVolume       = v.findViewById(R.id.layoutVolume);
        seekBarVolume      = v.findViewById(R.id.seekBarVolume);

        layoutSeekbar      = v.findViewById(R.id.layoutSeekbar);
        layoutSongList     = v.findViewById(R.id.layoutSongList);
        seekBarMusic       = v.findViewById(R.id.seekBarMusic);
        tvSeekStart        = v.findViewById(R.id.tvSeekStart);
        tvSeekEnd          = v.findViewById(R.id.tvSeekEnd);
        rvSongList         = v.findViewById(R.id.rvSongList);

        // Nút thêm nhạc và ProgressBar upload
        btnAddMusic        = v.findViewById(R.id.btnAddMusic);
        progressUpload     = v.findViewById(R.id.progressUpload);
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
        rvSongList.setHasFixedSize(false); // false vì list có thể thay đổi khi thêm nhạc
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Load User Songs từ Supabase
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Gọi API lấy danh sách nhạc user đã upload, ghép vào cuối danh sách nhạc mặc định.
     */
    private void loadUserSongsFromSupabase() {
        if (userId == null || userId.isEmpty()) return;

        SupabaseMusicApiService api = getMusicApi();

        api.getUserSongs("eq." + userId, "song_id,songs(*)").enqueue(
                new Callback<List<SupabaseMusicApiService.UserSongResponse>>() {
                    @Override
                    public void onResponse(
                            Call<List<SupabaseMusicApiService.UserSongResponse>> call,
                            Response<List<SupabaseMusicApiService.UserSongResponse>> response) {

                        if (!response.isSuccessful() || response.body() == null) return;

                        MusicService service = getService();
                        if (service == null) return;

                        // Lấy list nhạc mặc định (mock songs)
                        List<Song> combinedList = new ArrayList<>(MusicService.getMockSongs());

                        // Ghép nhạc user upload vào sau
                        for (SupabaseMusicApiService.UserSongResponse r : response.body()) {
                            if (r.song != null) {
                                combinedList.add(r.song);
                            }
                        }

                        // Cập nhật Service và Adapter
                        service.setSongList(combinedList);
                        if (songAdapter != null && isUiAvailable() && rvSongList != null) {
                            // Tạo lại adapter với list mới
                            songAdapter = new SongAdapter(combinedList, position -> {
                                service.playAt(position);
                                songAdapter.setCurrentPlayingIndex(position);
                                refreshUI();
                            });
                            songAdapter.setCurrentPlayingIndex(service.getCurrentIndex());
                            rvSongList.setAdapter(songAdapter);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<SupabaseMusicApiService.UserSongResponse>> call,
                            Throwable t) {
                        // Không cần báo lỗi — vẫn có nhạc mặc định
                    }
                });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // File Picker
    // ══════════════════════════════════════════════════════════════════════════

    /** Mở file picker chỉ cho phép chọn file audio. Với GetContent, hệ thống cấp URI permission tạm thời. */
    private void openFilePicker() {
        if (pickAudioLauncher != null) {
            pickAudioLauncher.launch("audio/*");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Upload Logic
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Xử lý file audio user vừa chọn:
     * 1. Lấy tên file gốc
     * 2. Upload lên Supabase Storage
     * 3. Insert vào bảng songs
     * 4. Insert vào bảng user_songs
     * 5. Cập nhật danh sách nhạc trong Service và Adapter
     */
    private void handleSelectedAudioFile(Uri fileUri) {
        // Lấy tên file gốc từ URI
        String originalFileName = getFileNameFromUri(fileUri);

        if (userId == null || userId.isEmpty()) {
            showToast("Không xác định được tài khoản để lưu bài hát.");
            return;
        }

        String displayTitle = stripFileExtension(originalFileName);
        String mimeType = getAudioMimeType(fileUri);
        String storageFileName = buildStorageFileName(originalFileName);

        // Hiện progress bar, ẩn nút thêm
        setUploadingState(true);

        SupabaseStorageUploader uploader = new SupabaseStorageUploader(requireContext());
        uploader.uploadAudio(fileUri, storageFileName, mimeType, new SupabaseStorageUploader.UploadCallback() {

            @Override
            public void onSuccess(String publicUrl) {
                // Upload Storage thành công → Insert vào bảng songs
                insertSongToDatabase(displayTitle, "Unknown Artist", publicUrl, storageFileName, uploader);
            }

            @Override
            public void onFailure(String errorMessage) {
                setUploadingState(false);
                showToast("Upload thất bại: " + errorMessage);
            }
        });
    }

    /** Insert bài hát mới vào bảng `songs`, lấy id để insert vào `user_songs` */
    private void insertSongToDatabase(
            String title,
            String artist,
            String url,
            String storageFileName,
            SupabaseStorageUploader uploader
    ) {
        SupabaseMusicApiService api = getMusicApi();

        SongInsertRequest request = new SongInsertRequest(title, artist, url, false);

        // "return=representation" để Supabase trả về bản ghi vừa insert (có id)
        api.insertSong("return=representation", request).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().isEmpty()) {
                    Song insertedSong = response.body().get(0);
                    // Tiếp theo: liên kết bài hát này với user
                    linkSongToUser(insertedSong, storageFileName, uploader);
                } else {
                    setUploadingState(false);
                    cleanupFailedUpload(uploader, null, storageFileName);
                    String errorDetail = getErrorDetailsFromResponse(response);
                    Log.e(TAG, "Insert song that bai (" + response.code() + "): " + errorDetail);
                    showToast("Luu bai hat that bai (" + response.code() + "). " + errorDetail);
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                setUploadingState(false);
                cleanupFailedUpload(uploader, null, storageFileName);
                Log.e(TAG, "Insert song loi ket noi: " + t.getMessage(), t);
                showToast("Loi ket noi khi luu bai hat: " + safeThrowableMessage(t));
            }
        });
    }

    /** Insert vào bảng `user_songs` để liên kết user với bài hát */
    private void linkSongToUser(Song song, String storageFileName, SupabaseStorageUploader uploader) {
        if (userId == null || userId.isEmpty()) {
            setUploadingState(false);
            cleanupFailedUpload(uploader, song, storageFileName);
            showToast("Không xác định được tài khoản để liên kết bài hát.");
            return;
        }

        SupabaseMusicApiService api = getMusicApi();

        api.insertUserSong(new UserSongInsert(userId, song.getDbId()))
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        setUploadingState(false);
                        if (response.isSuccessful()) {
                            showToast("Da them \"" + song.getTitle() + "\" vao danh sach.");
                            // Thêm bài hát vào Service và cập nhật Adapter
                            addSongToListAndRefresh(song);
                        } else {
                            cleanupFailedUpload(uploader, song, storageFileName);
                            String errorDetail = getErrorDetailsFromResponse(response);
                            Log.e(TAG, "Link user_songs that bai (" + response.code() + "): " + errorDetail);
                            showToast("Lien ket bai hat that bai (" + response.code() + "). " + errorDetail);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        setUploadingState(false);
                        cleanupFailedUpload(uploader, song, storageFileName);
                        Log.e(TAG, "Link user_songs loi ket noi: " + t.getMessage(), t);
                        showToast("Loi mang khi lien ket bai hat: " + safeThrowableMessage(t));
                    }
                });
    }

    /** Thêm bài hát mới vào Service và cập nhật RecyclerView */
    private void addSongToListAndRefresh(Song newSong) {
        MusicService service = getService();
        if (service == null) return;

        service.addSong(newSong);

        if (!isUiAvailable()) return;

        List<Song> updatedList = service.getSongList();
        int newPosition = updatedList.size() - 1;

        if (songAdapter != null && rvSongList != null) {
            songAdapter.notifyItemInserted(newPosition);
            // Cuộn xuống bài hát vừa thêm
            rvSongList.smoothScrollToPosition(newPosition);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    /** Lấy tên file gốc từ URI (ví dụ: "my_song.mp3") */
    private String getFileNameFromUri(Uri uri) {
        String fileName = "audio_" + System.currentTimeMillis() + ".mp3";
        Context context = getContext();
        if (context == null) {
            return fileName;
        }
        try (Cursor cursor = context.getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) fileName = cursor.getString(idx);
            }
        } catch (Exception e) {
            // Giữ tên mặc định
        }
        return fileName;
    }

    /** Bật/tắt trạng thái "đang upload" trên UI */
    private void setUploadingState(boolean isUploading) {
        if (!isUiAvailable()) return;
        if (progressUpload != null) {
            progressUpload.setVisibility(isUploading ? View.VISIBLE : View.GONE);
        }
        if (btnAddMusic != null) {
            btnAddMusic.setEnabled(!isUploading);
            btnAddMusic.setAlpha(isUploading ? 0.4f : 1.0f);
        }
    }

    private void cleanupFailedUpload(
            SupabaseStorageUploader uploader,
            @Nullable Song insertedSong,
            String storageFileName
    ) {
        uploader.deleteFile(storageFileName);

        if (insertedSong == null || insertedSong.getDbId() <= 0) {
            return;
        }

        SupabaseMusicApiService api = getMusicApi();
        api.deleteSong("eq." + insertedSong.getDbId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Cleanup song row thất bại với code " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Cleanup song row lỗi: " + t.getMessage());
            }
        });
    }

    private SupabaseMusicApiService getMusicApi() {
        return SupabaseClient.getClient(getSupabaseAccessToken())
                .create(SupabaseMusicApiService.class);
    }

    private String getSupabaseAccessToken() {
        Context context = getContext();
        if (context == null) {
            return "";
        }
        return SupabaseSessionManager.getAccessToken(context);
    }

    private String getErrorDetailsFromResponse(Response<?> response) {
        if (response == null) {
            return "Khong ro loi";
        }

        try {
            if (response.errorBody() != null) {
                String detail = response.errorBody().string();
                if (detail != null && !detail.trim().isEmpty()) {
                    return detail.trim();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Khong doc duoc errorBody: " + e.getMessage(), e);
        }

        return "Khong ro loi";
    }

    private String safeThrowableMessage(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null || throwable.getMessage().trim().isEmpty()) {
            return "Khong ro nguyen nhan";
        }
        return throwable.getMessage();
    }

    private String getAudioMimeType(Uri uri) {
        Context context = getContext();
        if (context == null) {
            return "audio/mpeg";
        }

        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null && mimeType.startsWith("audio/")) {
            return mimeType;
        }
        return "audio/mpeg";
    }

    private String stripFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName;
        }
        return fileName.substring(0, dotIndex);
    }

    private String buildStorageFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = stripFileExtension(originalFileName)
                .replaceAll("[^a-zA-Z0-9._-]", "_")
                .replaceAll("_+", "_");

        if (baseName.isEmpty()) {
            baseName = "audio";
        }

        return userId + "_" + System.currentTimeMillis() + "_" + baseName + extension;
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return ".mp3";
        }
        return fileName.substring(dotIndex).toLowerCase(Locale.ROOT);
    }

    private void showToast(String message) {
        Context context = getContext();
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isUiAvailable() {
        return isAdded() && getView() != null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Listeners
    // ══════════════════════════════════════════════════════════════════════════

    private void setupListeners() {
        requireView().findViewById(R.id.btnCloseSheet).setOnClickListener(v -> {
            if (isFullPlayer) switchToMiniPlayer();
            else dismiss();
        });

        View.OnClickListener toggleStateListener = v -> {
            if (isFullPlayer) switchToMiniPlayer();
            else switchToFullPlayer();
        };
        requireView().findViewById(R.id.layoutSongInfo).setOnClickListener(toggleStateListener);
        btnPlaylist.setOnClickListener(toggleStateListener);

        btnPlayPause.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc == null) return;
            if (svc.isPlaying()) svc.pause();
            else {
                if (svc.getDuration() == 0) svc.play();
                else svc.resume();
            }
            refreshUI();
        });

        btnMusicNext.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc != null) { svc.next(); refreshUI(); }
        });

        btnMusicPrevious.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc != null) { svc.previous(); refreshUI(); }
        });

        btnRepeat.setOnClickListener(v -> {
            MusicService svc = getService();
            if (svc == null) return;
            showRepeatModeToast(svc.cycleRepeatMode());
            refreshUI();
        });
        btnRepeat.setAlpha(0.4f);

        // Nút thêm nhạc
        if (btnAddMusic != null) {
            btnAddMusic.setOnClickListener(v -> openFilePicker());
        }

        seekBarMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar sb) { userIsSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                userIsSeeking = false;
                MusicService svc = getService();
                if (svc != null && svc.getDuration() > 0) {
                    svc.seekTo((int) ((sb.getProgress() / 100.0f) * svc.getDuration()));
                }
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Volume Control
    // ══════════════════════════════════════════════════════════════════════════

    private void setupVolumeControl() {
        if (audioManager == null) return;
        int maxVol     = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        seekBarVolume.setMax(maxVol);
        seekBarVolume.setProgress(currentVol);
        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // State Switching
    // ══════════════════════════════════════════════════════════════════════════

    private void switchToFullPlayer() {
        if (isFullPlayer) return;
        isFullPlayer = true;
        TransitionManager.beginDelayedTransition(rootSheetLayout);
        layoutVolume.setVisibility(View.GONE);
        layoutSeekbar.setVisibility(View.VISIBLE);
        layoutSongList.setVisibility(View.VISIBLE);
        tvSheetTitle.setText("Danh sách phát");
        ivArrowState.setImageResource(android.R.drawable.arrow_up_float);
        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        updateMusicSeekBar();
        if (songAdapter != null) {
            songAdapter.setCurrentPlayingIndex(getService() != null
                    ? getService().getCurrentIndex() : -1);
        }
    }

    private void switchToMiniPlayer() {
        if (!isFullPlayer) return;
        isFullPlayer = false;
        TransitionManager.beginDelayedTransition(rootSheetLayout);
        layoutVolume.setVisibility(View.VISIBLE);
        layoutSeekbar.setVisibility(View.GONE);
        layoutSongList.setVisibility(View.GONE);
        tvSheetTitle.setText("Âm nhạc");
        ivArrowState.setImageResource(android.R.drawable.arrow_down_float);
        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UI Update
    // ══════════════════════════════════════════════════════════════════════════

    private void refreshUI() {
        MusicService svc = getService();
        if (svc == null) return;

        Song song = svc.getCurrentSong();
        if (song != null) {
            tvCurrentSongTitle.setText(song.getTitle());
            ivAlbumArt.setImageResource(
                    song.getCoverResId() != 0 ? song.getCoverResId() : R.drawable.workout_1);
        }

        int pos = svc.getCurrentPosition();
        int dur = svc.getDuration();
        tvSongTime.setText(formatTime(pos) + "/" + formatTime(dur));

        btnPlayPause.setImageResource(svc.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);

        updateRepeatModeUi(svc.getRepeatMode());
        updatePreviousButtonUi(svc);

        switchMusic.setOnCheckedChangeListener(null);
        switchMusic.setChecked(svc.isBackgroundPlaybackEnabled());
        switchMusic.setOnCheckedChangeListener((btn, isChecked) -> {
            MusicService s = getService();
            if (s == null) return;
            s.setBackgroundPlaybackEnabled(isChecked);
            showBackgroundPlaybackToast(isChecked);
        });

        if (songAdapter != null) songAdapter.setCurrentPlayingIndex(svc.getCurrentIndex());
        capturePlaybackSnapshot(svc);
    }

    private void updateMusicSeekBar() {
        if (userIsSeeking || !isAdded()) return;
        MusicService svc = getService();
        if (svc == null) return;

        if (hasPlaybackSnapshotChanged(svc)) { refreshUI(); svc = getService(); }
        if (svc == null) return;

        int position = svc.getCurrentPosition();
        int duration = svc.getDuration();
        if (duration > 0) seekBarMusic.setProgress((int) ((position / (float) duration) * 100));
        tvSeekStart.setText(formatTime(position));
        tvSeekEnd.setText(formatTime(duration));
        tvSongTime.setText(formatTime(position) + "/" + formatTime(duration));
        capturePlaybackSnapshot(svc);
    }

    private void updateRepeatModeUi(int repeatMode) {
        if (!isAdded()) return;
        int tintResId;
        int iconResId = android.R.drawable.ic_menu_rotate;
        switch (repeatMode) {
            case MusicService.REPEAT_MODE_ONE:
                tintResId = R.color.orange_warning;
                iconResId = android.R.drawable.ic_menu_revert;
                break;
            case MusicService.REPEAT_MODE_ALL:
                tintResId = R.color.primary_green;
                break;
            default:
                tintResId = R.color.grey_locked;
                break;
        }
        int tintColor = ContextCompat.getColor(requireContext(), tintResId);
        btnRepeat.setImageResource(iconResId);
        btnRepeat.setImageTintList(ColorStateList.valueOf(tintColor));
        btnRepeat.setAlpha(repeatMode == MusicService.REPEAT_MODE_OFF ? 0.45f : 1.0f);
    }

    private void updatePreviousButtonUi(MusicService svc) {
        if (!isAdded()) return;
        List<Song> songs = svc.getSongList();
        boolean canGoPrevious = songs != null && !songs.isEmpty()
                && (svc.getCurrentIndex() > 0
                || svc.getRepeatMode() == MusicService.REPEAT_MODE_ALL);
        int tintColor = ContextCompat.getColor(requireContext(),
                canGoPrevious ? android.R.color.black : R.color.grey_locked);
        btnMusicPrevious.setEnabled(canGoPrevious);
        btnMusicPrevious.setAlpha(canGoPrevious ? 1.0f : 0.35f);
        btnMusicPrevious.setImageTintList(ColorStateList.valueOf(tintColor));
    }

    private void showRepeatModeToast(int repeatMode) {
        if (!isAdded()) return;
        String msg;
        switch (repeatMode) {
            case MusicService.REPEAT_MODE_ONE: msg = "Đang lặp lại bài hiện tại"; break;
            case MusicService.REPEAT_MODE_ALL: msg = "Đang lặp lại toàn bộ danh sách"; break;
            default: msg = "Đã tắt lặp lại"; break;
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showBackgroundPlaybackToast(boolean enabled) {
        if (!isAdded()) return;
        Toast.makeText(requireContext(),
                enabled ? "Đã bật chạy nhạc nền" : "Đã tắt chạy nhạc nền",
                Toast.LENGTH_SHORT).show();
    }

    private boolean hasPlaybackSnapshotChanged(MusicService svc) {
        return lastKnownSongIndex  != svc.getCurrentIndex()
                || lastKnownPlaying    != svc.isPlaying()
                || lastKnownRepeatMode != svc.getRepeatMode();
    }

    private void capturePlaybackSnapshot(MusicService svc) {
        lastKnownSongIndex  = svc.getCurrentIndex();
        lastKnownPlaying    = svc.isPlaying();
        lastKnownRepeatMode = svc.getRepeatMode();
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "00:00";
        int totalSec = ms / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60);
    }

    @Nullable
    private MusicService getService() {
        if (musicBinder == null) return null;
        return musicBinder.getService();
    }
}
