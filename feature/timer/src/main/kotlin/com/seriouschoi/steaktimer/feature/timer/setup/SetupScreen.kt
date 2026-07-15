package com.seriouschoi.steaktimer.feature.timer.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * 설정 화면. 간격을 고르고 시작한다. 시작하면 [onStarted]로 타이머 화면 전환을 알린다.
 */
@Composable
fun SetupScreen(
    onStarted: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(timeText = { TimeText() }) {
        SetupContent(
            timeText = ui.timeText,
            onDecrease = { viewModel.dispatch(SetupUiIntent.Decrease) },
            onIncrease = { viewModel.dispatch(SetupUiIntent.Increase) },
            onStart = {
                viewModel.dispatch(SetupUiIntent.Start)
                onStarted()
            },
        )
    }
}

/** 표시 전용 설정 UI. 10초 스텝(10초~10분). 상태는 [SetupViewModel]이 소유한다. */
@Composable
internal fun SetupContent(
    timeText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Spacer(
            modifier = Modifier.weight(1f)
        )
        // 설정 줄: [－] 00:00 [＋]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onDecrease,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            ) {
                Icon(
                    InlineSliderDefaults.Decrease,
                    contentDescription = "감소",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize),
                )
            }
            Text(
                text = timeText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.display3,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onIncrease,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize),
            ) {
                Icon(
                    InlineSliderDefaults.Increase,
                    contentDescription = "증가",
                    modifier = Modifier.size(ButtonDefaults.SmallIconSize),
                )
            }
        }
        // 버튼 줄: 시작
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CompactChip(
                onClick = onStart,
                label = { Text("시작") },
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SetupPreview() {
    Scaffold(timeText = { TimeText() }) {
        SetupContent(timeText = "01:00", onDecrease = {}, onIncrease = {}, onStart = {})
    }
}
