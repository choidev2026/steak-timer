package com.seriouschoi.steaktimer.domain

import com.seriouschoi.steaktimer.domain.SteakTimerState.Alerting
import com.seriouschoi.steaktimer.domain.SteakTimerState.ConfirmStop
import com.seriouschoi.steaktimer.domain.SteakTimerState.Idle
import com.seriouschoi.steaktimer.domain.SteakTimerState.Running
import kotlin.test.Test
import kotlin.test.assertEquals

private const val INTERVAL = 180_000L // 3분

class TimerReducerTest {

    @Test
    fun `Idle에서 Start하면 Running 초기 상태`() {
        val next = Idle.reduce(TimerIntent.Start(INTERVAL))
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 0), next)
    }

    @Test
    fun `Idle은 Start 외 입력을 무시한다`() {
        assertEquals(Idle, Idle.reduce(TimerIntent.Advance))
        assertEquals(Idle, Idle.reduce(TimerIntent.RequestStop))
        assertEquals(Idle, Idle.reduce(TimerIntent.Tick(1000)))
    }

    @Test
    fun `Tick은 remaining을 감소시킨다`() {
        val running = Running(INTERVAL, INTERVAL, cycle = 0)
        val next = running.reduce(TimerIntent.Tick(1000))
        assertEquals(Running(INTERVAL, INTERVAL - 1000, cycle = 0), next)
    }

    @Test
    fun `Tick 누적으로 0에 도달하면 Alerting으로 전이(cycle 유지)`() {
        var state: SteakTimerState = Running(INTERVAL, 3000, cycle = 2)
        state = state.reduce(TimerIntent.Tick(1000))
        state = state.reduce(TimerIntent.Tick(1000))
        assertEquals(Running(INTERVAL, 1000, cycle = 2), state)
        state = state.reduce(TimerIntent.Tick(1000))
        assertEquals(Alerting(INTERVAL, cycle = 2), state)
    }

    @Test
    fun `Tick이 0을 넘겨도(overshoot) Alerting`() {
        val running = Running(INTERVAL, 500, cycle = 0)
        val next = running.reduce(TimerIntent.Tick(5000))
        assertEquals(Alerting(INTERVAL, cycle = 0), next)
    }

    @Test
    fun `Alerting에서 Advance하면 다음 인터벌 Running + cycle+1`() {
        val alerting = Alerting(INTERVAL, cycle = 2)
        val next = alerting.reduce(TimerIntent.Advance)
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 3), next)
    }

    @Test
    fun `Running에서 Advance하면 조기 뒤집기 재시작 + cycle+1`() {
        val running = Running(INTERVAL, 42_000, cycle = 1)
        val next = running.reduce(TimerIntent.Advance)
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 2), next)
    }

    @Test
    fun `Running에서 RequestStop하면 ConfirmStop(resumeTo 보존)`() {
        val running = Running(INTERVAL, 42_000, cycle = 1)
        val next = running.reduce(TimerIntent.RequestStop)
        assertEquals(ConfirmStop(resumeTo = running), next)
    }

    @Test
    fun `Alerting에서는 RequestStop을 무시한다`() {
        val alerting = Alerting(INTERVAL, cycle = 0)
        assertEquals(alerting, alerting.reduce(TimerIntent.RequestStop))
    }

    @Test
    fun `Alerting에서는 Tick을 무시한다(시간 정지)`() {
        val alerting = Alerting(INTERVAL, cycle = 0)
        assertEquals(alerting, alerting.reduce(TimerIntent.Tick(1000)))
    }

    @Test
    fun `ConfirmStop에서 확인하면 Idle`() {
        val confirm = ConfirmStop(resumeTo = Running(INTERVAL, 10_000, cycle = 1))
        assertEquals(Idle, confirm.reduce(TimerIntent.ConfirmStop))
    }

    @Test
    fun `ConfirmStop에서 취소하면 직전 상태로 복귀`() {
        val running = Running(INTERVAL, 10_000, cycle = 1)
        val confirm = ConfirmStop(resumeTo = running)
        assertEquals(running, confirm.reduce(TimerIntent.CancelStop))
    }

    @Test
    fun `핵심 루프 - 시작 후 완주 뒤집기 조기 뒤집기가 cycle을 누적`() {
        var s: SteakTimerState = Idle
        s = s.reduce(TimerIntent.Start(INTERVAL))           // Running cycle 0
        s = s.reduce(TimerIntent.Tick(INTERVAL))            // Alerting cycle 0
        s = s.reduce(TimerIntent.Advance)                       // 정상 뒤집기 -> Running cycle 1
        s = s.reduce(TimerIntent.Advance)                       // 조기 뒤집기 -> Running cycle 2
        assertEquals(Running(INTERVAL, INTERVAL, cycle = 2), s)
    }
}
