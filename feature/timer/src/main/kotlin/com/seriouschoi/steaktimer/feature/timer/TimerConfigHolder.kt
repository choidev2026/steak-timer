package com.seriouschoi.steaktimer.feature.timer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 사용자가 **맞춰둔 설정(intended)** 을 갖는 앱스코프 홀더.
 *
 * `SteakTimerSession`이 "지금 도는 카운트다운(active)"을 갖는 것과 짝을 이루는, "무엇으로 시작할지"의
 * 단일 진실원이다. 앱 수명 동안 살아있으므로:
 * - 타이머가 끝나고 설정 화면으로 돌아와도 **직전 설정이 유지**된다(기본값 리셋 아님).
 * - 타일 프리셋 같은 런치 입력은 [seed]로 한 번 주입된다(nav-arg로 실어나르지 않음).
 *
 * (나중에 DataStore 영속화를 붙이면 "마지막 인터벌 기억"도 이 홀더로 자연히 들어온다.)
 */
@Singleton
class TimerConfigHolder @Inject constructor() {

    private val _seconds = MutableStateFlow(DEFAULT_SECONDS)

    /** 현재 맞춰둔 간격(초). */
    val seconds: StateFlow<Int> = _seconds.asStateFlow()

    /**
     * 런치 입력으로 초기값을 주입한다. 프리셋이 유효 범위 안이면 그 값으로 세팅,
     * 아니면(없음/범위 밖) **현재 값을 유지**한다(복귀 시 직전 설정 보존).
     */
    fun seed(launch: TimerLaunch) {
        val preset = launch.presetSeconds
        if (preset in MIN_SECONDS..MAX_SECONDS) _seconds.value = preset
    }

    fun increase() = _seconds.update { (it + STEP_SECONDS).coerceAtMost(MAX_SECONDS) }
    fun decrease() = _seconds.update { (it - STEP_SECONDS).coerceAtLeast(MIN_SECONDS) }

    companion object {
        const val DEFAULT_SECONDS = 60   // 기본 1분
        const val MIN_SECONDS = 10
        const val MAX_SECONDS = 600      // 10분
        const val STEP_SECONDS = 10
    }
}
