package com.seriouschoi.steaktimer.core.timersession

import android.content.Context
import android.content.Intent
import com.seriouschoi.steaktimer.core.timersession.di.ApplicationScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 세션 상태를 단일 진실원으로 관측해, **앱스코프 효과**를 실행하는 러너:
 *  - 포그라운드 서비스 수명([ServiceEffect.StartService]/[ServiceEffect.StopService])
 *  - 완주 알람 예약/취소([ServiceEffect.ScheduleAlarm]/[ServiceEffect.CancelAlarm], 딥슬립 관통)
 *
 * "어떤 전이에 어떤 효과"는 순수 함수 [effectsFor]가 정하고(#35), 여기선 그중 **자기 것만** 실행한다.
 * Alerting 관련 효과는 서비스가 살아있어야 하므로 [TimerForegroundService]가 실행한다.
 *
 * UI 이벤트가 아니라 세션 상태로 트리거하므로 화면/ViewModel과 독립적이고 robust하다.
 * [SteakTimerApp]이 [start]로 관측을 켠다.
 */
@Singleton
class TimerServiceController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SteakTimerSession,
    private val alarmScheduler: TimerAlarmScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            session.state.serviceEffects().collect { effect ->
                when (effect) {
                    ServiceEffect.StartService -> startService()
                    ServiceEffect.StopService -> stopService()
                    is ServiceEffect.ScheduleAlarm -> alarmScheduler.scheduleAfter(effect.afterMs)
                    ServiceEffect.CancelAlarm -> alarmScheduler.cancel()
                    // Alerting 효과는 서비스가 살아있어야 해서 서비스 러너가 실행.
                    ServiceEffect.StartAlerting, ServiceEffect.StopAlerting -> Unit
                }
            }
        }
    }

    private fun startService() {
        context.startForegroundService(Intent(context, TimerForegroundService::class.java))
    }

    private fun stopService() {
        context.stopService(Intent(context, TimerForegroundService::class.java))
    }
}
