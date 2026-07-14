package com.seriouschoi.steaktimer.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.StepperDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices

/** 뒤집기 간격을 고르고 시작하는 설정 화면. 10초 스텝(10초~10분). */
@Composable
internal fun SetupContent(
    onStart: (Long) -> Unit,
) {
    var seconds by rememberSaveable { mutableIntStateOf(DEFAULT_SETUP_SECONDS) }

    Stepper(
        value = seconds,
        onValueChange = { seconds = it },
        valueProgression = IntProgression.fromClosedRange(MIN_SECONDS, MAX_SECONDS, STEP_SECONDS),
        increaseIcon = { Icon(StepperDefaults.Increase, contentDescription = "증가") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, contentDescription = "감소") },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = formatMmSs(seconds * 1000L),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.display2,
            )
            Spacer(Modifier.height(6.dp))
            CompactChip(
                onClick = { onStart(seconds * 1000L) },
                label = { Text("시작") },
                colors = ChipDefaults.primaryChipColors(),
            )
        }
    }
}

private const val DEFAULT_SETUP_SECONDS = 60   // 기본 1분
private const val MIN_SECONDS = 10
private const val MAX_SECONDS = 600            // 10분
private const val STEP_SECONDS = 10

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SetupPreview() {
    Scaffold(timeText = { TimeText() }) {
        SetupContent(onStart = {})
    }
}
