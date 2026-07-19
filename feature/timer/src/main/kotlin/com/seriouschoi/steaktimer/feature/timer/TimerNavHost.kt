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
 * 앱 목적지를 타입으로 표현한 라우트 계층. 각 경로가 자기 [pattern](등록·pop용)과
 * 필요한 arg·경로 생성을 스스로 안다 — 문자열이 흩어지지 않고 한 타입 안에 갇힌다.
 *
 * type-safe nav(@Serializable 라우트)가 이상적이지만, 현재 스택(navigation 2.6.0 +
 * Wear `SwipeDismissableNavHost`)이 type-safe 오버로드를 아직 안 줘서, **문자열 라우트를
 * sealed 계층으로 감싸** 수동으로 타입 안전성을 흉내 낸다.
 */
sealed interface Route {
    /** `composable()` 등록과 `popBackStack()`에 쓰는 라우트 패턴. */
    val pattern: String

    data object Timer : Route {
        override val pattern = "timer"
    }

    data object Setup : Route {
        /** nav-arg 이름. `SetupViewModel`이 같은 이름으로 읽는다. */
        const val ARG_PRESET = "preset"

        /** 프리셋 없이 진입했음을 뜻하는 sentinel. 이 값이면 기본값으로 초기화된다. */
        const val PRESET_NONE = -1

        // 선택적 preset 쿼리 인자를 갖는 단일 패턴. 등록·pop은 이걸, 시작은 path(preset)을 쓴다.
        override val pattern = "setup?$ARG_PRESET={$ARG_PRESET}"

        /** 프리셋을 채운 setup 경로(시작 목적지). preset이 없으면(≤0) 패턴 그대로 둬 기본값이 채워진다. */
        fun path(preset: Int): String =
            if (preset > 0) "setup?$ARG_PRESET=$preset" else pattern
    }
}

/**
 * 앱 화면 그래프. 설정 ↔ 타이머 두 목적지.
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
