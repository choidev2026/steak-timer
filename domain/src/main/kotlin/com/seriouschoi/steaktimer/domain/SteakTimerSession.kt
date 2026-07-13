package com.seriouschoi.steaktimer.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 순수 상태기계([reduce])를 실제로 굴리는 얇은 세션 홀더.
 *
 * - 현재 상태를 [state]로 노출한다.
 * - 사용자 입력(start/tap/longPress/...)을 받아 [reduce]로 상태를 갱신한다.
 * - Running 동안에만 [TimerEngine]의 tick을 구독해 시간 경과를 주입하고,
 *   Running을 벗어나면(Alerting/Idle/ConfirmStop) 구독을 멈춘다.
 *
 * [scope]는 tick 구독이 도는 코루틴 스코프. 상태 전이는 [dispatch]에서 원자적으로 처리한다.
 */
class SteakTimerSession(
    private val engine: TimerEngine,
    private val scope: CoroutineScope,
    private val tickPeriodMs: Long = TimerEngine.DEFAULT_PERIOD_MS,
) {
    private val _state = MutableStateFlow<SteakTimerState>(SteakTimerState.Idle)
    val state: StateFlow<SteakTimerState> = _state.asStateFlow()

    private var tickJob: Job? = null

    fun start(intervalMs: Long) = dispatch(TimerIntent.Start(intervalMs))
    fun tap() = dispatch(TimerIntent.Tap)
    fun longPress() = dispatch(TimerIntent.LongPress)
    fun confirmStop() = dispatch(TimerIntent.ConfirmStop)
    fun cancelStop() = dispatch(TimerIntent.CancelStop)

    @Synchronized
    private fun dispatch(intent: TimerIntent) {
        val prev = _state.value
        val next = prev.reduce(intent)
        if (next == prev) return
        _state.value = next
        syncTicking(prev, next)
    }

    /**
     * Running으로 '진입'하면 tick 구독 시작, Running에서 '이탈'하면 중단.
     * Running 내부 전이(tick 감소, tap 재시작)에서는 기존 구독을 유지한다.
     */
    private fun syncTicking(prev: SteakTimerState, next: SteakTimerState) {
        val wasRunning = prev is SteakTimerState.Running
        val isRunning = next is SteakTimerState.Running
        when {
            isRunning && !wasRunning -> startTicking()
            !isRunning && wasRunning -> stopTicking()
        }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            engine.ticks(tickPeriodMs).collect { elapsed ->
                dispatch(TimerIntent.Tick(elapsed))
            }
        }
    }

    private fun stopTicking() {
        tickJob?.cancel()
        tickJob = null
    }
}
