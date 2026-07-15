package com.seriouschoi.steaktimer.feature.timer.countdown

/**
 * 화면에서 발행하는 UI 인텐트. **사용자 입력 메커니즘(버튼/탭)** 단위로 담는다.
 * ViewModel이 이걸 도메인 의미(TimerIntent.Advance/RequestStop/...)로 번역한다.
 */
sealed interface TimerUiIntent {
    /** 건너뛰기 버튼, 또는 알림 중 화면 전체 탭. → 도메인 Advance. */
    data object Skip : TimerUiIntent

    /** 정지 버튼. → 도메인 RequestStop(종료 확인 진입). */
    data object Stop : TimerUiIntent

    /** 종료 확인 오버레이의 '종료'. */
    data object ConfirmStop : TimerUiIntent

    /** 종료 확인 오버레이의 '취소'. */
    data object CancelStop : TimerUiIntent
}
