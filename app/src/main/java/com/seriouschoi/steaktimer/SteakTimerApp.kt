package com.seriouschoi.steaktimer

import android.app.Application
import com.seriouschoi.steaktimer.core.timersession.TimerServiceController
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** Hilt 그래프 진입점. 앱은 결합만 하고 로직은 두지 않는다. */
@HiltAndroidApp
class SteakTimerApp : Application() {

    // 세션 상태를 관측해 포그라운드 서비스를 start/stop하는 컨트롤러. 앱 시작 시 관측만 켠다.
    @Inject lateinit var timerServiceController: TimerServiceController

    override fun onCreate() {
        super.onCreate()
        timerServiceController.start()
    }
}
