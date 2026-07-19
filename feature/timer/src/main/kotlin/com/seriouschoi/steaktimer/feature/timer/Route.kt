package com.seriouschoi.steaktimer.feature.timer

/**
 * 앱 목적지를 타입으로 표현한 라우트 계층. 각 경로가 자기 [pattern](등록·pop용)을 안다.
 *
 * 프리셋 등 진입 입력은 더 이상 라우트에 싣지 않는다 — [TimerConfigHolder]가 설정을 소유하므로
 * 목적지는 순수 화면 식별자만 된다(#36).
 *
 * type-safe nav(@Serializable 라우트)가 이상적이지만, 현재 스택(navigation 2.6.0 +
 * Wear `SwipeDismissableNavHost`)이 type-safe 오버로드를 아직 안 줘서 문자열 라우트를
 * sealed 계층으로 감싼다.
 */
sealed interface Route {
    /** `composable()` 등록과 `popBackStack()`에 쓰는 라우트 패턴. */
    val pattern: String

    data object Setup : Route {
        override val pattern = "setup"
    }

    data object Timer : Route {
        override val pattern = "timer"
    }
}
