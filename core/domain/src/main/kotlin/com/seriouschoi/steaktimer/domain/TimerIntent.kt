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

    /**
     * 인터벌 시간이 다 됨. Running → 즉시 Alerting.
     * 화면 꺼짐/딥슬립으로 Tick(delay 루프)이 얼어붙어도, 예약된 알람이 이걸 발행해
     * 정확한 시각에 알림을 보장한다(Phase 7). 남은 시간 값과 무관하게 완주로 처리.
     */
    data object Deadline : TimerIntent

    /** 다음 인터벌로 넘어간다. Running=조기 뒤집기, Alerting=정상 뒤집기(다음 인터벌). */
    data object Advance : TimerIntent

    /** 정지를 요청한다. Running에서만 종료 확인으로 진입. */
    data object RequestStop : TimerIntent

    /** 종료 확인. */
    data object ConfirmStop : TimerIntent

    /** 종료 취소(직전 상태 복귀). */
    data object CancelStop : TimerIntent
}
