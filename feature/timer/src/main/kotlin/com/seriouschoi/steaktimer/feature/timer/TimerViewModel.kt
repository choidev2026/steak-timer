package com.seriouschoi.steaktimer.feature.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.TimerEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    engine: TimerEngine,
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

    init {
        // Phase 3 데모용: 설정 화면(Phase 6)이 생기기 전까지 기본 간격으로 자동 시작.
        session.start(DEFAULT_INTERVAL_MS)
    }

    companion object {
        private const val DEFAULT_INTERVAL_MS = 10_000L
    }
}
