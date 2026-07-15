package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.SteakTimerState

/** 도메인 상태 → 표시 전용 상태. */
fun SteakTimerState.toUiState(): TimerUiState = when (this) {
    is SteakTimerState.Idle -> TimerUiState.INITIAL // isIdle = true

    is SteakTimerState.Running -> TimerUiState(
        isIdle = false,
        timeText = TimeFormat.mmSs(remainingMs),
        progress = if (intervalMs <= 0L) 0f else (remainingMs.toFloat() / intervalMs).coerceIn(0f, 1f),
        isVibrating = false,
        hint = "",
        showStopConfirm = false,
    )

    is SteakTimerState.Alerting -> TimerUiState(
        isIdle = false,
        timeText = "뒤집기",
        progress = 0f,
        isVibrating = true,
        hint = "탭해서 다음",
        showStopConfirm = false,
    )

    is SteakTimerState.ConfirmStop -> resumeTo.toUiState().copy(showStopConfirm = true)
}
