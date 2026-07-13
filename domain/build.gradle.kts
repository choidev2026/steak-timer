plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // kotlin.test 어노테이션/단언을 JUnit4에 바인딩 (순수 상태기계 테스트용, 안드 의존 없음)
    testImplementation(kotlin("test-junit"))
}
