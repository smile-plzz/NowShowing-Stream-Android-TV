package com.nowshowing.tv.ui.player

import android.app.Application
import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class EmbedHostLifecycleTest {
    @Test fun terminatedRendererDetachesOnceAndIgnoresLateLifecycleAndInput() {
        val host = EmbedHost(RuntimeEnvironment.getApplication()) {}
        assertEquals(1, host.childCount)
        host.release(rendererTerminated = true)
        host.release()
        host.pausePlayer()
        host.resumePlayer()
        host.focusPlayer()
        assertEquals(0, host.childCount)
        assertFalse(host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)))
    }
}
