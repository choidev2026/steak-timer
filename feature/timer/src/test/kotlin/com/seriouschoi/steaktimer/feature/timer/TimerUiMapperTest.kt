package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.domain.SteakTimerState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimerUiMapperTest {

    @Test
    fun `Running은 남은시간과 진행률을 표시로 변환`() {
        val ui = SteakTimerState.Running(intervalMs = 10_000, remainingMs = 8_000, cycle = 0).toUiState()
        assertEquals("00:08", ui.timeText)
        assertEquals(0.8f, ui.progress)
        assertFalse(ui.isVibrating)
        assertFalse(ui.showStopConfirm)
        assertFalse(ui.isIdle) // Running은 진행 중
    }

    @Test
    fun `Alerting은 진동 상태와 안내를 표시`() {
        val ui = SteakTimerState.Alerting(intervalMs = 10_000, cycle = 1).toUiState()
        assertTrue(ui.isVibrating)
        assertEquals("탭해서 다음", ui.hint)
        assertFalse(ui.showStopConfirm)
    }

    @Test
    fun `ConfirmStop은 직전 상태 위에 종료확인 플래그를 얹는다`() {
        val running = SteakTimerState.Running(intervalMs = 10_000, remainingMs = 5_000, cycle = 0)
        val ui = SteakTimerState.ConfirmStop(resumeTo = running).toUiState()
        assertTrue(ui.showStopConfirm)
        assertEquals("00:05", ui.timeText) // resumeTo(Running) 기준 표시 유지
    }

    @Test
    fun `Idle은 초기 표시(isIdle)`() {
        val ui = SteakTimerState.Idle.toUiState()
        assertEquals(TimerUiState.INITIAL, ui)
        assertTrue(ui.isIdle)
    }
}
