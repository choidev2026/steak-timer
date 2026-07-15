package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.SteakTimerSession
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

private const val INTERVAL = 60_000L

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

    // 세션은 이제 주입 대상. 테스트에선 test 스케줄러에 묶인 backgroundScope로 만들어 넣는다.
    // 시작(start)은 UI 인텐트가 아니라 세션에 직접 발행한다(설정 화면 몫이므로).

    @Test
    fun `세션이 Running이면 타이머 화면 상태를 노출한다`() =
        runTest(mainDispatcher.scheduler) {
            val session = SteakTimerSession(FakeTimerEngine(), backgroundScope)
            val vm = TimerViewModel(session, FakeHaptic())
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            assertTrue(vm.uiState.value.isIdle) // 초기 Idle

            session.start(INTERVAL)
            runCurrent()
            assertFalse(vm.uiState.value.isIdle) // Running → 타이머 진행
            assertEquals(TimeFormat.mmSs(INTERVAL), vm.uiState.value.timeText)

            job.cancel()
        }

    @Test
    fun `정지 확정 시 isIdle이 되어 화면 이탈을 알린다`() =
        runTest(mainDispatcher.scheduler) {
            val session = SteakTimerSession(FakeTimerEngine(), backgroundScope)
            val vm = TimerViewModel(session, FakeHaptic())
            val job = backgroundScope.launch { vm.uiState.collect { } }
            session.start(INTERVAL)
            runCurrent()
            assertFalse(vm.uiState.value.isIdle)

            vm.dispatch(TimerUiIntent.LongPress)
            runCurrent()
            assertTrue(vm.uiState.value.showStopConfirm)

            vm.dispatch(TimerUiIntent.ConfirmStop)
            runCurrent()
            assertTrue(vm.uiState.value.isIdle) // 정지 → 설정 화면 복귀 신호

            job.cancel()
        }

    @Test
    fun `LongPress는 종료확인을 띄우고 CancelStop은 직전 상태로 복귀시킨다`() =
        runTest(mainDispatcher.scheduler) {
            val session = SteakTimerSession(FakeTimerEngine(), backgroundScope)
            val vm = TimerViewModel(session, FakeHaptic())
            val job = backgroundScope.launch { vm.uiState.collect { } }
            session.start(INTERVAL)
            runCurrent()

            assertFalse(vm.uiState.value.showStopConfirm)

            vm.dispatch(TimerUiIntent.LongPress)
            runCurrent()
            assertTrue(vm.uiState.value.showStopConfirm)

            vm.dispatch(TimerUiIntent.CancelStop)
            runCurrent()
            assertFalse(vm.uiState.value.showStopConfirm)
            assertFalse(vm.uiState.value.isIdle) // 취소는 실행 상태로 복귀

            job.cancel()
        }

    @Test
    fun `Alerting 진입 시 진동이 시작된다`() =
        runTest(mainDispatcher.scheduler) {
            val engine = FakeTimerEngine()
            val haptic = FakeHaptic()
            val session = SteakTimerSession(engine, backgroundScope)
            val vm = TimerViewModel(session, haptic)
            session.start(INTERVAL)
            runCurrent()
            assertEquals(0, haptic.startCount) // Running, 아직 알림 아님

            engine.emit(INTERVAL) // 인터벌만큼 흘리면 remaining 0 → Alerting
            runCurrent()
            assertEquals(1, haptic.startCount)
            assertEquals(0, haptic.stopCount)
        }

    @Test
    fun `탭하면 Alerting을 벗어나 진동이 정지된다`() =
        runTest(mainDispatcher.scheduler) {
            val engine = FakeTimerEngine()
            val haptic = FakeHaptic()
            val session = SteakTimerSession(engine, backgroundScope)
            val vm = TimerViewModel(session, haptic)
            session.start(INTERVAL)
            runCurrent()

            engine.emit(INTERVAL) // Alerting
            runCurrent()
            assertEquals(1, haptic.startCount)

            vm.dispatch(TimerUiIntent.Tap) // Alerting → 다음 Running
            runCurrent()
            assertEquals(1, haptic.stopCount)
        }
}
