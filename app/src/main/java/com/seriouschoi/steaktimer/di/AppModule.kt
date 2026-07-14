package com.seriouschoi.steaktimer.di

import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.TimerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** 앱 수명 CoroutineScope를 다른 주입 대상과 구분하기 위한 한정자. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * 컴포지션 루트의 앱 수준 결선. 세션 소유를 ViewModel에서 앱 그래프로 끌어올린다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 앱 수명 CoroutineScope. ViewModel 수명과 분리해, 세션이 화면(설정↔타이머 이동,
     * 회전, 잠깐의 백그라운드)보다 오래 살아남게 한다. SupervisorJob으로 한 자식의
     * 실패가 스코프 전체를 무너뜨리지 않게 하고, 타이머 진행은 UI와 무관하므로
     * Default 디스패처에서 돌린다.
     *
     * Phase 7에서 Foreground Service가 세션 수명을 넘겨받을 자리의 예비 스코프.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 타이머 세션을 앱 스코프 싱글턴으로 제공한다. 더는 ViewModel이 생성/소유하지 않으며,
     * 서비스(Phase 7)도 같은 싱글턴을 주입받을 수 있어 'service-ownable' 조건을 만족한다.
     */
    @Provides
    @Singleton
    fun provideSteakTimerSession(
        engine: TimerEngine,
        @ApplicationScope scope: CoroutineScope,
    ): SteakTimerSession = SteakTimerSession(engine, scope)
}
