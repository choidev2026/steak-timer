package com.seriouschoi.steaktimer.feature.timer.setup

/** 설정 화면에서 사용자가 낼 수 있는 입력. */
sealed interface SetupUiIntent {
    /** 간격 한 스텝 감소. */
    data object Decrease : SetupUiIntent

    /** 간격 한 스텝 증가. */
    data object Increase : SetupUiIntent

    /** 고른 간격으로 타이머 시작. */
    data object Start : SetupUiIntent
}
