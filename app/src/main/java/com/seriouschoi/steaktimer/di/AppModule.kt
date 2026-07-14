package com.seriouschoi.steaktimer.di

import com.seriouschoi.steaktimer.core.timersession.di.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * 컴포지션 루트의 앱 수준 결선. 앱 전역에서 공유하는 primitive만 둔다.
 * 세션 등 도메인 런타임 결선은 각 전용 모듈(:core:timersession)로 빠진다.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 앱 수명 CoroutineScope. ViewModel/화면 수명과 분리해, 세션 같은 장수명 작업이
     * 화면(설정↔타이머 이동, 회전, 잠깐의 백그라운드)보다 오래 살게 한다.
     * SupervisorJob으로 한 자식의 실패가 스코프 전체를 무너뜨리지 않게 하고,
     * UI와 무관한 작업이므로 Default 디스패처에서 돈다.
     *
     * Phase 7에서 Foreground Service가 이 스코프 수명을 직접 관리하도록 옮겨갈 수 있다.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
