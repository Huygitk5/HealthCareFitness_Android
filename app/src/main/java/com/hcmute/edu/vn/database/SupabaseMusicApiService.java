package com.hcmute.edu.vn.database;

import com.hcmute.edu.vn.model.Song;
import com.hcmute.edu.vn.model.SongInsertRequest;
import com.hcmute.edu.vn.model.UserSongInsert;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Các API endpoint liên quan đến tính năng nhạc.
 * Tách riêng ra file này để giữ SupabaseApiService.java gọn gàng.
 *
 * Thêm interface này vào SupabaseClient:
 *   SupabaseMusicApiService musicApi =
 *       SupabaseClient.getClient().create(SupabaseMusicApiService.class);
 */
public interface SupabaseMusicApiService {

    // ── Bảng songs ────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách nhạc của user (join qua bảng user_songs).
     * select=songs(*) để lấy toàn bộ thông tin bài hát.
     *
     * Ví dụ gọi:
     *   getUserSongs("eq." + userId, "song_id,songs(*)")
     */
    @GET("user_songs")
    Call<List<UserSongResponse>> getUserSongs(
            @Query("user_id") String eqUserId,
            @Query("select")  String select
    );

    /**
     * Thêm bài hát mới vào bảng songs.
     * Cần header "Prefer: return=representation" để Supabase trả về bản ghi vừa tạo
     * (bao gồm id BIGSERIAL tự sinh) — dùng để lấy song_id cho bước tiếp theo.
     */
    @POST("songs")
    Call<List<Song>> insertSong(
            @Header("Prefer") String prefer,
            @Body SongInsertRequest song
    );

    /**
     * Liên kết bài hát với user trong bảng user_songs.
     */
    @POST("user_songs")
    Call<Void> insertUserSong(
            @Body UserSongInsert userSong
    );

    /**
     * Xóa bài hát khỏi danh sách của user (chỉ xóa liên kết, không xóa file).
     */
    @DELETE("user_songs")
    Call<Void> deleteUserSong(
            @Query("user_id") String eqUserId,
            @Query("song_id") String eqSongId
    );

    // ── Inner class: Response khi join user_songs với songs ───────────────────

    class UserSongResponse {
        @com.google.gson.annotations.SerializedName("song_id")
        public long songId;

        @com.google.gson.annotations.SerializedName("songs")
        public Song song;
    }
}