package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.feature.timer.countdown.TimerScreen
import com.seriouschoi.steaktimer.feature.timer.setup.SetupScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/**
 * 앱 화면 그래프. 설정 ↔ 타이머 두 목적지. (목적지 라우트 타입은 [Route])
 *
 * - 설정에서 시작 → 타이머로 이동(설정은 백스택에 남겨 정지 시 되돌아옴).
 * - 타이머에서 정지(세션 Idle) → 설정으로 pop.
 *
 * [launch] : 앱을 띄운 입력(타일 프리셋 등). [TimerAppViewModel]이 이 값을 [TimerConfigHolder]에
 * **한 번** seeding 하고, 이후 설정 초기값은 홀더가 소유한다(#36). 그래프 자체는 진입 파라미터를
 * 라우트로 실어나르지 않고 순수 화면 전환만 담당한다.
 */
@Composable
fun TimerApp(
    launch: TimerLaunch = TimerLaunch(),
    viewModel: TimerAppViewModel = hiltViewModel(),
) {
    // 런치 입력을 홀더에 1회 주입(VM 수명으로 재-seed 방지 — 회전 등에도 한 번만).
    LaunchedEffect(Unit) { viewModel.seed(launch) }

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
