package com.seriouschoi.steaktimer.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
import com.seriouschoi.steaktimer.domain.TimerEngine
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
    engine: TimerEngine,
    private val haptic: Haptic,
) : ViewModel() {

    private val session = SteakTimerSession(engine, viewModelScope)

    val uiState: StateFlow<TimerUiState> = session.state
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            // 마지막 구독자(화면)가 떠난 뒤 5초 유예를 두고 상위를 멈춘다.
            // 화면 회전(config change)이나 짧은 백그라운드처럼 순간적으로 구독이 끊길 때
            // 상위 flow를 재시작하지 않게 하는 관용값. 오래 떠나면 멈춰 자원을 아낀다.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TimerUiState.INITIAL,
        )

    /** 화면에서 온 UI 인텐트를 세션 입력으로 발행한다. 단일 진입점. */
    fun dispatch(intent: TimerUiIntent) = when (intent) {
        TimerUiIntent.Tap -> session.tap()
        TimerUiIntent.LongPress -> session.longPress()
        TimerUiIntent.ConfirmStop -> session.confirmStop()
        TimerUiIntent.CancelStop -> session.cancelStop()
    }

    init {
        // Phase 3 데모용: 설정 화면(Phase 6)이 생기기 전까지 기본 간격으로 자동 시작.
        session.start(DEFAULT_INTERVAL_MS)

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

    companion object {
        // Phase 3 데모용 기본 간격. Phase 6 설정 화면이 생기면 대체된다.
        // 테스트가 'Alerting까지 흘릴 양'을 이 값으로 참조할 수 있게 internal.
        internal const val DEFAULT_INTERVAL_MS = 10_000L
    }
}
