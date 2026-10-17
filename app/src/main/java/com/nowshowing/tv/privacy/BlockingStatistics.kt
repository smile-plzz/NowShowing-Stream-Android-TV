package com.nowshowing.tv.privacy

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.atomic.AtomicLong

class BlockingStatistics(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "nowshowing_blocking_stats"
        private const val KEY_THREATS_BLOCKED = "stat_threats_blocked"
        private const val KEY_TRACKERS_BLOCKED = "stat_trackers_blocked"
        private const val KEY_REDIRECTS_BLOCKED = "stat_redirects_blocked"
    }

    private val threatsCount = AtomicLong(prefs.getLong(KEY_THREATS_BLOCKED, 0L))
    private val trackersCount = AtomicLong(prefs.getLong(KEY_TRACKERS_BLOCKED, 0L))
    private val redirectsCount = AtomicLong(prefs.getLong(KEY_REDIRECTS_BLOCKED, 0L))

    val totalThreatsBlocked: Long
        get() = threatsCount.get()

    val totalTrackersBlocked: Long
        get() = trackersCount.get()

    val totalRedirectsBlocked: Long
        get() = redirectsCount.get()

    val totalBlocked: Long
        get() = totalThreatsBlocked + totalTrackersBlocked + totalRedirectsBlocked

    fun recordThreatBlocked() {
        val newVal = threatsCount.incrementAndGet()
        prefs.edit().putLong(KEY_THREATS_BLOCKED, newVal).apply()
    }

    fun recordTrackerBlocked() {
        val newVal = trackersCount.incrementAndGet()
        prefs.edit().putLong(KEY_TRACKERS_BLOCKED, newVal).apply()
    }

    fun recordRedirectBlocked() {
        val newVal = redirectsCount.incrementAndGet()
        prefs.edit().putLong(KEY_REDIRECTS_BLOCKED, newVal).apply()
    }

    fun clearStats() {
        threatsCount.set(0L)
        trackersCount.set(0L)
        redirectsCount.set(0L)
        prefs.edit().clear().apply()
    }
}
