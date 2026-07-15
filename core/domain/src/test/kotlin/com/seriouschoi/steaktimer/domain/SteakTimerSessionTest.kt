package com.seriouschoi.steaktimer.domain

import com.seriouschoi.steaktimer.domain.SteakTimerState.Alerting
import com.seriouschoi.steaktimer.domain.SteakTimerState.ConfirmStop
import com.seriouschoi.steaktimer.domain.SteakTimerState.Running
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val INTERVAL = 3000L

/** 테스트용 엔진. 원할 때 원하는 경과값을 흘려보낸다. */
private class FakeTimerEngine : TimerEngine {
    val shared = MutableSharedFlow<Long>(extraBufferCapacity = 64)
    override fun ticks(periodMs: Long): Flow<Long> = shared
}

@OptIn(ExperimentalCoroutinesApi::class)
class SteakTimerSessionTest {

    @Test
    fun `start 후 tick 누적으로 remaining 감소하다가 Alerting 전이`() = runTest {
        val fake = FakeTimerEngine()
        val session = SteakTimerSession(fake, backgroundScope, tickPeriodMs = 1000)

        session.start(INTERVAL)
        runCurrent() // tickJob 구독 시작
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 0), session.state.value)

        fake.shared.emit(1000); runCurrent()
        assertEquals(Running(INTERVAL, INTERVAL - 1000, cycle = 0), session.state.value)

        fake.shared.emit(1000); runCurrent()
        assertEquals(Running(INTERVAL, INTERVAL - 2000, cycle = 0), session.state.value)

        fake.shared.emit(1000); runCurrent()
        assertEquals(Alerting(INTERVAL, cycle = 0), session.state.value)
    }

    @Test
    fun `Alerting에서 advance하면 다음 인터벌로 엔진 재구독`() = runTest {
        val fake = FakeTimerEngine()
        val session = SteakTimerSession(fake, backgroundScope, tickPeriodMs = 1000)

        session.start(INTERVAL); runCurrent()
        fake.shared.emit(INTERVAL); runCurrent() // 한 방에 완주 -> Alerting
        assertEquals(Alerting(INTERVAL, cycle = 0), session.state.value)

        session.advance(); runCurrent() // 정상 뒤집기 -> Running cycle 1, 재구독
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 1), session.state.value)

        fake.shared.emit(1000); runCurrent()
        assertEquals(Running(INTERVAL, INTERVAL - 1000, cycle = 1), session.state.value)
    }

    @Test
    fun `ConfirmStop 동안엔 tick이 상태를 바꾸지 않고 취소하면 재개된다`() = runTest {
        val fake = FakeTimerEngine()
        val session = SteakTimerSession(fake, backgroundScope, tickPeriodMs = 1000)

        session.start(INTERVAL); runCurrent()
        session.requestStop(); runCurrent() // Running -> ConfirmStop, 구독 중단
        val confirm = ConfirmStop(resumeTo = Running(INTERVAL, INTERVAL, cycle = 0))
        assertEquals(confirm, session.state.value)

        // 구독 중단 상태라 tick은 흘려도 무시됨(구독자 없음)
        fake.shared.emit(1000); runCurrent()
        assertEquals(confirm, session.state.value)

        session.cancelStop(); runCurrent() // -> Running 복귀, 재구독
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 0), session.state.value)

        fake.shared.emit(1000); runCurrent()
        assertEquals(Running(INTERVAL, INTERVAL - 1000, cycle = 0), session.state.value)
    }
}
