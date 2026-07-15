package com.seriouschoi.steaktimer.core.timersession

import com.seriouschoi.steaktimer.domain.SteakTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AlertingObserverTest {

    private val interval = 1_000L

    @Test
    fun `Alerting 진입에 onEnter, 이탈에 onLeave`() = runTest {
        val state = MutableStateFlow<SteakTimerState>(
            SteakTimerState.Running(interval, interval, cycle = 0),
        )
        var enter = 0
        var leave = 0
        val job = backgroundScope.launch {
            observeAlerting(state, onEnter = { enter++ }, onLeave = { leave++ })
        }
        runCurrent()
        assertEquals(0, enter) // 아직 Alerting 아님

        state.value = SteakTimerState.Alerting(interval, cycle = 0)
        runCurrent()
        assertEquals(1, enter)
        assertEquals(0, leave)

        state.value = SteakTimerState.Running(interval, interval, cycle = 1) // 이탈
        runCurrent()
        assertEquals(1, leave)

        job.cancel()
    }

    @Test
    fun `첫 알림 전 비-Alerting 전이는 onLeave를 부르지 않는다`() = runTest {
        val state = MutableStateFlow<SteakTimerState>(SteakTimerState.Idle)
        var enter = 0
        var leave = 0
        val job = backgroundScope.launch {
            observeAlerting(state, onEnter = { enter++ }, onLeave = { leave++ })
        }
        runCurrent()

        state.value = SteakTimerState.Running(interval, interval, cycle = 0)
        runCurrent()
        assertEquals(0, leave) // dropWhile로 첫 알림 전은 흘려보냄
        assertEquals(0, enter)

        job.cancel()
    }
}
