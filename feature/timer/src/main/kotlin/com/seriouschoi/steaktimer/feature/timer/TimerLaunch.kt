package com.seriouschoi.steaktimer.feature.timer

import android.content.Intent

/**
 * 앱을 어떤 입력으로 띄웠는지를 담는 구조화된 런치 입력.
 *
 * 지금은 타일 프리셋뿐이지만, 진입 파라미터가 늘어도 시그니처를 바꾸지 않도록 데이터 클래스로 둔다.
 * `MainActivity`가 intent에서 [toTimerLaunch]로 만들어 `TimerApp`에 넘긴다(nav-arg로 실어나르지 않음).
 */
data class TimerLaunch(
    val presetSeconds: Int = PRESET_NONE,
) {
    companion object {
        /** 프리셋 없이 진입했음을 뜻하는 sentinel. */
        const val PRESET_NONE = -1
    }
}

/**
 * 타일이 실어 보내는 프리셋 extra 키. `:feature:tile`의 같은 이름 상수와 **문자열이 일치**해야 한다.
 * (인텐트는 느슨한 결합 경계 = 직렬화 계약이라, 발행자·수신자가 키를 각자 정의하는 건 그 비용이다.)
 */
const val EXTRA_PRESET_SECONDS = "preset_seconds"

/**
 * intent에서 런치 입력을 뽑아낸다. **디코드 계약을 `TimerLaunch` 소유 모듈(여기)에 둬서**
 * `:app`은 extra 키를 몰라도 되게 한다 — `MainActivity`는 `intent.toTimerLaunch()`만 부른다.
 */
fun Intent.toTimerLaunch(): TimerLaunch =
    TimerLaunch(presetSeconds = getIntExtra(EXTRA_PRESET_SECONDS, TimerLaunch.PRESET_NONE))
