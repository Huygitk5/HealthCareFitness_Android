package com.hcmute.edu.vn.nutrition.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.hcmute.edu.vn.R;
import com.hcmute.edu.vn.home.activity.HomeActivity;
import com.hcmute.edu.vn.profile.ProfileActivity;
import com.hcmute.edu.vn.workout.activity.WorkoutActivity;

public class BreakfastNutritionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.breakfast_nutrition);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailMeal), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Bottom Navigation
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navNutrition = findViewById(R.id.nav_nutrition);
        LinearLayout navWorkout = findViewById(R.id.nav_workout);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(BreakfastNutritionActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navNutrition.setOnClickListener(v -> {
            Intent intent = new Intent(BreakfastNutritionActivity.this, NutritionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navWorkout.setOnClickListener(v -> {
            Intent intent = new Intent(BreakfastNutritionActivity.this, WorkoutActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(BreakfastNutritionActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}
