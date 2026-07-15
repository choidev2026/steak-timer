package com.seriouschoi.steaktimer.feature.timer.setup

import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
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
import kotlin.test.assertTrue

/** 설정 화면은 엔진 tick이 필요 없다. 계약만 채우는 빈 엔진. */
private class NoopTimerEngine : TimerEngine {
    override fun ticks(periodMs: Long): Flow<Long> = MutableSharedFlow()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `증가·감소는 스텝만큼 조절한다`() =
        runTest(mainDispatcher.scheduler) {
            val vm = SetupViewModel(SteakTimerSession(NoopTimerEngine(), backgroundScope))
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            val start = vm.uiState.value.seconds // 기본 60
            assertEquals(SetupViewModel.DEFAULT_SETUP_SECONDS, start)

            vm.dispatch(SetupUiIntent.Increase)
            runCurrent()
            assertEquals(start + SetupViewModel.STEP_SECONDS, vm.uiState.value.seconds)

            vm.dispatch(SetupUiIntent.Decrease)
            vm.dispatch(SetupUiIntent.Decrease)
            runCurrent()
            assertEquals(start - SetupViewModel.STEP_SECONDS, vm.uiState.value.seconds)

            job.cancel()
        }

    @Test
    fun `감소는 최소값 아래로 내려가지 않는다`() =
        runTest(mainDispatcher.scheduler) {
            val vm = SetupViewModel(SteakTimerSession(NoopTimerEngine(), backgroundScope))
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            repeat(100) { vm.dispatch(SetupUiIntent.Decrease) }
            runCurrent()
            assertEquals(SetupViewModel.MIN_SECONDS, vm.uiState.value.seconds)

            job.cancel()
        }

    @Test
    fun `Start는 고른 간격으로 세션을 시작시킨다`() =
        runTest(mainDispatcher.scheduler) {
            val session = SteakTimerSession(NoopTimerEngine(), backgroundScope)
            val vm = SetupViewModel(session)
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            vm.dispatch(SetupUiIntent.Increase) // 60 → 70초
            runCurrent()

            vm.dispatch(SetupUiIntent.Start)
            runCurrent()

            val state = session.state.value
            assertTrue(state is SteakTimerState.Running)
            assertEquals(70 * 1000L, (state as SteakTimerState.Running).intervalMs)

            job.cancel()
        }
}
