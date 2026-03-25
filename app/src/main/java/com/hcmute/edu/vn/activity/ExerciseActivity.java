package com.hcmute.edu.vn.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.model.Exercise;
import com.hcmute.edu.vn.service.MusicService;

import java.util.ArrayList;

/**
 * ExerciseActivity — màn hình thực hiện bài tập.
 *
 * Tích hợp MusicService theo mô hình Hybrid:
 *   - startService()  → nhạc chạy độc lập với vòng đời Activity
 *   - bindService()   → lấy MusicBinder để điều khiển từ BottomSheet
 *
 * Khi Activity bị destroy (xoay màn hình), nhạc vẫn phát vì Service đã được start.
 * bindService() được gọi lại trong onStart() để kết nối lại với Service đang chạy.
 */
public class ExerciseActivity extends AppCompatActivity {

    private ArrayList<Exercise> exerciseList;
    private int currentIndex = 0;

    private ImageView ivExercise;
    private TextView tvExerciseName, tvTimer, tvExerciseProgress;
    private ImageButton btnNext, btnPrevious, btnClose, icWorkoutMusic;
    private Button btnPause;

    private MusicService.MusicBinder musicBinder;  // null cho đến khi bind thành công
    private boolean isMusicServiceBound = false;

    /**
     * ServiceConnection — nhận callback khi bind/unbind thành công với MusicService.
     * Được khai báo là field để dùng lại trong onStart/onStop.
     */
    private final ServiceConnection musicServiceConnection = new ServiceConnection() {

        /**
         * Gọi khi bindService() thành công.
         * Lưu lại MusicBinder để truyền vào BottomSheet.
         */
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicService.MusicBinder) service;
            isMusicServiceBound = true;
            // Tự động phát bài đầu khi kết nối lần đầu (nếu chưa phát)
        }

        /**
         * Gọi khi Service bị kill đột ngột (hiếm gặp).
         * Reset binder để tránh NPE.
         */
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBinder = null;
            isMusicServiceBound = false;
        }
    };

    // ======================= Lifecycle =======================

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_exercise);
        
        // Cấu hình thanh trạng thái
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);

        initViews();

        // Nhận danh sách bài tập từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EXTRA_EXERCISE_LIST")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                exerciseList = intent.getSerializableExtra("EXTRA_EXERCISE_LIST", ArrayList.class);
            } else {
                exerciseList = (ArrayList<Exercise>) intent.getSerializableExtra("EXTRA_EXERCISE_LIST");
            }
        }

        if (exerciseList != null && !exerciseList.isEmpty()) {
            currentIndex = 0;
            updateExerciseUI();
        } else {
            Toast.makeText(this, "Không có dữ liệu bài tập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupListeners();

        // Khởi động MusicService (START — chạy liên tục dù Activity bị destroy)
        Intent musicIntent = new Intent(this, MusicService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(musicIntent);
        } else {
            startService(musicIntent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // BIND — kết nối để nhận IBinder điều khiển nhạc
        // Guard: tránh bind nhiều lần nếu Service vẫn còn bound (ví dụ sau onStop sớm)
        if (!isMusicServiceBound) {
            Intent musicIntent = new Intent(this, MusicService.class);
            bindService(musicIntent, musicServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MusicService musicService = musicBinder != null ? musicBinder.getService() : null;
        boolean shouldStopMusicService = (musicService == null)
                || !musicService.isPlaying()
                || !musicService.isBackgroundPlaybackEnabled();
        // UNBIND — giải phóng kết nối, nhưng Service vẫn sống (đã startService)
        if (isMusicServiceBound) {
            unbindService(musicServiceConnection);
            isMusicServiceBound = false;
            musicBinder = null;
        }
        if (shouldStopMusicService) {
            stopService(new Intent(this, MusicService.class));
        }
    }

    /**
     * Dừng hẳn Service khi Activity bị finish() hoàn toàn (bấm nút Close).
     * Phân biệt với onStop để tránh dừng nhạc khi chỉ xoay màn hình.
     */
    // ======================= View Initialization =======================

    private void initViews() {
        ivExercise = findViewById(R.id.ivExercise);
        tvExerciseName = findViewById(R.id.tvExerciseName);
        tvTimer = findViewById(R.id.tvTimer);
        tvExerciseProgress = findViewById(R.id.tvExerciseProgress);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnClose = findViewById(R.id.btnClose);
        btnPause = findViewById(R.id.btnPause);
        icWorkoutMusic   = findViewById(R.id.icWorkoutMusic);
    }

    private void setupListeners() {
        // Đóng Activity → dừng hẳn Service
        btnClose.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (currentIndex < exerciseList.size() - 1) {
                currentIndex++;
                updateExerciseUI();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateExerciseUI();
            }
        });

        btnPause.setOnClickListener(v ->
                Toast.makeText(ExerciseActivity.this, "Đã bấm Pause", Toast.LENGTH_SHORT).show()
        );

        /**
         * Nút âm nhạc → mở MusicBottomSheetFragment.
         * Truyền MusicBinder vào Fragment để điều khiển trực tiếp Service.
         */
        icWorkoutMusic.setOnClickListener(v -> {
            if (!isMusicServiceBound || musicBinder == null) {
                Toast.makeText(this, "Đang kết nối dịch vụ nhạc...", Toast.LENGTH_SHORT).show();
                return;
            }
            MusicBottomSheetFragment sheet =
                    MusicBottomSheetFragment.newInstance(musicBinder);
            sheet.show(getSupportFragmentManager(), "MusicBottomSheet");
        });
    }

    private void updateExerciseUI() {
        if (exerciseList == null || exerciseList.isEmpty()) return;

        Exercise currentExercise = exerciseList.get(currentIndex);

        // 1. Tên bài tập
        if (currentExercise.getName() != null) {
            tvExerciseName.setText(currentExercise.getName().toUpperCase());
        }

        // 2. Reps hoặc Timer
        if (currentExercise.getBaseRecommendedReps() != null) {
            String repsData = currentExercise.getBaseRecommendedReps();
            tvTimer.setText(repsData);

            // Kiểm tra: Nếu chuỗi có chứa dấu ":" (ví dụ 00:30) thì là đếm giờ -> Hiện nút Pause
            // Ngược lại (ví dụ 15, 12) thì là đếm số lần (Reps) -> Ẩn nút Pause
            if (repsData.contains(":")) {
                btnPause.setVisibility(View.VISIBLE);
            } else {
                btnPause.setVisibility(View.INVISIBLE);
            }
        } else {
            // Đề phòng trường hợp dữ liệu null
            btnPause.setVisibility(View.INVISIBLE);
        }

        // 3. Load ảnh bằng Glide
        com.bumptech.glide.Glide.with(this)
                .load(currentExercise.getImageUrl()) // Lấy link ảnh từ Supabase
                .placeholder(R.drawable.workout_1)   // Ảnh chờ trong lúc load mạng
                .error(R.drawable.workout_1)         // Ảnh mặc định nếu link hỏng/không có link
                .into(ivExercise);

        // 4. Set số thứ tự bài tập
        String progressText = (currentIndex + 1) + " / " + exerciseList.size();
        tvExerciseProgress.setText(progressText);

        // 5. Ẩn/Hiện nút Previous & Next ở đầu/cuối danh sách
        btnPrevious.setVisibility(currentIndex == 0 ? View.INVISIBLE : View.VISIBLE);
        btnNext.setVisibility(currentIndex == exerciseList.size() - 1 ? View.INVISIBLE : View.VISIBLE);
    }
}
