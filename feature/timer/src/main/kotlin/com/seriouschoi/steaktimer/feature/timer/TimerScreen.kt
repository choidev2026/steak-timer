package com.seriouschoi.steaktimer.feature.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TimerContent(uiState)
}

@Composable
private fun TimerContent(ui: TimerUiState) {
    Scaffold(timeText = { TimeText() }) {
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
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun TimerContentRunningPreview() {
    TimerContent(TimerUiState(timeText = "00:08", progress = 0.8f, isVibrating = false, hint = "", showStopConfirm = false))
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun TimerContentAlertingPreview() {
    TimerContent(TimerUiState(timeText = "뒤집기", progress = 0f, isVibrating = true, hint = "탭해서 다음", showStopConfirm = false))
}
