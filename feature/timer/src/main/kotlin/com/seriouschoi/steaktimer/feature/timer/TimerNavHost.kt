package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.feature.timer.countdown.TimerScreen
import com.seriouschoi.steaktimer.feature.timer.setup.SetupScreen

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/**
 * 앱 화면 그래프. 설정 ↔ 타이머 두 목적지. (목적지 라우트 타입은 [Route])
 *
 * - 설정에서 시작 → 타이머로 이동(설정은 백스택에 남겨 정지 시 되돌아옴).
 * - 타이머에서 정지(세션 Idle) → 설정으로 pop.
 *
 * 설정 초기값(타일 프리셋 포함)은 라우트가 아니라 [TimerConfigHolder]가 갖는다 —
 * `MainActivity`가 런치 입력을 그 홀더에 seed하고 `SetupViewModel`이 홀더를 읽는다(#36).
 * 그래서 이 그래프는 순수 화면 전환만 담당하고 진입 파라미터를 실어나르지 않는다.
 */
@Composable
fun TimerApp() {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Route.Setup.pattern,
    ) {
        composable(Route.Setup.pattern) {
            SetupScreen(onStarted = { navController.navigate(Route.Timer.pattern) })
        }
        composable(Route.Timer.pattern) {
            TimerScreen(onExit = { navController.popBackStack(Route.Setup.pattern, inclusive = false) })
        }
    }
}
