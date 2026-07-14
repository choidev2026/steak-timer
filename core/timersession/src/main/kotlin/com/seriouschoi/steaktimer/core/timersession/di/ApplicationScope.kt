package com.seriouschoi.steaktimer.core.timersession.di

import javax.inject.Qualifier

/**
 * 앱 수명 CoroutineScope를 다른 주입 대상과 구분하는 한정자.
 *
 * 인스턴스는 :app(컴포지션 루트)이 제공하고, 세션 provider가 이를 주입받는다.
 * core는 :app을 의존할 수 없으므로(방향이 app→core) 애노테이션은 여기(core)에 둔다.
 * 지금은 이 스코프의 유일한 소비자가 타이머 세션이라 이 모듈에 두며,
 * 소비자가 늘면 :core:common 같은 공용 모듈로 승격한다.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
