package com.seriouschoi.steaktimer.feature.timer

/**
 * 앱을 어떤 입력으로 띄웠는지를 담는 구조화된 런치 입력.
 *
 * 지금은 타일 프리셋뿐이지만, 진입 파라미터가 늘어도 시그니처를 바꾸지 않도록 데이터 클래스로 둔다.
 * `MainActivity`가 intent에서 만들어 [TimerConfigHolder.seed]에 한 번 넘긴다(nav-arg로 실어나르지 않음).
 */
data class TimerLaunch(
    val presetSeconds: Int = PRESET_NONE,
) {
    companion object {
        /** 프리셋 없이 진입했음을 뜻하는 sentinel. */
        const val PRESET_NONE = -1
    }
}
