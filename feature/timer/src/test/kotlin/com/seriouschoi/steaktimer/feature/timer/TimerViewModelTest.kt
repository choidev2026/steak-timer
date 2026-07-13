package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.TimerEngine
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** tick을 흘리지 않는 테스트용 엔진. dispatch 배선 검증엔 시간 진행이 필요 없다. */
private class FakeTimerEngine : TimerEngine {
    private val shared = MutableSharedFlow<Long>()
    override fun ticks(periodMs: Long): Flow<Long> = shared
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val mainDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = kotlinx.coroutines.Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = kotlinx.coroutines.Dispatchers.resetMain()

    @Test
    fun `LongPress는 종료확인을 띄우고 CancelStop은 직전 상태로 복귀시킨다`() =
        runTest(mainDispatcher.scheduler) {
            val vm = TimerViewModel(FakeTimerEngine()) // init에서 기본 간격 자동 start → Running
            // uiState는 WhileSubscribed라 구독자가 있어야 상위가 돈다.
            val job = backgroundScope.launch { vm.uiState.collect { } }
            runCurrent()

            assertFalse(vm.uiState.value.showStopConfirm) // Running

            vm.dispatch(TimerUiIntent.LongPress)
            runCurrent()
            assertTrue(vm.uiState.value.showStopConfirm) // ConfirmStop

            vm.dispatch(TimerUiIntent.CancelStop)
            runCurrent()
            assertFalse(vm.uiState.value.showStopConfirm) // 복귀(Running)

            job.cancel()
        }
}
