package com.seriouschoi.steaktimer.core.platform

import com.seriouschoi.steaktimer.domain.TimerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 코루틴 delay 기반 TimerEngine 구현.
 *
 * periodMs마다 그만큼의 경과를 방출한다. 구독을 취소하면 멈춘다.
 * 화면 꺼짐/도즈 상황에서도 정확히 울리는 신뢰성 강화(ForegroundService/AlarmManager)는 Phase 7.
 */
class CoroutineTimerEngine @Inject constructor() : TimerEngine {

    override fun ticks(periodMs: Long): Flow<Long> = flow {
        while (true) {
            delay(periodMs)
            emit(periodMs)
        }
    }
}
