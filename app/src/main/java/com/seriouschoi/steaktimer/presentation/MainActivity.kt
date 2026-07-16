package com.seriouschoi.steaktimer.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.seriouschoi.steaktimer.presentation.theme.SteakTimerTheme
import com.seriouschoi.steaktimer.feature.timer.TimerApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 결과와 무관하게 앱은 돌아간다(FGS는 실행). 다만 권한이 있어야 진행 알림/Ongoing Activity가 보인다.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            SteakTimerTheme {
                TimerApp()
            }
        }
    }

    /** API 33+에서 POST_NOTIFICATIONS는 런타임 권한 — 없으면 진행 알림/Ongoing Activity가 안 보이므로 요청. */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
