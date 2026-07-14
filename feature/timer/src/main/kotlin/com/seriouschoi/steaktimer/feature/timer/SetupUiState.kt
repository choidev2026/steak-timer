package com.seriouschoi.steaktimer.feature.timer

/**
 * 설정 화면 표시 전용 상태. 고른 간격(초)과 그 표시 문자열만 갖는다.
 */
data class SetupUiState(
    val seconds: Int,
    val timeText: String,
)
