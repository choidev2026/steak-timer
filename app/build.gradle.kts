plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.seriouschoi.steaktimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.seriouschoi.steaktimer"
        minSdk = 30          // Wear OS 3 (API 30)
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // 컴포지션 루트: 기능과 어댑터 모듈을 결합(어댑터 바인딩이 그래프에 들어오도록 platform도 의존)
    implementation(project(":feature:timer"))
    implementation(project(":core:platform"))
    // 세션 결선을 위해 domain 타입을 직접 참조(앱 스코프 세션 provide, #16)
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.wear.compose.material)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
