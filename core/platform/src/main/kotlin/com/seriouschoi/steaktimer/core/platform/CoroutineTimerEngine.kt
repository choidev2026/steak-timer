package com.seriouschoi.steaktimer.core.platform

import android.os.SystemClock
import com.seriouschoi.steaktimer.domain.TimerEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/**
 * 코루틴 delay 기반 TimerEngine 구현.
 *
 * periodMs를 목표 주기로 삼되, 방출값은 **'지난 tick 이후 실제 경과(ms)'**다.
 * 시각은 [SystemClock.elapsedRealtime]로 재는데, 이건 **딥슬립 시간을 포함**한다(uptimeMillis는 제외).
 * 그래서 화면 꺼짐/딥슬립으로 delay가 얼었다 깨어나면, 밀렸던 만큼이 **한 번의 큰 경과**로 방출돼
 * 남은시간 표시가 즉시 실제값으로 보정된다(#28).
 *
 * 정확한 완주 시각 보장은 여전히 AlarmManager(#23) 몫. 이 엔진은 '표시용 경과'를 정확히 낸다.
 */
class CoroutineTimerEngine internal constructor(
    private val now: () -> Long,
) : TimerEngine {

    @Inject constructor() : this({ SystemClock.elapsedRealtime() })

    override fun ticks(periodMs: Long): Flow<Long> = flow {
        var last = now()
        while (true) {
            delay(periodMs)
            val current = now()
            emit(current - last) // 고정 periodMs가 아니라 실제 경과(딥슬립 포함)
            last = current
        }
    }
}
