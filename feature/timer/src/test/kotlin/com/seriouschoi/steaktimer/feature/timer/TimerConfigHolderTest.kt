package com.seriouschoi.steaktimer.feature.timer

import kotlin.test.Test
import kotlin.test.assertEquals

/** 설정 홀더의 값/조작/seed 규칙은 순수 로직이라 여기서 단독 테스트한다(Android·코루틴 불필요). */
class TimerConfigHolderTest {

    @Test
    fun `기본값은 60초`() {
        assertEquals(TimerConfigHolder.DEFAULT_SECONDS, TimerConfigHolder().seconds.value)
    }

    @Test
    fun `increase decrease는 스텝만큼 조절한다`() {
        val holder = TimerConfigHolder()
        val base = TimerConfigHolder.DEFAULT_SECONDS
        val step = TimerConfigHolder.STEP_SECONDS

        holder.increase()
        assertEquals(base + step, holder.seconds.value)

        holder.decrease()
        holder.decrease()
        assertEquals(base - step, holder.seconds.value)
    }

    @Test
    fun `최소·최대 범위를 넘지 않는다`() {
        val holder = TimerConfigHolder()
        repeat(100) { holder.decrease() }
        assertEquals(TimerConfigHolder.MIN_SECONDS, holder.seconds.value)
        repeat(100) { holder.increase() }
        assertEquals(TimerConfigHolder.MAX_SECONDS, holder.seconds.value)
    }

    @Test
    fun `유효한 프리셋 seed는 반영된다`() {
        val holder = TimerConfigHolder()
        holder.seed(TimerLaunch(presetSeconds = 30))
        assertEquals(30, holder.seconds.value)
    }

    @Test
    fun `범위 밖·없는 프리셋 seed는 현재 값을 유지한다`() {
        val holder = TimerConfigHolder()
        val base = TimerConfigHolder.DEFAULT_SECONDS
        val step = TimerConfigHolder.STEP_SECONDS

        holder.seed(TimerLaunch(presetSeconds = TimerConfigHolder.MIN_SECONDS - 1)) // 최소 미만
        assertEquals(base, holder.seconds.value)
        holder.seed(TimerLaunch(presetSeconds = TimerConfigHolder.MAX_SECONDS + 1)) // 최대 초과
        assertEquals(base, holder.seconds.value)
        holder.seed(TimerLaunch())                                                   // PRESET_NONE
        assertEquals(base, holder.seconds.value)

        // 조절된 값도 잘못된 seed로는 안 덮인다(복귀 시 직전 설정 보존).
        holder.increase()
        holder.seed(TimerLaunch(presetSeconds = TimerLaunch.PRESET_NONE))
        assertEquals(base + step, holder.seconds.value)
    }
}
