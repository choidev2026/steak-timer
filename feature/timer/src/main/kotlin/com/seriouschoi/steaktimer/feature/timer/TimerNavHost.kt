package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.feature.timer.countdown.TimerScreen
import com.seriouschoi.steaktimer.feature.timer.setup.SetupScreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/**
 * 앱 화면 그래프. 설정 ↔ 타이머 두 목적지. (목적지 라우트 타입은 [Route])
 *
 * - 설정에서 시작 → 타이머로 이동(설정은 백스택에 남겨 정지 시 되돌아옴).
 * - 타이머에서 정지(세션 Idle) → 설정으로 pop.
 *
 * 두 화면은 각자 ViewModel을 갖고, 공유 세션(앱 스코프 싱글턴)을 통해 이어진다.
 *
 * [presetSeconds] : 타일에서 프리셋을 들고 진입하면 그 초를 setup 시작 목적지에 nav-arg로 실어,
 * `SetupViewModel`이 `SavedStateHandle`로 읽어 초기값을 잡는다(액티비티 인텐트를 컴포저블에 직접
 * 꽂지 않고 기존 네비 흐름을 그대로 태운다).
 */
@Composable
fun TimerApp(presetSeconds: Int = Route.Setup.PRESET_NONE) {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        // 프리셋이 있으면 그 값을 채운 setup 목적지로 시작, 없으면 패턴 그대로(기본값이 채워짐).
        startDestination = Route.Setup.path(presetSeconds),
    ) {
        composable(
            route = Route.Setup.pattern,
            arguments = listOf(
                navArgument(Route.Setup.ARG_PRESET) {
                    type = NavType.IntType
                    defaultValue = Route.Setup.PRESET_NONE
                },
            ),
        ) {
            SetupScreen(onStarted = { navController.navigate(Route.Timer.pattern) })
        }
        composable(Route.Timer.pattern) {
            TimerScreen(onExit = { navController.popBackStack(Route.Setup.pattern, inclusive = false) })
        }
    }
}
