package com.nowshowing.tv.playback

import android.content.Context

class PlaybackPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("playback", Context.MODE_PRIVATE)
    var autoplay: Boolean
        get() = prefs.getBoolean("autoplay_next", true)
        set(value) { prefs.edit().putBoolean("autoplay_next", value).apply() }
}
