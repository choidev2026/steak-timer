package com.seriouschoi.steaktimer.core.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTimerEngineTest {

    @Test
    fun `periodMs 간격으로 경과를 방출한다`() = runTest {
        val engine = CoroutineTimerEngine()
        // 가상시간이라 실제로 기다리지 않고 delay가 즉시 진행됨
        val emitted = engine.ticks(periodMs = 100).take(3).toList()
        assertEquals(listOf(100L, 100L, 100L), emitted)
    }
}
