package com.seriouschoi.steaktimer.feature.timer

/**
 * 화면 표시 전용 상태. 도메인 개념(phase 등)은 담지 않고, "무엇을 그릴지"만 갖는다.
 * 도메인 상태([com.seriouschoi.steaktimer.domain.SteakTimerState])를 Mapper로 변환해 만든다.
 */
data class TimerUiState(
    val showSetup: Boolean,     // 설정 화면을 그릴지 (도메인 Idle)
    val timeText: String,
    val progress: Float,        // 0f..1f 원형 게이지 (남은 비율)
    val isVibrating: Boolean,   // 알림(진동) 상태 표시
    val hint: String,           // "탭해서 다음" 등
    val showStopConfirm: Boolean,
) {
    companion object {
        // 초기값은 도메인 Idle에 대응 → 설정 화면.
        val INITIAL = TimerUiState(
            showSetup = true,
            timeText = "00:00",
            progress = 0f,
            isVibrating = false,
            hint = "",
            showStopConfirm = false,
        )
    }
}
