package com.seriouschoi.steaktimer.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * 타이머 화면. 세션이 Idle로 돌아오면(정지) [onExit]로 설정 화면 복귀를 알린다.
 */
@Composable
fun TimerScreen(
    onExit: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 정지(→ Idle)되면 설정 화면으로 되돌린다. 진입 시엔 세션이 Running이라 isIdle=false이므로
    // 곧장 튕기지 않는다(초기값을 현재 세션 상태로 잡아둔 덕분).
    LaunchedEffect(uiState.isIdle) {
        if (uiState.isIdle) onExit()
    }

    TimerContent(ui = uiState, onIntent = viewModel::dispatch)
}

@Composable
private fun TimerContent(
    ui: TimerUiState,
    onIntent: (TimerUiIntent) -> Unit,
) {
    Scaffold(timeText = { TimeText() }) {
        Box(modifier = Modifier.fillMaxSize()) {
            TimerBody(ui = ui, onIntent = onIntent)

            if (ui.showStopConfirm) {
                StopConfirmOverlay(
                    onStop = { onIntent(TimerUiIntent.ConfirmStop) },
                    onCancel = { onIntent(TimerUiIntent.CancelStop) },
                )
            }
        }
    }
}

/** 타이머 본체: 남은 시간 + 원형 progress + 탭/롱프레스 제스처. */
@Composable
private fun TimerBody(
    ui: TimerUiState,
    onIntent: (TimerUiIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 종료 확인 오버레이가 떠 있으면 본체는 제스처를 받지 않는다.
            .pointerInput(ui.showStopConfirm) {
                if (!ui.showStopConfirm) {
                    detectTapGestures(
                        onTap = { onIntent(TimerUiIntent.Tap) },
                        onLongPress = { onIntent(TimerUiIntent.LongPress) },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = ui.progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = ui.timeText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.display1,
            )
            if (ui.hint.isNotEmpty()) {
                Text(
                    text = ui.hint,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.caption2,
                )
            }
        }
    }
}

@Composable
private fun StopConfirmOverlay(
    onStop: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            // 배경 탭을 흡수해 밑으로 새지 않게 하고, 실수 dismiss도 막는다(버튼으로만 동작).
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "종료할까요?",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.title3,
            )
            Spacer(Modifier.height(12.dp))
            Chip(
                onClick = onStop,
                label = { Text("종료") },
                colors = ChipDefaults.primaryChipColors(),
            )
            Spacer(Modifier.height(6.dp))
            Chip(
                onClick = onCancel,
                label = { Text("취소") },
                colors = ChipDefaults.secondaryChipColors(),
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun TimerContentRunningPreview() {
    TimerContent(
        ui = TimerUiState(isIdle = false, timeText = "00:08", progress = 0.8f, isVibrating = false, hint = "", showStopConfirm = false),
        onIntent = {},
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun StopConfirmPreview() {
    TimerContent(
        ui = TimerUiState(isIdle = false, timeText = "00:05", progress = 0.5f, isVibrating = false, hint = "", showStopConfirm = true),
        onIntent = {},
    )
}
