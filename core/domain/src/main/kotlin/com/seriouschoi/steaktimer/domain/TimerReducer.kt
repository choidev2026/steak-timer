package com.seriouschoi.steaktimer.domain

import com.seriouschoi.steaktimer.domain.SteakTimerState.Alerting
import com.seriouschoi.steaktimer.domain.SteakTimerState.ConfirmStop
import com.seriouschoi.steaktimer.domain.SteakTimerState.Idle
import com.seriouschoi.steaktimer.domain.SteakTimerState.Running

/**
 * 순수 함수 상태 전이. (state, intent) -> state.
 * 부수효과 없음. 처리 불가한 입력은 현재 상태를 그대로 반환한다.
 *
 * cycle 규칙: "뒤집기가 일어나는 순간(= tap 재시작)"에 +1.
 *  - Alerting에서 tap(정상 뒤집기) -> cycle+1
 *  - Running에서 tap(조기 뒤집기)  -> cycle+1
 *  - Alerting 진입(완주) 자체로는 증가하지 않음.
 */
fun SteakTimerState.reduce(intent: TimerIntent): SteakTimerState = when (this) {

    is Idle -> when (intent) {
        is TimerIntent.Start -> Running(
            intervalMs = intent.intervalMs,
            remainingMs = intent.intervalMs,
            cycle = 0,
        )
        else -> this
    }

    is Running -> when (intent) {
        is TimerIntent.Tick -> {
            val left = remainingMs - intent.elapsedMs
            if (left <= 0L) Alerting(intervalMs = intervalMs, cycle = cycle)
            else copy(remainingMs = left)
        }
        // 조기 뒤집기: 남은 시간 무시하고 다음 인터벌 재시작
        is TimerIntent.Tap -> Running(
            intervalMs = intervalMs,
            remainingMs = intervalMs,
            cycle = cycle + 1,
        )
        // 종료 확인은 Running에서만 받는다
        is TimerIntent.LongPress -> ConfirmStop(resumeTo = this)
        else -> this
    }

    is Alerting -> when (intent) {
        // 정상 뒤집기: 다음 인터벌 시작 (Alerting 이탈 = 진동 정지 신호)
        is TimerIntent.Tap -> Running(
            intervalMs = intervalMs,
            remainingMs = intervalMs,
            cycle = cycle + 1,
        )
        // Alerting에서는 tick(시간 정지)도, longPress(부자연스러움)도 무시
        else -> this
    }

    is ConfirmStop -> when (intent) {
        is TimerIntent.ConfirmStop -> Idle
        is TimerIntent.CancelStop -> resumeTo
        else -> this
    }
}
