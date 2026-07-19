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
import com.seriouschoi.steaktimer.feature.timer.TimerConfigHolder
import com.seriouschoi.steaktimer.feature.timer.TimerLaunch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 앱스코프 설정 홀더 — 런치 입력(타일 프리셋)을 여기 seed한다.
    @Inject lateinit var configHolder: TimerConfigHolder

    // 결과와 무관하게 앱은 돌아간다(FGS는 실행). 다만 권한이 있어야 진행 알림/Ongoing Activity가 보인다.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        // 최초 진입에만 런치 입력을 seed한다. 회전 등 재생성(savedInstanceState != null)엔 다시 seed하지
        // 않아, 사용자가 조절 중이던 값이나 직전 설정을 덮어쓰지 않는다.
        if (savedInstanceState == null) {
            val presetSeconds = intent.getIntExtra(EXTRA_PRESET_SECONDS, TimerLaunch.PRESET_NONE)
            configHolder.seed(TimerLaunch(presetSeconds = presetSeconds))
        }
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

    companion object {
        // 타일이 프리셋 초를 실어 보내는 intent extra 키(타일 모듈의 같은 이름 상수와 문자열 일치).
        const val EXTRA_PRESET_SECONDS = "preset_seconds"
    }
}
