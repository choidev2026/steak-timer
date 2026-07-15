package com.seriouschoi.steaktimer.core.timersession

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 타이머 활성 동안 프로세스를 살려두고(포그라운드 + 노티), 알림(Alerting) 동안 진동을
 * 헤드리스에서 구동하는 서비스.
 *
 * 타이밍 보장은 [TimerAlarmScheduler]의 `setAlarmClock`이 맡는다: 딥슬립으로 delay 루프가
 * 얼어붙어도 예약 시각에 시스템이 이 서비스를 [ACTION_DEADLINE]로 깨워 세션을 완주 처리한다.
 * WakeLock은 **Alerting 동안만** 잡아 진동을 유지한다(세션 내내 잡지 않아 배터리 우호적).
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var session: SteakTimerSession
    @Inject lateinit var haptic: Haptic

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        // Alerting 동안만 WakeLock + 진동. (알람이 딥슬립에서 깨운 직후에도 CPU를 유지해 진동 지속)
        scope.launch {
            observeAlerting(
                state = session.state,
                onEnter = { acquireWakeLock(); haptic.startAlert() },
                onLeave = { haptic.stop(); releaseWakeLock() },
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DEADLINE) {
            // 예약 알람 발화: 관측자가 뜰 때까지 CPU를 먼저 붙잡고 세션을 완주 처리.
            acquireWakeLock()
            session.reachDeadline()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        haptic.stop()
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "타이머 실행", NotificationManager.IMPORTANCE_LOW),
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("스테이크 타이머")
            .setContentText("타이머 실행 중")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_DEADLINE = "com.seriouschoi.steaktimer.action.DEADLINE"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "steak_timer_running"
        private const val WAKELOCK_TAG = "SteakTimer::AlertWakeLock"
        // 안전장치: 사용자가 응답 안 해도 진동이 영원히 안 돌게 하는 상한. 정상 이탈(Alerting→해제)에서 먼저 해제됨.
        private const val WAKELOCK_TIMEOUT_MS = 10L * 60L * 1000L // 10분
    }
}
