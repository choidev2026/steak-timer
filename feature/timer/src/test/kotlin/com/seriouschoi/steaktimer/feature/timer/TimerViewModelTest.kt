package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.TimerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 필요할 때 경과를 흘려보내는 테스트용 엔진. */
private class FakeTimerEngine : TimerEngine {
    private val shared = MutableSharedFlow<Long>(extraBufferCapacity = 8)
    override fun ticks(periodMs: Long): Flow<Long> = shared
    suspend fun emit(ms: Long) = shared.emit(ms)
}

/** 진동 호출을 세는 테스트용 Haptic. */
private class FakeHaptic : Haptic {
    var startCount = 0
    var stopCount = 0
    override fun startAlert() { startCount++ }
    override fun stop() { stopCount++ }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `LongPress는 종료확인을 띄우고 CancelStop은 직전 상태로 복귀시킨다`() =
        runTest(mainDispatcher.scheduler) {
            val vm = TimerViewModel(FakeTimerEngine(), FakeHaptic())
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            assertFalse(vm.uiState.value.showStopConfirm)

            vm.dispatch(TimerUiIntent.LongPress)
            runCurrent()
            assertTrue(vm.uiState.value.showStopConfirm)

            vm.dispatch(TimerUiIntent.CancelStop)
            runCurrent()
            assertFalse(vm.uiState.value.showStopConfirm)

            job.cancel()
        }

    @Test
    fun `Alerting 진입 시 진동이 시작된다`() =
        runTest(mainDispatcher.scheduler) {
            val engine = FakeTimerEngine()
            val haptic = FakeHaptic()
            TimerViewModel(engine, haptic) // init에서 기본 간격(DEFAULT_INTERVAL_MS)으로 자동 start → Running
            runCurrent()
            assertEquals(0, haptic.startCount) // 아직 Running(알림 아님)

            // 기본 간격만큼 흘리면 remaining 0 → Alerting 진입
            engine.emit(TimerViewModel.DEFAULT_INTERVAL_MS)
            runCurrent()
            assertEquals(1, haptic.startCount)
            assertEquals(0, haptic.stopCount)
        }

    @Test
    fun `탭하면 Alerting을 벗어나 진동이 정지된다`() =
        runTest(mainDispatcher.scheduler) {
            val engine = FakeTimerEngine()
            val haptic = FakeHaptic()
            val vm = TimerViewModel(engine, haptic)
            runCurrent()

            engine.emit(TimerViewModel.DEFAULT_INTERVAL_MS) // 기본 간격만큼 흘려 Alerting 진입
            runCurrent()
            assertEquals(1, haptic.startCount)

            vm.dispatch(TimerUiIntent.Tap) // Alerting → 다음 Running
            runCurrent()
            assertEquals(1, haptic.stopCount)
        }
}
