package com.seriouschoi.steaktimer.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.seriouschoi.steaktimer.presentation.theme.SteakTimerTheme
import com.seriouschoi.steaktimer.feature.timer.TimerApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SteakTimerTheme {
                TimerApp()
            }
        }
    }
}
