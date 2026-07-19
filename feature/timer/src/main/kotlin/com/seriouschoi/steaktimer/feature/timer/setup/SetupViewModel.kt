package com.seriouschoi.steaktimer.feature.timer.setup

import com.seriouschoi.steaktimer.feature.timer.TimeFormat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.feature.timer.TimerConfigHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 설정 화면의 얇은 어댑터.
 *
 * 간격 상태는 더 이상 여기서 소유하지 않는다 — 앱스코프 [TimerConfigHolder](intended 설정의 단일
 * 진실원)를 읽어 표시하고, 사용자 입력은 홀더로 위임한다. 그 덕에 화면을 벗어났다 돌아와도
 * (타이머 종료 후 복귀 등) 직전 설정이 유지된다.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val config: TimerConfigHolder,
    private val session: SteakTimerSession,
) : ViewModel() {

    val uiState: StateFlow<SetupUiState> = config.seconds
        .map { it.toSetupUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = config.seconds.value.toSetupUiState(),
        )

    fun dispatch(intent: SetupUiIntent) = when (intent) {
        SetupUiIntent.Decrease -> config.decrease()
        SetupUiIntent.Increase -> config.increase()
        // 시작은 현재 설정 초로 세션에 직접 발행. 화면 전환(→ 타이머)은 컴포저블이 담당한다.
        SetupUiIntent.Start -> session.start(config.seconds.value * 1000L)
    }

    private fun Int.toSetupUiState(): SetupUiState =
        SetupUiState(seconds = this, timeText = TimeFormat.mmSs(this * 1000L))
}
