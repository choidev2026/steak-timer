package com.seriouschoi.steaktimer.core.timersession

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.seriouschoi.steaktimer.domain.Haptic
import com.seriouschoi.steaktimer.domain.SteakTimerSession
import com.seriouschoi.steaktimer.domain.SteakTimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 타이머 활성 동안 프로세스를 살려두고(포그라운드 + Ongoing Activity), 알림(Alerting) 동안
 * 진동을 헤드리스에서 구동하는 서비스.
 *
 * 타이밍 보장은 [TimerAlarmScheduler]의 `setAlarmClock`이 맡는다(딥슬립을 뚫는 완주 알람).
 * WakeLock은 Alerting 동안만 잡아 진동을 유지한다. Ongoing Activity로 실행 중 타이머를
 * 워치페이스에 노출하고, 탭하면 앱으로 복귀한다(Step B).
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
        startForeground(NOTIFICATION_ID, buildNotification(session.state.value))

        // Alerting 동안: WakeLock + 진동 + 뒤집기 heads-up 알림. 이탈 시 되돌린다.
        scope.launch {
            observeAlerting(
                state = session.state,
                onEnter = { acquireWakeLock(); haptic.startAlert(); postFlipAlert() },
                onLeave = { haptic.stop(); releaseWakeLock(); cancelFlipAlert() },
            )
        }
        // 표시 상태(러닝 사이클/알림)가 바뀔 때만 Ongoing Activity 갱신(틱마다 X — 카운트다운은 시스템이 렌더).
        scope.launch {
            session.state
                .distinctUntilChangedBy { displayKey(it) }
                .collect { state ->
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(state))
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

    /** 뒤집기 시점에 손목만 들어도 보이는 heads-up 알림. 탭하면 앱 복귀. */
    private fun postFlipAlert() {
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle("뒤집기!")
            .setContentText("탭해서 다음")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .apply { touchIntent()?.let { setContentIntent(it) } }
            .build()
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun cancelFlipAlert() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    /** 표시 갱신 트리거 키: 러닝은 사이클 단위(틱 제외), 알림/그 외는 하나로. */
    private fun displayKey(state: SteakTimerState): String = when (state) {
        is SteakTimerState.Running -> "run:${state.cycle}"
        is SteakTimerState.Alerting -> "alert"
        is SteakTimerState.ConfirmStop -> "confirm"
        SteakTimerState.Idle -> "idle"
    }

    private fun buildNotification(state: SteakTimerState): android.app.Notification {
        val contentText = when (state) {
            is SteakTimerState.Alerting -> "뒤집기!"
            is SteakTimerState.Running -> "${state.cycle + 1}번째 굽는 중"
            else -> "타이머 실행 중"
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("스테이크 타이머")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .apply { touchIntent()?.let { setContentIntent(it) } }

        val status = when (state) {
            // Running: 완주 시각까지 카운트다운(시스템이 초 단위 렌더 → 매 틱 갱신 불필요)
            is SteakTimerState.Running ->
                Status.Builder()
                    .addTemplate("#remain#")
                    .addPart("remain", Status.TimerPart(SystemClock.elapsedRealtime() + state.remainingMs))
                    .build()
            else -> Status.Builder().addTemplate(contentText).build()
        }

        val ongoing = OngoingActivity.Builder(this, NOTIFICATION_ID, builder)
            .setStaticIcon(android.R.drawable.ic_lock_idle_alarm)
            .setStatus(status)
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
