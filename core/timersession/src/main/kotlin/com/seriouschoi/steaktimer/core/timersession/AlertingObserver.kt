package com.seriouschoi.steaktimer.core.timersession

import com.seriouschoi.steaktimer.domain.SteakTimerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map

/**
 * 세션 상태에서 Alerting 진입/이탈을 관측해 콜백한다.
 * dropWhile로 첫 알림 전의 비-Alerting은 흘려보내 불필요한 [onLeave] 호출을 막는다.
 *
 * 진동뿐 아니라 WakeLock 획득/해제 같은 부수효과도 함께 걸 수 있도록 콜백형으로 둔다.
 * 이 관측은 화면 꺼짐에서도 동작해야 하므로 살아있음이 보장되는 Foreground Service가 돌린다.
 * 순수 코루틴 로직이라 여기서 분리해 단위 테스트한다.
 */
internal suspend fun observeAlerting(
    state: Flow<SteakTimerState>,
    onEnter: () -> Unit,
    onLeave: () -> Unit,
) {
    state
        .map { it is SteakTimerState.Alerting }
        .distinctUntilChanged()
        .dropWhile { !it }
        .collect { alerting -> if (alerting) onEnter() else onLeave() }
}
