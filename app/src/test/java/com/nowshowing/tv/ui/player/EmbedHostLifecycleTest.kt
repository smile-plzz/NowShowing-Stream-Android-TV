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
        val host = EmbedHost(RuntimeEnvironment.getApplication(), openMenu = {})
        assertEquals(1, host.childCount)
        host.release(rendererTerminated = true)
        host.release()
        host.pausePlayer()
        host.resumePlayer()
        host.focusPlayer()
        assertEquals(0, host.childCount)
        assertFalse(host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER)))
    }

    @Test fun mediaKeysAreInterceptedAndDispatchedToWeb() {
        val host = EmbedHost(RuntimeEnvironment.getApplication(), openMenu = {})
        val mediaKeys = listOf(
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_REWIND
        )
        for (keyCode in mediaKeys) {
            assertTrue("Key $keyCode should be intercepted on ACTION_DOWN",
                host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)))
            assertTrue("Key $keyCode should be intercepted on ACTION_UP",
                host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode)))
        }
        host.release()
        for (keyCode in mediaKeys) {
            assertFalse("Released host should ignore key $keyCode",
                host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode)))
        }
    }

    @Test fun mediaNextTriggersOnNextSourceCallback() {
        var nextTriggered = false
        val host = EmbedHost(
            context = RuntimeEnvironment.getApplication(),
            openMenu = {},
            onNextSource = { nextTriggered = true }
        )
        assertTrue(host.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT)))
        assertTrue(nextTriggered)
        // Repeat count > 0 should not re-trigger
        nextTriggered = false
        assertTrue(host.dispatchKeyEvent(KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 1)))
        assertFalse(nextTriggered)
    }

    @Test fun mediaScriptsContainSafeGuards() {
        listOf(
            EmbedHost.JS_MEDIA_PLAY_PAUSE,
            EmbedHost.JS_MEDIA_PLAY,
            EmbedHost.JS_MEDIA_PAUSE,
            EmbedHost.JS_MEDIA_FORWARD_10S,
            EmbedHost.JS_MEDIA_REWIND_10S
        ).forEach { js ->
            assertTrue(js.contains("findVideo()"))
            assertTrue(js.contains("if (!video) return;"))
            assertTrue(js.contains("catch (e)"))
        }
    }
}
