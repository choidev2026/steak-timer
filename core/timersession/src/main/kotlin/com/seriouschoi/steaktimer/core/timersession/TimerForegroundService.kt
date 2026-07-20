package com.seriouschoi.steaktimer.core.timersession

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
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
 * 타이머 활성 동안 프로세스를 살려두고(포그라운드 + Ongoing Activity), 알림(Alerting) 동안
 * 진동을 헤드리스에서 구동하는 서비스.
 *
 * 타이밍 보장은 [TimerAlarmScheduler]의 `setAlarmClock`이 맡는다(딥슬립을 뚫는 완주 알람).
 * WakeLock은 Alerting 동안만 잡아 진동을 유지한다. Ongoing Activity로 워치페이스에
 * 아이콘을 노출하고, 탭하면 앱으로 복귀한다(Step B).
 */
@AndroidEntryPoint
class TimerForegroundService : Service() {

    @Inject lateinit var session: SteakTimerSession
    @Inject lateinit var haptic: Haptic

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        ensureAlertChannel()
        // 정적 알림 하나면 충분: 대상 기기(GW6/WearOS6)에서 Ongoing Activity는 아이콘만 렌더하고
        // 동적 카운트다운은 안 보인다 → 사이클마다 갱신하지 않는다. 뒤집기 안내는 postFlipAlert가 담당.
        startForeground(NOTIFICATION_ID, buildNotification())

        // 서비스가 살아있어야 걸 수 있는 효과(Alerting)만 여기서 실행한다.
        // 매핑은 순수 함수 effectsFor가 정하고, 이 러너는 자기 것만 골라 실행(#35).
        scope.launch {
            session.state.serviceEffects().collect { effect ->
                when (effect) {
                    ServiceEffect.StartAlerting -> { acquireWakeLock(); haptic.startAlert(); postFlipAlert() }
                    ServiceEffect.StopAlerting -> { haptic.stop(); releaseWakeLock(); cancelFlipAlert() }
                    // 서비스 수명·알람은 앱스코프 컨트롤러가 실행.
                    else -> Unit
                }
            }
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
        // 포그라운드 알림을 명시적으로 제거. notify()로 갱신해온 FGS 알림은 서비스 종료만으론
        // 자동 삭제가 안 돼 워치페이스 아이콘/알림이 남는다 → 여기서 확실히 내린다.
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID)
        cancelFlipAlert()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // --- 알림 / Ongoing Activity ---

    private val notificationManager: NotificationManager
        get() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "타이머 실행", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    /** 뒤집기 알림 채널: heads-up 위해 HIGH, 단 소리·진동은 끔(무음 스펙 + 진동은 haptic이 담당). */
    private fun ensureAlertChannel() {
        if (notificationManager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID, "뒤집기 알림", NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 뒤집기 시점 알림. 손목만 들어도 보이는 heads-up + **full-screen intent**로,
     * 화면이 꺼져 있다 켜질 땐 워치페이스가 아니라 앱(뒤집기 화면)이 바로 뜨게 한다.
     * (화면이 켜져 있으면 heads-up으로만) 탭하면 앱 복귀.
     */
    private fun postFlipAlert() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("뒤집기!")
            .setContentText("탭해서 다음")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .apply {
                touchIntent()?.let {
                    setContentIntent(it)
                    setFullScreenIntent(it, /* highPriority = */ true)
                }
            }
            .build()
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun cancelFlipAlert() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    /**
     * FGS 필수 알림 + **최소 Ongoing Activity**(워치페이스 아이콘 + 탭복귀).
     * 동적 카운트다운/사이클 텍스트는 대상 기기(GW6/WearOS6)에서 렌더되지 않아 싣지 않는다
     * — 확인할 수 없는 표시 정보는 남기지 않는다. 뒤집기 안내는 [postFlipAlert]가 담당.
     */
    private fun buildNotification(): android.app.Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스테이크 타이머")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .apply { touchIntent()?.let { setContentIntent(it) } }

        val ongoing = OngoingActivity.Builder(this, NOTIFICATION_ID, builder)
            .setStaticIcon(android.R.drawable.ic_lock_idle_alarm)
            .setStatus(Status.Builder().addTemplate("스테이크 타이머").build())
            .apply { touchIntent()?.let { setTouchIntent(it) } }
            .build()
        ongoing.apply(this)

        return builder.build()
    }

    /** 워치페이스 칩 탭 → 앱 복귀. 런처 인텐트로 열어 :app 클래스 참조를 피한다. */
    private fun touchIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE)
    }

    // --- WakeLock ---

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

    companion object {
        const val ACTION_DEADLINE = "com.seriouschoi.steaktimer.action.DEADLINE"
        private const val NOTIFICATION_ID = 1
        private const val ALERT_NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "steak_timer_running"
        private const val ALERT_CHANNEL_ID = "steak_timer_alert"
        private const val WAKELOCK_TAG = "SteakTimer::AlertWakeLock"
        private const val WAKELOCK_TIMEOUT_MS = 10L * 60L * 1000L // 10분(안전장치)
    }
}
