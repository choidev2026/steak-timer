package com.seriouschoi.steaktimer.core.timersession.di

import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.TimerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

/**
 * 타이머 세션(도메인 상태기계 런타임)을 앱 그래프에 결선한다.
 *
 * 세션 클래스는 :core:domain(순수 kotlin), 앱 수명 스코프 제공은 :app에 있고,
 * 이 모듈은 둘을 묶어 앱 수명 싱글턴 세션으로 만든다. ViewModel은 더 이상 세션을
 * 생성하지 않고 이 싱글턴을 주입받는다.
 *
 * Phase 7의 Foreground Service도 이 모듈에 함께 들어와 같은 세션을 소유하게 될 자리.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimerSessionModule {

    @Provides
    @Singleton
    fun provideSteakTimerSession(
        engine: TimerEngine,
        @ApplicationScope scope: CoroutineScope,
    ): SteakTimerSession = SteakTimerSession(engine, scope)
}
