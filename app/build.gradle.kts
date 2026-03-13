plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.hcmute.edu.vn"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.hcmute.edu.vn"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.code.gson:gson:2.10.1")
    // Thư viện Retrofit để gọi API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Thư viện Gson để tự động parse JSON thành Model Java
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Thư viện OkHttp để cấu hình Header (Interceptor) cho Supabase
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Thư viện vẽ biểu đồ
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}