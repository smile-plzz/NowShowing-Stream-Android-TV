package com.nowshowing.tv.playback

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class PlaybackPreferencesTest {
    @Test fun defaultOnAndPersistsOff() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("playback", 0).edit().clear().commit()
        assertTrue(PlaybackPreferences(context).autoplay)
        PlaybackPreferences(context).autoplay = false
        assertFalse(PlaybackPreferences(context).autoplay)
    }
}
