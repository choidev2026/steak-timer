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
    // 컴포지션 루트: 기능과 어댑터/런타임 모듈을 결합(각 모듈의 Hilt 바인딩이 그래프에 들어오도록 의존)
    implementation(project(":feature:timer"))
    implementation(project(":feature:tile")) // 타일 서비스 번들 + 매니페스트 머지
    implementation(project(":core:platform"))
    implementation(project(":core:timersession"))
    // 앱 수명 스코프 provide에 필요(도메인 타입은 직접 참조하지 않음 — 세션 결선은 timersession으로 이동)
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
