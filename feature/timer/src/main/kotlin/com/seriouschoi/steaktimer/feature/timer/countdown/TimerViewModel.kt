package com.seriouschoi.steaktimer.feature.timer.countdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    // 세션은 ViewModel이 소유하지 않는다. 앱 스코프 싱글턴을 주입받아 화면 수명보다 오래 살게 한다(#16).
    // 진동 구동은 더 이상 ViewModel이 하지 않는다 — 화면 꺼짐/AOD에서도 울리도록
    // Foreground Service가 넘겨받았다(Phase 7). ViewModel은 표시 상태만 다룬다.
    private val session: SteakTimerSession,
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

    /** 화면 UI 인텐트(메커니즘)를 도메인 의미로 번역해 세션에 발행한다. 단일 진입점.
     *  Start는 여기 없다 — 시작은 설정 화면(SetupViewModel)이 세션에 직접 발행한다. */
    fun dispatch(intent: TimerUiIntent) = when (intent) {
        TimerUiIntent.Skip -> session.advance()
        TimerUiIntent.Stop -> session.requestStop()
        TimerUiIntent.ConfirmStop -> session.confirmStop()
        TimerUiIntent.CancelStop -> session.cancelStop()
    }
}
