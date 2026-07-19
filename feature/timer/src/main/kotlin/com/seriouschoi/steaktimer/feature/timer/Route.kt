package com.seriouschoi.steaktimer.feature.timer

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
