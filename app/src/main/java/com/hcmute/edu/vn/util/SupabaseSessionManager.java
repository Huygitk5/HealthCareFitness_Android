package com.hcmute.edu.vn.util;

import android.content.Context;
import android.content.SharedPreferences;

public final class SupabaseSessionManager {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_ACCESS_TOKEN = "KEY_SUPABASE_ACCESS_TOKEN";

    private SupabaseSessionManager() {
        // Utility class
    }

    public static void saveAccessToken(Context context, String accessToken) {
        if (context == null) {
            return;
        }

        SharedPreferences preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .apply();
    }

    public static String getAccessToken(Context context) {
        if (context == null) {
            return "";
        }

        return context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACCESS_TOKEN, "");
    }

    public static void clearAccessToken(Context context) {
        if (context == null) {
            return;
        }

        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACCESS_TOKEN)
                .apply();
    }
}
