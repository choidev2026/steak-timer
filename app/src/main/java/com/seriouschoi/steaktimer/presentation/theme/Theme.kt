package com.seriouschoi.steaktimer.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * Wear 앱 테마. Phase 0에서는 기본 Wear MaterialTheme만 감싼다.
 * 색상/타이포는 이후 필요해질 때 확장.
 */
@Composable
fun SteakTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
