package com.seriouschoi.steaktimer.domain

/**
 * 진동(햅틱) Port. 실제로 어떻게 떨지는 바깥 계층(:core:platform)이 구현한다.
 *
 * 참고: 도메인 세션은 이 Port를 소비하지 않는다(진동은 표현 계층 피드백).
 * 다만 소비자(:feature ViewModel)와 구현(:core:platform)이 함께 의존하는 공용 커널이
 * :core:domain이라, 계약을 여기에 둔다.
 */
interface Haptic {
    /** 탭 전까지 반복(펄스) 진동 시작. */
    fun startAlert()

    /** 진동 정지. */
    fun stop()
}
