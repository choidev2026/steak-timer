package com.seriouschoi.steaktimer.core.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.seriouschoi.steaktimer.domain.Haptic
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Vibrator 기반 Haptic 구현. 알림 동안 짧은 펄스를 반복하고, stop에서 취소한다.
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
        vibrator.vibrate(effect)
    }

    override fun stop() {
        vibrator.cancel()
    }
}
