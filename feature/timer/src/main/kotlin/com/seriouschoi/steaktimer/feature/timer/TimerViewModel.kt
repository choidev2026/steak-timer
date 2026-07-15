package com.seriouschoi.steaktimer.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    // 세션은 더 이상 ViewModel이 소유하지 않는다. 앱 스코프 싱글턴을 주입받아
    // 화면 수명보다 오래 살게 한다(#16). Phase 7에서 서비스가 같은 세션을 넘겨받는다.
    private val session: SteakTimerSession,
    private val haptic: Haptic,
) : ViewModel() {

    val uiState: StateFlow<TimerUiState> = session.state
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            // 마지막 구독자(화면)가 떠난 뒤 5초 유예를 두고 상위를 멈춘다.
            // 화면 회전(config change)이나 짧은 백그라운드처럼 순간적으로 구독이 끊길 때
            // 상위 flow를 재시작하지 않게 하는 관용값. 오래 떠나면 멈춰 자원을 아낀다.
            started = SharingStarted.WhileSubscribed(5_000),
            // 초기값은 하드코딩된 INITIAL이 아니라 '지금 세션 상태'로 잡는다.
            // 타이머 화면은 세션이 Running일 때 진입하므로, 첫 프레임부터 isIdle=false여야
            // "막 진입했는데 isIdle=true라 곧장 설정 화면으로 튕기는" 오작동을 막는다.
            initialValue = session.state.value.toUiState(),
        )

    /** 화면에서 온 UI 인텐트를 세션 입력으로 발행한다. 단일 진입점.
     *  Start는 여기 없다 — 시작은 설정 화면(SetupViewModel)이 세션에 직접 발행한다. */
    fun dispatch(intent: TimerUiIntent) = when (intent) {
        TimerUiIntent.Tap -> session.tap()
        TimerUiIntent.LongPress -> session.longPress()
        TimerUiIntent.ConfirmStop -> session.confirmStop()
        TimerUiIntent.CancelStop -> session.cancelStop()
    }

    init {
        // 진동 구동(표현 계층 side-effect). Alerting 진입 → 시작, 이탈 → 정지.
        // dropWhile로 첫 알림 전의 비-Alerting 상태는 흘려보내 불필요한 stop 호출을 막는다.
        // 참고: 화면 꺼짐/AOD에서의 신뢰성은 Phase 7(Foreground Service)에서 서비스가 넘겨받는다.
        viewModelScope.launch {
            session.state
                .map { it is SteakTimerState.Alerting }
                .distinctUntilChanged()
                .dropWhile { !it }
                .collect { alerting ->
                    if (alerting) haptic.startAlert() else haptic.stop()
                }
        }
    }
}
