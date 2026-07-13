package com.seriouschoi.steaktimer.core.platform.di

import com.seriouschoi.steaktimer.core.platform.CoroutineTimerEngine
import com.seriouschoi.steaktimer.domain.TimerEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 어댑터 구현을 도메인 Port에 바인딩. @InstallIn으로 앱 그래프에 자동 집계되어,
 * :app은 이 바인딩의 존재를 몰라도 결선만으로 TimerEngine을 주입받는다.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {

    @Binds
    @Singleton
    abstract fun bindTimerEngine(impl: CoroutineTimerEngine): TimerEngine
}
