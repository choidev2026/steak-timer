package com.seriouschoi.steaktimer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.seriouschoi.steaktimer.R
import com.seriouschoi.steaktimer.presentation.theme.SteakTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SteakTimerApp()
        }
    }
}

@Composable
fun SteakTimerApp() {
    SteakTimerTheme {
        Scaffold(
            timeText = { TimeText() },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Phase 0 placeholder. 이후 Phase에서 타이머 화면으로 대체.
                Text(
                    text = stringResource(R.string.app_name),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SteakTimerAppPreview() {
    SteakTimerApp()
}
