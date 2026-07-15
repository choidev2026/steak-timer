package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.SteakTimerState

/** 도메인 상태 → 표시 전용 상태. */
fun SteakTimerState.toUiState(): TimerUiState = when (this) {
    is SteakTimerState.Idle -> TimerUiState.INITIAL // isIdle = true, alert = null

    is SteakTimerState.Running -> TimerUiState(
        isIdle = false,
        timeText = TimeFormat.mmSs(remainingMs),
        progress = if (intervalMs <= 0L) 0f else (remainingMs.toFloat() / intervalMs).coerceIn(0f, 1f),
        alert = null,
    )

    is SteakTimerState.Alerting -> TimerUiState(
        isIdle = false,
        timeText = TimeFormat.mmSs(0), // 완주 → 시계는 00:00, "뒤집기" 문구는 Flip 알림이 담당
        progress = 0f,
        alert = TimerAlert.Flip,
    )

    is SteakTimerState.ConfirmStop -> resumeTo.toUiState().copy(alert = TimerAlert.ConfirmStop)
}
