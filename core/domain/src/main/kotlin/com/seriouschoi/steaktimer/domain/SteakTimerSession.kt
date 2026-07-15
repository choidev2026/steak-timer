package com.seriouschoi.steaktimer.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 순수 상태기계([reduce])를 실제로 굴리는 얇은 세션 홀더.
 *
 * - 현재 상태를 [state]로 노출한다.
 * - 사용자 입력(start/advance/requestStop/...)을 [dispatch]로 받아 [reduce]로 갱신한다.
 * - "Running인가"만 보고 tick 구독을 켜고 끈다. 이 스위칭은 [flatMapLatest]가 담당한다.
 *
 * tick 구독을 수동 Job으로 관리하지 않는 이유:
 * Job.cancel()은 협조적(비동기)이라 "cancel 직후 새 collect launch"를 손으로 하면
 * 이전 collector가 아직 살아 있어 stale tick이 끼거나 순서가 꼬일 수 있다.
 * flatMapLatest는 상위 값이 바뀌면 이전 inner flow를 취소하고 그 취소된 inner의
 * 방출을 하위로 전달하지 않음을 보장하므로, 그 race가 원천적으로 없다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SteakTimerSession(
    engine: TimerEngine,
    scope: CoroutineScope,
    tickPeriodMs: Long = TimerEngine.DEFAULT_PERIOD_MS,
) {
    private val _state = MutableStateFlow<SteakTimerState>(SteakTimerState.Idle)
    val state: StateFlow<SteakTimerState> = _state.asStateFlow()

    init {
        // Running으로 진입하면 엔진 tick 구독, 벗어나면 자동 취소.
        // remaining만 바뀌는 Running 내부 전이는 distinctUntilChanged가 걸러 재구독하지 않는다.
        scope.launch {
            _state
                .map { it is SteakTimerState.Running }
                .distinctUntilChanged()
                .flatMapLatest { running ->
                    if (running) engine.ticks(tickPeriodMs) else emptyFlow()
                }
                .collect { elapsed -> dispatch(TimerIntent.Tick(elapsed)) }
        }
    }

    fun start(intervalMs: Long) = dispatch(TimerIntent.Start(intervalMs))
    fun advance() = dispatch(TimerIntent.Advance)
    fun requestStop() = dispatch(TimerIntent.RequestStop)
    fun confirmStop() = dispatch(TimerIntent.ConfirmStop)
    fun cancelStop() = dispatch(TimerIntent.CancelStop)

    @Synchronized
    private fun dispatch(intent: TimerIntent) {
        val prev = _state.value
        val next = prev.reduce(intent)
        if (next == prev) return
        _state.value = next
    }
}
