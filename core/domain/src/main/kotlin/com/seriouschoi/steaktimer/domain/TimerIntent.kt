package com.seriouschoi.steaktimer.domain

/**
 * 상태기계에 들어오는 입력. **도메인 의미**로만 정의한다.
 * (탭/롱프레스/버튼 같은 입력 '메커니즘'은 UI 몫 → TimerUiIntent에서 이걸로 번역)
 */
sealed interface TimerIntent {

    /** 설정한 간격으로 세션 시작. */
    data class Start(val intervalMs: Long) : TimerIntent

    /** 경과 시간 주입(실제 시간 공급은 TimerEngine 몫). */
    data class Tick(val elapsedMs: Long) : TimerIntent

    /** 다음 인터벌로 넘어간다. Running=조기 뒤집기, Alerting=정상 뒤집기(다음 인터벌). */
    data object Advance : TimerIntent

    /** 정지를 요청한다. Running에서만 종료 확인으로 진입. */
    data object RequestStop : TimerIntent

    /** 종료 확인. */
    data object ConfirmStop : TimerIntent

    /** 종료 취소(직전 상태 복귀). */
    data object CancelStop : TimerIntent
}
