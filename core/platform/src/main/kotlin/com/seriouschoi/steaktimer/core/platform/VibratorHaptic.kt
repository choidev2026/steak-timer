package com.seriouschoi.steaktimer.core.platform

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.seriouschoi.steaktimer.domain.Haptic
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Vibrator 기반 Haptic 구현. 알림 동안 짧은 펄스를 반복하고, stop에서 취소한다.
 *
 * 진동을 **ALARM usage**로 낸다: 화면 꺼짐/Doze/AOD에서 시스템이 일반(UNKNOWN) 진동을
 * 억제하기 때문에, 알람으로 표시해야 그 억제를 뚫고 안 보는 중에도 울린다(Phase 7).
 */
class VibratorHaptic @Inject constructor(
    @ApplicationContext context: Context,
) : Haptic {

    private val vibrator: Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    override fun startAlert() {
        // [대기 0, 진동 400, 쉼 300] 을 index 0부터 반복 → 400ms 진동 / 300ms 쉼 펄스 반복
        val timings = longArrayOf(0L, 400L, 300L)
        val effect = VibrationEffect.createWaveform(timings, /* repeat = */ 0)
        // ALARM usage로 내야 Doze/화면 꺼짐에서 억제되지 않는다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, attrs)
        }
    }

    override fun stop() {
        vibrator.cancel()
    }
}
