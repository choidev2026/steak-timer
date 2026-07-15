package com.seriouschoi.steaktimer.feature.timer.setup

import com.seriouschoi.steaktimer.feature.timer.TimeFormat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 설정 화면 상태(간격 선택)를 소유하고, 시작 시 세션에 발행한다.
 *
 * 간격 선택 상태를 컴포저블 로컬이 아니라 여기서 갖는 이유:
 * (1) 화면 수명(회전 등) 넘어 유지, (2) Step 3(지속성)에서 저장된 기본값을 여기로
 * 로드해 초기 seconds를 채우기 위함.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val session: SteakTimerSession,
) : ViewModel() {

    private val _seconds = MutableStateFlow(DEFAULT_SETUP_SECONDS)

    val uiState: StateFlow<SetupUiState> = _seconds
        .map { it.toSetupUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DEFAULT_SETUP_SECONDS.toSetupUiState(),
        )

    fun dispatch(intent: SetupUiIntent) = when (intent) {
        SetupUiIntent.Decrease -> _seconds.update { (it - STEP_SECONDS).coerceAtLeast(MIN_SECONDS) }
        SetupUiIntent.Increase -> _seconds.update { (it + STEP_SECONDS).coerceAtMost(MAX_SECONDS) }
        // 시작은 세션에 직접 발행. 화면 전환(→ 타이머)은 컴포저블이 담당한다.
        SetupUiIntent.Start -> session.start(_seconds.value * 1000L)
    }

    private fun Int.toSetupUiState(): SetupUiState =
        SetupUiState(seconds = this, timeText = TimeFormat.mmSs(this * 1000L))

    companion object {
        const val DEFAULT_SETUP_SECONDS = 60   // 기본 1분
        const val MIN_SECONDS = 10
        const val MAX_SECONDS = 600            // 10분
        const val STEP_SECONDS = 10
    }
}
