package com.seriouschoi.steaktimer.domain

/** 상태기계에 들어오는 입력. */
sealed interface TimerIntent {

    /** 설정한 간격으로 세션 시작. */
    data class Start(val intervalMs: Long) : TimerIntent

    /** 경과 시간 주입(실제 시간 공급은 Phase 2의 TimerEngine 몫). */
    data class Tick(val elapsedMs: Long) : TimerIntent

    /** 짧은 탭. Running=조기 뒤집기, Alerting=정상 뒤집기(다음 인터벌). */
    data object Tap : TimerIntent

    /** 길게 누르기. Running에서만 종료 확인으로 진입. */
    data object LongPress : TimerIntent

    /** 종료 확인. */
    data object ConfirmStop : TimerIntent

    /** 종료 취소(직전 상태 복귀). */
    data object CancelStop : TimerIntent
}
