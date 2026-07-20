package com.seriouschoi.steaktimer.feature.timer.countdown

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
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
    Scaffold(timeText = { TimeText(modifier = Modifier.padding(top = 10.dp)) }) {
        Box(modifier = Modifier.fillMaxSize()) {
            TimerBody(ui = ui, onIntent = onIntent)

            // 알림은 종류별로 자기 뷰와 상호작용을 소유한다.
            when (ui.alert) {
                TimerAlert.Flip -> FlipAlert(onSkip = { onIntent(TimerUiIntent.Skip) })
                TimerAlert.ConfirmStop -> StopConfirmOverlay(
                    onStop = { onIntent(TimerUiIntent.ConfirmStop) },
                    onCancel = { onIntent(TimerUiIntent.CancelStop) },
                )
                null -> Unit
            }
        }
    }
}

/**
 * 타이머 본체: 남은 시간(시계) + 원형 progress + 실행 중 하단 조작 버튼.
 * 알림(Flip/ConfirmStop)이 떠 있으면 조작 버튼은 숨고, 상호작용은 각 알림 뷰가 맡는다.
 */
@Composable
private fun TimerBody(
    ui: TimerUiState,
    onIntent: (TimerUiIntent) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = ui.progress,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
        )
        Text(
            text = ui.timeText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.display1,
        )

        // 실행 중(알림 없음)에만 하단 [정지][건너뛰기]. 정지는 필수 동작이라 숨기지 않고 노출한다(#17).
        if (ui.alert == null) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { onIntent(TimerUiIntent.Stop) },
                    colors = ButtonDefaults.secondaryButtonColors(
                        backgroundColor = Color.DarkGray,
                    ),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "정지",
                        tint = Color.Red,
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize),
                    )
                }
                Button(
                    onClick = { onIntent(TimerUiIntent.Skip) },
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "건너뛰기",
                        modifier = Modifier.size(ButtonDefaults.SmallIconSize),
                    )
                }
            }
        }
    }
}

/** "뒤집기" 알림. 탭 어디든 = 다음 인터벌(Skip). 큰 타겟으로 빠르게 해제. */
@Composable
private fun FlipAlert(onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .pointerInput(Unit) { detectTapGestures { onSkip() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "뒤집기",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.display2,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "탭해서 다음",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.caption2,
            )
        }
    }
}

/** 정지 확인 알림. 종료/취소 버튼으로만 닫힌다(바깥 탭 흡수). */
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
private fun TimerRunningPreview() {
    TimerContent(
        ui = TimerUiState(isIdle = false, timeText = "00:08", progress = 0.8f, alert = null),
        onIntent = {},
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun FlipAlertPreview() {
    TimerContent(
        ui = TimerUiState(
            isIdle = false,
            timeText = "00:00",
            progress = 0f,
            alert = TimerAlert.Flip
        ),
        onIntent = {},
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun StopConfirmPreview() {
    TimerContent(
        ui = TimerUiState(
            isIdle = false,
            timeText = "00:05",
            progress = 0.5f,
            alert = TimerAlert.ConfirmStop
        ),
        onIntent = {},
    )
}
