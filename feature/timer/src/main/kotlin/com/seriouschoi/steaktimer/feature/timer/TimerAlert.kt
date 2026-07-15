package com.seriouschoi.steaktimer.feature.timer

/**
 * 타이머 화면에 뜨는 알림(오버레이) 종류. **표현 계층 개념**이다.
 * 도메인 상태(Alerting/ConfirmStop)를 Mapper가 이 알림으로 옮긴다.
 *
 * 두 variant는 '닫힘 방식'이 다르다:
 * - [Flip] = dismissable — 탭하면 닫히며 다음 인터벌로(Skip).
 * - [ConfirmStop] = modal — 종료/취소 버튼 선택이 필요하고, 바깥 탭으로 안 닫힌다.
 */
sealed interface TimerAlert {
    /** "뒤집기" 알림. 탭 = 다음 인터벌(Skip). */
    data object Flip : TimerAlert

    /** 정지 확인 알림. 종료/취소 버튼. */
    data object ConfirmStop : TimerAlert
}
