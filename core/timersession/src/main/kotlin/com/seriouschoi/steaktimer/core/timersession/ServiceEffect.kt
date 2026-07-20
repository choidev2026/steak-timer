package com.seriouschoi.steaktimer.core.timersession

import com.seriouschoi.steaktimer.domain.SteakTimerState
import kotlinx.coroutines.flow.Flow

/**
 * 도메인 상태 **전이**에서 파생되는 서비스 부수효과. 순수 데이터(플랫폼 의존 없음).
 *
 * "무엇을 언제"만 표현하고, 실제 실행(HOW)은 러너가 맡는다. 효과는 수명이 달라 두 러너로 갈린다:
 * - 앱스코프(컨트롤러): [StartService]/[StopService]/[ScheduleAlarm]/[CancelAlarm]
 * - 서비스 안(FGS): [StartAlerting]/[StopAlerting] — 서비스가 살아있어야 걸 수 있는 것들
 */
sealed interface ServiceEffect {
    data object StartService : ServiceEffect
    data object StopService : ServiceEffect
    data class ScheduleAlarm(val afterMs: Long) : ServiceEffect
    data object CancelAlarm : ServiceEffect

    /** WakeLock 획득 + 진동 시작 + 뒤집기 heads-up 알림. */
    data object StartAlerting : ServiceEffect

    /** 진동 정지 + WakeLock 해제 + 뒤집기 알림 제거. */
    data object StopAlerting : ServiceEffect
}

/**
 * 이전→현재 상태 전이에서 나올 효과 목록을 계산하는 **순수 함수**(#35).
 *
 * 세션(단일 상태머신) 하나만 진실원이고, 서비스/컨트롤러는 이 함수로 상태에서 효과를 '파생'한다.
 * Android 의존이 없어 전이 규칙 전체를 계측 없이 유닛테스트할 수 있다.
 *
 * prev=null(관측 시작/서비스 갓 뜸)도 안전하게 다룬다 — 첫 활성 전이만 StartService를 내고,
 * 그전 비교로 spurious한 Stop류가 안 나온다.
 */
fun effectsFor(prev: SteakTimerState?, cur: SteakTimerState): List<ServiceEffect> = buildList {
    val wasActive = prev != null && prev !is SteakTimerState.Idle
    val isActive = cur !is SteakTimerState.Idle
    if (!wasActive && isActive) add(ServiceEffect.StartService)
    if (wasActive && !isActive) add(ServiceEffect.StopService)

    if (isEnteringRunning(prev, cur)) {
        add(ServiceEffect.ScheduleAlarm((cur as SteakTimerState.Running).remainingMs))
    }
    if (isLeavingRunning(prev, cur)) add(ServiceEffect.CancelAlarm)

    if (prev !is SteakTimerState.Alerting && cur is SteakTimerState.Alerting) {
        add(ServiceEffect.StartAlerting)
    }
    if (prev is SteakTimerState.Alerting && cur !is SteakTimerState.Alerting) {
        add(ServiceEffect.StopAlerting)
    }
}

/** 새 러닝 에피소드 진입 — 비-Running에서 오거나 cycle이 바뀐 조기 뒤집기(틱은 cycle 동일이라 제외). */
private fun isEnteringRunning(prev: SteakTimerState?, cur: SteakTimerState): Boolean =
    cur is SteakTimerState.Running &&
        (prev !is SteakTimerState.Running || prev.cycle != cur.cycle)

private fun isLeavingRunning(prev: SteakTimerState?, cur: SteakTimerState): Boolean =
    cur !is SteakTimerState.Running && prev is SteakTimerState.Running

/**
 * 상태 흐름을 전이별 효과로 풀어 [run]에 흘리는 공용 드라이버.
 * 컨트롤러·서비스가 이걸로 관측하고, 각자 **자기 것만** 실행한다(매핑은 [effectsFor] 한 곳).
 */
suspend fun runServiceEffects(state: Flow<SteakTimerState>, run: (ServiceEffect) -> Unit) {
    var prev: SteakTimerState? = null
    state.collect { cur ->
        effectsFor(prev, cur).forEach(run)
        prev = cur
    }
}
