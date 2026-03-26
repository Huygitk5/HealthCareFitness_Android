package com.hcmute.edu.vn.database;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {
    // Lay trong muc Project Settings -> API cua Supabase
    private static final String SUPABASE_URL = "https://npifogdquxhxylhrbmec.supabase.co/rest/v1/";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_L0AauC1QjtEemHrDWF83-A_sbB76Ynd";

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = buildRetrofit(SUPABASE_ANON_KEY);
        }
        return retrofit;
    }

    public static Retrofit getClient(String accessToken) {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return getClient();
        }
        return buildRetrofit(accessToken.trim());
    }

    private static Retrofit buildRetrofit(String bearerToken) {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            Request newRequest = chain.request().newBuilder()
                    .addHeader("apikey", SUPABASE_ANON_KEY)
                    .addHeader("Authorization", "Bearer " + bearerToken)
                    .addHeader("Content-Type", "application/json")
                    .build();
            return chain.proceed(newRequest);
        }).build();

        return new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}
