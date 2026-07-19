package com.seriouschoi.steaktimer.feature.timer

import com.seriouschoi.steaktimer.feature.timer.countdown.TimerScreen
import com.seriouschoi.steaktimer.feature.timer.setup.SetupScreen

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

/** 프리셋 없이 진입했음을 뜻하는 sentinel. 이 값이면 setup은 기본값으로 초기화된다. */
const val PRESET_NONE = -1

/** setup 목적지의 nav-arg 이름. `SetupViewModel`이 같은 이름으로 읽는다(양쪽 일치 필요). */
const val ARG_PRESET = "preset"

/**
 * 목적지 라우트를 한 곳에서 관리한다 — 문자열 조립/일치를 객체로 캡슐화해
 * 등록·시작·pop이 흩어진 문자열이 아니라 **같은 소스**를 쓰게 한다(패턴 불일치 위험 제거).
 *
 * type-safe nav(@Serializable 라우트)가 이상적이지만, 현재 스택(navigation 2.6.0 +
 * Wear `SwipeDismissableNavHost`)이 type-safe 오버로드를 아직 안 줘서 문자열 라우트를
 * 쓰되 이 객체로 감싼다.
 */
private object Routes {
    const val TIMER = "timer"

    // setup은 선택적 preset 쿼리 인자를 갖는 단일 패턴. 등록·pop은 이 패턴을, 시작은 setup(preset)을 쓴다.
    const val SETUP_PATTERN = "setup?$ARG_PRESET={$ARG_PRESET}"

    /** 프리셋을 채운 setup 경로. preset이 없으면(≤0) 패턴 그대로 둬 기본값이 채워지게 한다. */
    fun setup(preset: Int): String =
        if (preset > 0) "setup?$ARG_PRESET=$preset" else SETUP_PATTERN
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
fun TimerApp(presetSeconds: Int = PRESET_NONE) {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(
        navController = navController,
        // 프리셋이 있으면 그 값을 채운 setup 목적지로 시작, 없으면 패턴 그대로(기본값이 채워짐).
        startDestination = Routes.setup(presetSeconds),
    ) {
        composable(
            route = Routes.SETUP_PATTERN,
            arguments = listOf(
                navArgument(ARG_PRESET) {
                    type = NavType.IntType
                    defaultValue = PRESET_NONE
                },
            ),
        ) {
            SetupScreen(onStarted = { navController.navigate(Routes.TIMER) })
        }
        composable(Routes.TIMER) {
            TimerScreen(onExit = { navController.popBackStack(Routes.SETUP_PATTERN, inclusive = false) })
        }
    }
}
