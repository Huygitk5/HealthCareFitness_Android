package com.hcmute.edu.vn.database;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {
    // Lấy trong mục Project Settings -> API của Supabase
    private static final String SUPABASE_URL = "https://npifogdquxhxylhrbmec.supabase.co/rest/v1/";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_L0AauC1QjtEemHrDWF83-A_sbB76Ynd";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            // Tạo một Interceptor để tự động nhét Header vào mọi cuộc gọi API
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request newRequest = chain.request().newBuilder()
                        .addHeader("apikey", SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        // Bắt buộc với Supabase khi dùng POST/PATCH
                        .addHeader("Content-Type", "application/json")
                        .build();
                return chain.proceed(newRequest);
            }).build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(SUPABASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
