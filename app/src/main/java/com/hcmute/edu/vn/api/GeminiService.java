package com.hcmute.edu.vn.api;

import com.hcmute.edu.vn.model.gemini.GeminiRequest;
import com.hcmute.edu.vn.model.gemini.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}
