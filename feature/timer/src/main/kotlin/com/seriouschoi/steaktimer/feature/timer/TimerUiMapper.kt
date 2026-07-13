package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.SteakTimerState

/** 도메인 상태 → 표시 전용 상태. */
fun SteakTimerState.toUiState(): TimerUiState = when (this) {
    is SteakTimerState.Idle -> TimerUiState.INITIAL

    is SteakTimerState.Running -> TimerUiState(
        timeText = formatMmSs(remainingMs),
        progress = if (intervalMs <= 0L) 0f else (remainingMs.toFloat() / intervalMs).coerceIn(0f, 1f),
        isVibrating = false,
        hint = "",
        showStopConfirm = false,
    )

    is SteakTimerState.Alerting -> TimerUiState(
        timeText = "뒤집기",
        progress = 0f,
        isVibrating = true,
        hint = "탭해서 다음",
        showStopConfirm = false,
    )

    is SteakTimerState.ConfirmStop -> resumeTo.toUiState().copy(showStopConfirm = true)
}

/** 남은 밀리초를 mm:ss로. 카운트다운 느낌을 위해 올림(1ms도 1초로 표시). */
private fun formatMmSs(ms: Long): String {
    val totalSec = ((ms + 999) / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
