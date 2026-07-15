package com.seriouschoi.steaktimer.core.timersession

import android.content.Context
import android.content.Intent
import com.seriouschoi.steaktimer.core.timersession.di.ApplicationScope
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 세션 상태를 단일 진실원으로 관측해 두 가지를 관리한다:
 *  1. **포그라운드 서비스 수명** — 활성(Idle 아님)이면 서비스 up, Idle이면 down.
 *  2. **완주 알람 예약** — Running 인터벌이 시작될 때마다 그 완주 시각에 알람을 예약하고,
 *     Running을 벗어나면 취소한다. (딥슬립을 뚫는 타이밍 보장은 이 알람이 맡는다.)
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
        // 1. 포그라운드 서비스 수명
        scope.launch {
            session.state
                .map { it !is SteakTimerState.Idle }
                .distinctUntilChanged()
                .collect { active -> if (active) startService() else stopService() }
        }
        // 2. 완주 알람 예약: 새 Running에 진입할 때만 예약, Running을 벗어나면 취소.
        scope.launch {
            var prev: SteakTimerState? = null
            session.state.collect { cur ->
                val enteringRunning = cur is SteakTimerState.Running &&
                    (prev !is SteakTimerState.Running || (prev as SteakTimerState.Running).cycle != cur.cycle)
                val leavingRunning = cur !is SteakTimerState.Running && prev is SteakTimerState.Running
                when {
                    enteringRunning -> alarmScheduler.scheduleAfter((cur as SteakTimerState.Running).remainingMs)
                    leavingRunning -> alarmScheduler.cancel()
                }
                prev = cur
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
