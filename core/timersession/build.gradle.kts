plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.seriouschoi.steaktimer.core.timersession"
    compileSdk = 34

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // 세션(SteakTimerSession)과 Port(TimerEngine) 타입만 참조. 어댑터 바인딩은
    // 병합된 앱 그래프에서 Hilt가 해결하므로 :core:platform 직접 의존은 불필요.
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
