package com.seriouschoi.steaktimer.feature.timer

/**
 * 타이머 화면 표시 전용 상태. 도메인 개념(phase 등)은 담지 않고, "무엇을 그릴지"만 갖는다.
 * 도메인 상태([com.seriouschoi.steaktimer.domain.SteakTimerState])를 Mapper로 변환해 만든다.
 */
data class TimerUiState(
    // 세션이 Idle로 돌아왔는지(=정지됨). 화면 전환은 Nav가 담당하고,
    // 타이머 화면은 이 값이 true가 되면 설정 화면으로 되돌아간다.
    val isIdle: Boolean,
    val timeText: String,
    val progress: Float,        // 0f..1f 원형 게이지 (남은 비율)
    val isVibrating: Boolean,   // 알림(진동) 상태 표시
    val hint: String,           // "탭해서 다음" 등
    val showStopConfirm: Boolean,
) {
    companion object {
        // 초기값은 도메인 Idle에 대응.
        val INITIAL = TimerUiState(
            isIdle = true,
            timeText = "00:00",
            progress = 0f,
            isVibrating = false,
            hint = "",
            showStopConfirm = false,
        )
    }
}
