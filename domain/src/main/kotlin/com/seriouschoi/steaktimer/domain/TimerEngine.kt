package com.seriouschoi.steaktimer.domain

import kotlinx.coroutines.flow.Flow

/**
 * 시간 공급 Port. 실제 구현(코루틴/AlarmManager 등)은 바깥 계층(:core:platform).
 *
 * 도메인은 "시간이 얼마나 흘렀는지"만 알면 되고, 어떻게 흘리는지는 모른다.
 */
interface TimerEngine {

    /**
     * 구독하면 [periodMs] 간격으로 '지난 tick 이후 경과(ms)'를 방출한다.
     * 구독을 취소하면 멈춘다.
     */
    fun ticks(periodMs: Long = DEFAULT_PERIOD_MS): Flow<Long>

    companion object {
        const val DEFAULT_PERIOD_MS = 200L
    }
}
