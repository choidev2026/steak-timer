package com.seriouschoi.steaktimer.feature.timer.setup

import com.seriouschoi.steaktimer.feature.timer.TimeFormat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.feature.timer.Route
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
 * (1) 화면 수명(회전 등) 넘어 유지, (2) 타일에서 넘어온 프리셋(nav-arg)을 초기값으로 로드하기 위함.
 *
 * 초기 seconds는 [SavedStateHandle]의 `preset`(타일 딥링크가 실은 nav-arg)에서 온다.
 * 유효 범위를 벗어나거나 없으면 [DEFAULT_SETUP_SECONDS]로 폴백한다.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val session: SteakTimerSession,
) : ViewModel() {

    private val _seconds = MutableStateFlow(savedStateHandle.initialSeconds())

    val uiState: StateFlow<SetupUiState> = _seconds
        .map { it.toSetupUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            // 첫 프레임부터 프리셋이 보이도록 초기값도 현재 seconds로 맞춘다(기본값으로 깜빡이지 않게).
            initialValue = _seconds.value.toSetupUiState(),
        )

    fun dispatch(intent: SetupUiIntent) = when (intent) {
        SetupUiIntent.Decrease -> _seconds.update { (it - STEP_SECONDS).coerceAtLeast(MIN_SECONDS) }
        SetupUiIntent.Increase -> _seconds.update { (it + STEP_SECONDS).coerceAtMost(MAX_SECONDS) }
        // 시작은 세션에 직접 발행. 화면 전환(→ 타이머)은 컴포저블이 담당한다.
        SetupUiIntent.Start -> session.start(_seconds.value * 1000L)
    }

    private fun Int.toSetupUiState(): SetupUiState =
        SetupUiState(seconds = this, timeText = TimeFormat.mmSs(this * 1000L))

    private fun SavedStateHandle.initialSeconds(): Int {
        val preset = get<Int>(Route.Setup.ARG_PRESET) ?: Route.Setup.PRESET_NONE
        return if (preset in MIN_SECONDS..MAX_SECONDS) preset else DEFAULT_SETUP_SECONDS
    }

    companion object {
        const val DEFAULT_SETUP_SECONDS = 60   // 기본 1분
        const val MIN_SECONDS = 10
        const val MAX_SECONDS = 600            // 10분
        const val STEP_SECONDS = 10
    }
}
