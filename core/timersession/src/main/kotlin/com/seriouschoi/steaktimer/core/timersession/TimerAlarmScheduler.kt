package com.seriouschoi.steaktimer.core.timersession

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인터벌 완주 시각에 정확히 발화할 알람을 시스템에 예약한다.
 *
 * `setAlarmClock`을 쓰는 이유: 딥슬립(Doze/suspend)을 뚫고 **정확한 시각에** 발화한다.
 * (`set`/`setRepeating`은 배터리 절약으로 부정확하게 묶이고, `setExactAndAllowWhileIdle`는
 * Doze 중 앱당 ~9분 제한이 있어 짧은 인터벌엔 부적합. `setAlarmClock`은 알람시계용이라 제한 없음.)
 *
 * 발화 대상은 이미 떠 있는 [TimerForegroundService](ACTION_DEADLINE)로, CPU를 붙잡은 채
 * 세션을 완주 처리 → Alerting → 진동으로 이어진다.
 */
@Singleton
class TimerAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val operation: PendingIntent
        get() = PendingIntent.getForegroundService(
            context,
            REQUEST_CODE,
            Intent(context, TimerForegroundService::class.java).setAction(TimerForegroundService.ACTION_DEADLINE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** 지금부터 [afterMillis] 뒤에 발화하도록 예약(기존 예약은 대체). */
    fun scheduleAfter(afterMillis: Long) {
        val triggerAt = System.currentTimeMillis() + afterMillis
        val info = AlarmManager.AlarmClockInfo(triggerAt, /* showIntent = */ null)
        alarmManager.setAlarmClock(info, operation)
    }

    fun cancel() {
        alarmManager.cancel(operation)
    }

    private companion object {
        const val REQUEST_CODE = 1001
    }
}
