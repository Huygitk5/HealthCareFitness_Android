package com.hcmute.edu.vn.util;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Utility class upload file .mp3 lên Supabase Storage.
 *
 * Supabase Storage không dùng Retrofit mà dùng REST API riêng,
 * nên phải dùng OkHttpClient trực tiếp.
 *
 * Endpoint upload:
 *   POST /storage/v1/object/{bucket}/{filePath}
 */
public class SupabaseStorageUploader {

    private static final String TAG = "StorageUploader";

    // ── Thay bằng thông tin project của bạn ──────────────────────────────────
    private static final String SUPABASE_URL     = "https://npifogdquxhxylhrbmec.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_L0AauC1QjtEemHrDWF83-A_sbB76Ynd";
    private static final String BUCKET_NAME      = "music_storage";
    // ─────────────────────────────────────────────────────────────────────────

    public interface UploadCallback {
        /** Upload thành công, trả về URL công khai của file */
        void onSuccess(String publicUrl);
        /** Upload thất bại */
        void onFailure(String errorMessage);
    }

    private final OkHttpClient httpClient;
    private final Context context;

    public SupabaseStorageUploader(Context context) {
        this.context    = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // file lớn cần timeout dài hơn
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    /**
     * Upload file .mp3 lên Supabase Storage.
     *
     * @param fileUri   URI của file .mp3 do user chọn (từ Intent/File Picker)
     * @param fileName  Tên file lưu trên Storage (ví dụ: "userId_timestamp.mp3")
     * @param callback  Callback trả kết quả về Main Thread
     */
    public void uploadMp3(Uri fileUri, String fileName, UploadCallback callback) {
        // Đọc bytes của file từ URI trên background thread
        new Thread(() -> {
            InputStream inputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(fileUri);
                if (inputStream == null) {
                    notifyFailure(callback, "Không thể đọc file. URI không hợp lệ.");
                    return;
                }

                byte[] fileBytes = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    fileBytes = inputStream.readAllBytes();
                }

                String uploadPath = SUPABASE_URL
                        + "/storage/v1/object/"
                        + BUCKET_NAME + "/"
                        + fileName;

                RequestBody requestBody = RequestBody.create(
                        fileBytes,
                        MediaType.parse("audio/mpeg")
                );

                Request request = new Request.Builder()
                        .url(uploadPath)
                        .addHeader("apikey",        SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("Content-Type",  "audio/mpeg")
                        // "x-upsert: true" cho phép ghi đè nếu file đã tồn tại
                        .addHeader("x-upsert",      "true")
                        .post(requestBody)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.e(TAG, "Upload thất bại: " + e.getMessage());
                        notifyFailure(callback, "Lỗi mạng: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            // Tạo Public URL từ tên bucket và file path
                            String publicUrl = SUPABASE_URL
                                    + "/storage/v1/object/public/"
                                    + BUCKET_NAME + "/"
                                    + fileName;

                            Log.d(TAG, "Upload thành công: " + publicUrl);
                            notifySuccess(callback, publicUrl);
                        } else {
                            String errorBody = response.body() != null
                                    ? response.body().string()
                                    : "Không rõ lỗi";
                            Log.e(TAG, "Upload lỗi HTTP " + response.code() + ": " + errorBody);
                            notifyFailure(callback, "Lỗi server " + response.code() + ": " + errorBody);
                        }
                        response.close();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "Đọc file thất bại: " + e.getMessage());
                notifyFailure(callback, "Không thể đọc file: " + e.getMessage());
            } finally {
                if (inputStream != null) {
                    try { inputStream.close(); } catch (IOException ignored) {}
                }
            }
        }).start();
    }

    // ── Helpers: callback về Main Thread ─────────────────────────────────────

    private void notifySuccess(UploadCallback callback, String url) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> callback.onSuccess(url));
    }

    private void notifyFailure(UploadCallback callback, String error) {
        new android.os.Handler(android.os.Looper.getMainLooper())
                .post(() -> callback.onFailure(error));
    }
}