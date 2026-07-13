package com.seriouschoi.steaktimer.domain

/**
 * 스테이크 타이머 세션의 도메인 상태.
 *
 * UI(다이얼로그/화면)와 무관한 "세션의 사실"만 표현한다.
 * 진동을 켜고 끄는 동작은 이 상태의 전이에서 파생되는 부수효과이며 바깥 계층이 관찰한다.
 */
sealed interface SteakTimerState {

    /** 설정 대기. 아직 시작 전. */
    data object Idle : SteakTimerState

    /** 카운트다운 진행 중. 시간이 흐른다. */
    data class Running(
        val intervalMs: Long,
        val remainingMs: Long,
        val cycle: Int,
    ) : SteakTimerState

    /** 인터벌 완료. 타이머는 멈춰 있고 사용자의 뒤집기(탭)를 기다린다. 이 구간 동안 진동이 울린다. */
    data class Alerting(
        val intervalMs: Long,
        val cycle: Int,
    ) : SteakTimerState

    /** 종료 확인. 확인하면 Idle, 취소하면 직전 상태로 복귀. */
    data class ConfirmStop(
        val resumeTo: SteakTimerState,
    ) : SteakTimerState
}
