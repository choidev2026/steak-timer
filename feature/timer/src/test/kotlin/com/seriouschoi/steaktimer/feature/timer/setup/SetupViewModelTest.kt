package com.seriouschoi.steaktimer.feature.timer.setup

import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
import com.seriouschoi.steaktimer.domain.TimerEngine
import com.seriouschoi.steaktimer.feature.timer.TimerConfigHolder
import com.seriouschoi.steaktimer.feature.timer.TimerLaunch
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

/**
 * SetupViewModel은 이제 얇은 어댑터 — 간격 상태/조작 규칙은 [TimerConfigHolder]가 소유한다.
 * 그래서 여기선 "홀더를 반영하는가 / 홀더로 위임하는가 / 시작 시 홀더 값으로 세션을 켜는가"만 본다.
 * (조작·seed 규칙 자체는 TimerConfigHolderTest에서 검증)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState는 홀더의 현재 값을 반영하고, 조작은 홀더로 위임된다`() =
        runTest(mainDispatcher.scheduler) {
            val config = TimerConfigHolder()
            val vm = SetupViewModel(config, SteakTimerSession(NoopTimerEngine(), backgroundScope))
            assertEquals(config.seconds.value, vm.uiState.value.seconds) // 초기값 = 홀더 값(기본)

            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            // 기대값을 매직넘버가 아니라 "기본값 + 한 스텝"으로 명시.
            val expected = TimerConfigHolder.DEFAULT_SECONDS + TimerConfigHolder.STEP_SECONDS
            vm.dispatch(SetupUiIntent.Increase)
            runCurrent()
            assertEquals(expected, config.seconds.value)     // 홀더로 위임됨
            assertEquals(expected, vm.uiState.value.seconds) // uiState가 홀더를 반영

            job.cancel()
        }

    @Test
    fun `Start는 홀더의 현재 간격으로 세션을 시작시킨다`() =
        runTest(mainDispatcher.scheduler) {
            val config = TimerConfigHolder().apply { seed(TimerLaunch(presetSeconds = 30)) }
            val session = SteakTimerSession(NoopTimerEngine(), backgroundScope)
            val vm = SetupViewModel(config, session)
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            vm.dispatch(SetupUiIntent.Start)
            runCurrent()

            val state = session.state.value
            assertTrue(state is SteakTimerState.Running)
            assertEquals(30 * 1000L, (state as SteakTimerState.Running).intervalMs)

            job.cancel()
        }
}
