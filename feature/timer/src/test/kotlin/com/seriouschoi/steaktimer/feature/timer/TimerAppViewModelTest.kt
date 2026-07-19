package com.seriouschoi.steaktimer.feature.timer

import kotlin.test.Test
import kotlin.test.assertEquals

/** 루트 VM은 "런치 입력을 홀더에 1회만 seed"가 전부다. 그 1회성을 검증한다. */
class TimerAppViewModelTest {

    @Test
    fun `seed는 홀더에 프리셋을 반영한다`() {
        val config = TimerConfigHolder()
        val vm = TimerAppViewModel(config)
        vm.seed(TimerLaunch(presetSeconds = 30))
        assertEquals(30, config.seconds.value)
    }

    @Test
    fun `seed는 첫 호출만 적용되고 이후 호출은 무시된다`() {
        val config = TimerConfigHolder()
        val vm = TimerAppViewModel(config)

        vm.seed(TimerLaunch(presetSeconds = 30))
        config.increase() // 사용자가 조절: 30 → 40
        vm.seed(TimerLaunch(presetSeconds = 20)) // 재-seed(회전 등) — 무시돼야 함

        assertEquals(40, config.seconds.value)
    }
}
