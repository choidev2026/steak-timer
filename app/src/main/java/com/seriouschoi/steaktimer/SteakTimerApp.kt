package com.seriouschoi.steaktimer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt 그래프 진입점. 앱은 결합만 하고 로직은 두지 않는다. */
@HiltAndroidApp
class SteakTimerApp : Application()
