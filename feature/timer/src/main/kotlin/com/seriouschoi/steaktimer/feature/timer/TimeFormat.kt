package com.seriouschoi.steaktimer.feature.timer

/** 밀리초를 mm:ss로. 카운트다운 느낌을 위해 올림(1ms도 1초로 표시). */
internal fun formatMmSs(ms: Long): String {
    val totalSec = ((ms + 999) / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
