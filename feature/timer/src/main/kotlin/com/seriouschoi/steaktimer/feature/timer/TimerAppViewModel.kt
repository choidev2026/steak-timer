package com.seriouschoi.steaktimer.feature.timer

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 앱 루트의 얇은 ViewModel — 유일한 일은 **런치 입력을 설정 홀더에 한 번 seeding** 하는 것.
 *
 * "1회성"은 ViewModel 수명으로 보장된다: 이 VM은 화면 구성 변경(회전 등)을 넘어 살아있으므로,
 * [seeded] 플래그가 재생성 뒤에도 유지돼 재-seed로 사용자 조절값/직전 설정을 덮어쓰지 않는다.
 * (그래서 Activity의 savedInstanceState 가드가 여기로 흡수된다.)
 *
 * per-screen VM/세션과 역할이 겹치지 않게 이 이상은 하지 않는다.
 */
@HiltViewModel
class TimerAppViewModel @Inject constructor(
    private val config: TimerConfigHolder,
) : ViewModel() {

    private var seeded = false

    fun seed(launch: TimerLaunch) {
        if (seeded) return
        seeded = true
        config.seed(launch)
    }
}
