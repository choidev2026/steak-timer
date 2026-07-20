package com.seriouschoi.steaktimer.core.timersession

import com.seriouschoi.steaktimer.domain.SteakTimerState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * effectsFor는 순수 함수라 상태 전이 규칙 전체를 계측 없이 검증한다(#35).
 * 각 전이가 정확히 어떤 효과 집합을 내는지 고정한다.
 */
class ServiceEffectTest {

    private fun running(remaining: Long = 30_000L, cycle: Int = 0) =
        SteakTimerState.Running(intervalMs = 60_000L, remainingMs = remaining, cycle = cycle)

    private fun alerting(cycle: Int = 0) =
        SteakTimerState.Alerting(intervalMs = 60_000L, cycle = cycle)

    @Test
    fun `부팅 - null에서 Idle은 아무 효과 없음`() {
        assertEquals(emptyList(), effectsFor(null, SteakTimerState.Idle))
    }

    @Test
    fun `Idle에서 Running - 서비스 시작 + 알람 예약`() {
        assertEquals(
            listOf(ServiceEffect.StartService, ServiceEffect.ScheduleAlarm(30_000L)),
            effectsFor(SteakTimerState.Idle, running(remaining = 30_000L)),
        )
    }

    @Test
    fun `Running 틱 - 같은 cycle에서 remaining만 바뀌면 효과 없음`() {
        assertEquals(
            emptyList(),
            effectsFor(running(remaining = 30_000L, cycle = 0), running(remaining = 29_000L, cycle = 0)),
        )
    }

    @Test
    fun `조기 뒤집기 - cycle이 바뀐 Running은 알람 재예약만`() {
        assertEquals(
            listOf(ServiceEffect.ScheduleAlarm(60_000L)),
            effectsFor(running(cycle = 0), running(remaining = 60_000L, cycle = 1)),
        )
    }

    @Test
    fun `Running에서 Alerting - 알람 취소 + Alerting 시작`() {
        assertEquals(
            listOf(ServiceEffect.CancelAlarm, ServiceEffect.StartAlerting),
            effectsFor(running(), alerting()),
        )
    }

    @Test
    fun `Alerting에서 다음 Running - 알람 예약 + Alerting 정지`() {
        assertEquals(
            listOf(ServiceEffect.ScheduleAlarm(60_000L), ServiceEffect.StopAlerting),
            effectsFor(alerting(cycle = 0), running(remaining = 60_000L, cycle = 1)),
        )
    }

    @Test
    fun `Alerting에서 Idle - 서비스 정지 + Alerting 정지`() {
        assertEquals(
            listOf(ServiceEffect.StopService, ServiceEffect.StopAlerting),
            effectsFor(alerting(), SteakTimerState.Idle),
        )
    }

    @Test
    fun `Running에서 Idle - 서비스 정지 + 알람 취소`() {
        assertEquals(
            listOf(ServiceEffect.StopService, ServiceEffect.CancelAlarm),
            effectsFor(running(), SteakTimerState.Idle),
        )
    }

    @Test
    fun `Running에서 ConfirmStop - 알람 취소(서비스는 유지)`() {
        val confirm = SteakTimerState.ConfirmStop(resumeTo = running())
        assertEquals(
            listOf(ServiceEffect.CancelAlarm),
            effectsFor(running(), confirm),
        )
    }

    @Test
    fun `ConfirmStop에서 Running 복귀 - 알람 재예약`() {
        val confirm = SteakTimerState.ConfirmStop(resumeTo = running())
        assertEquals(
            listOf(ServiceEffect.ScheduleAlarm(30_000L)),
            effectsFor(confirm, running(remaining = 30_000L)),
        )
    }

    @Test
    fun `서비스 갓 뜸 - null에서 Alerting은 서비스시작 + Alerting시작`() {
        // 프로세스 재시작 등으로 prev=null인 채 Alerting을 만나도 안전하게 StartAlerting을 낸다.
        assertEquals(
            listOf(ServiceEffect.StartService, ServiceEffect.StartAlerting),
            effectsFor(null, alerting()),
        )
    }
}
