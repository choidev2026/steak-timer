package com.seriouschoi.steaktimer.feature.timer

/**
 * 화면에서 발행하는 UI 인텐트. **사용자가 실제로 낼 수 있는 것만** 담는다.
 * (도메인 TimerIntent의 Tick/Start 같은 엔진·시스템 입력은 여기 없음 → 계층 분리)
 */
sealed interface TimerUiIntent {
    /** 짧은 탭. 실행 중=조기 넘기기, 알림 중=다음 인터벌. */
    data object Tap : TimerUiIntent

    /** 롱프레스. 종료 확인으로 진입(Running에서만 의미). */
    data object LongPress : TimerUiIntent

    /** 종료 확인 오버레이의 '종료'. */
    data object ConfirmStop : TimerUiIntent

    /** 종료 확인 오버레이의 '취소'. */
    data object CancelStop : TimerUiIntent
}
