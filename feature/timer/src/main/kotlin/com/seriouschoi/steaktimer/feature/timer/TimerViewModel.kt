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
