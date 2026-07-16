package com.seriouschoi.steaktimer.core.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTimerEngineTest {

    @Test
    fun `방출값은 지난 tick 이후 실제 경과 - 슬립 갭은 한 번의 큰 경과로 보정`() = runTest {
        // 시계를 직접 제어: delay(가상시간)와 '실제 시각'을 분리해 딥슬립을 재현한다.
        var fakeNow = 0L
        val engine = CoroutineTimerEngine(now = { fakeNow })
        val emitted = mutableListOf<Long>()
        val job = backgroundScope.launch { engine.ticks(periodMs = 100).collect { emitted += it } }
        runCurrent() // 코루틴 시작 → 첫 delay 전에 last = now() = 0 캡처

        // 정상 tick 2번: 시계가 주기만큼 흐름
        fakeNow = 100; advanceTimeBy(100); runCurrent()
        fakeNow = 200; advanceTimeBy(100); runCurrent()
        assertEquals(listOf(100L, 100L), emitted)

        // 딥슬립 갭: 가상 delay는 100인데 실제 시계는 5000ms 점프 → 한 번의 큰 경과(5000) 방출
        fakeNow = 5200; advanceTimeBy(100); runCurrent()
        assertEquals(listOf(100L, 100L, 5000L), emitted)

        job.cancel()
    }
}
