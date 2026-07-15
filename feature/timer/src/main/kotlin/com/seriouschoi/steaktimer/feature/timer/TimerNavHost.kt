package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.feature.timer.countdown.TimerScreen
import com.seriouschoi.steaktimer.feature.timer.setup.SetupScreen

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/**
 * 앱 화면 그래프. 설정 ↔ 타이머 두 목적지.
 *
 * - 설정에서 시작 → 타이머로 이동(설정은 백스택에 남겨 정지 시 되돌아옴).
 * - 타이머에서 정지(세션 Idle) → 설정으로 pop.
 *
 * 두 화면은 각자 ViewModel을 갖고, 공유 세션(앱 스코프 싱글턴)을 통해 이어진다.
 */
@Composable
fun TimerApp() {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.SETUP,
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onStarted = { navController.navigate(Routes.TIMER) },
            )
        }
        composable(Routes.TIMER) {
            TimerScreen(
                onExit = { navController.popBackStack(Routes.SETUP, inclusive = false) },
            )
        }
    }
}

private object Routes {
    const val SETUP = "setup"
    const val TIMER = "timer"
}
