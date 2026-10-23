package com.nowshowing.tv.ui.player

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies that the QR-overlay cleanup asset exists, is non-empty, and contains
 * the structural markers required for correct operation.
 *
 * These tests run on the JVM via Robolectric so the asset filesystem is accessible.
 * They do not execute JavaScript — they validate that the file shipped is the one
 * the WebView will receive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class QrOverlayCleanupTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun loadScript(): String =
        context.assets.open("ad_overlay_cleanup.js").bufferedReader().readText()

    private fun stripComments(js: String): String =
        js.replace(Regex("""/\*[\s\S]*?\*/"""), "").replace(Regex("""//.*"""), "")

    @Test
    fun cleanupScriptAssetExists() {
        val script = loadScript()
        assertTrue("Cleanup script must be non-empty", script.isNotBlank())
    }

    @Test
    fun cleanupScriptHasIdempotencyGuard() {
        val script = loadScript()
        assertTrue(
            "Script must guard against re-injection with window.__nsAdCleanup",
            script.contains("window.__nsAdCleanup")
        )
    }

    @Test
    fun cleanupScriptContainsMutationObserver() {
        val script = loadScript()
        val code = stripComments(script)
        assertTrue(
            "Script must use MutationObserver instead of setInterval",
            code.contains("MutationObserver")
        )
        assertFalse(
            "Script must not use setInterval (polling is forbidden)",
            code.contains("setInterval")
        )
    }

    @Test
    fun cleanupScriptMatchesMultipleKeywords() {
        val script = loadScript()
        // Must match more than one keyword so a language-change doesn't break it
        assertTrue("Must include 'not a robot'", script.contains("not a robot"))
        assertTrue("Must include 'scan the qr'", script.contains("scan the qr"))
        assertTrue("Must include 'qr code'", script.contains("qr code"))
    }

    @Test
    fun cleanupScriptUsesZIndexThreshold() {
        val script = loadScript()
        assertTrue(
            "Script must check z-index to avoid hiding provider controls",
            script.contains("Z_THRESHOLD") || script.contains("zIndex") || script.contains("z-index")
        )
    }

    @Test
    fun cleanupScriptDoesNotManipulateFocus() {
        val script = loadScript()
        val code = stripComments(script)
        // On Android TV, focus must never be stolen from D-pad navigation.
        assertFalse(
            "Script must not call focus() — that steals D-pad control",
            Regex("""(?<![_a-zA-Z])focus\s*\(""").containsMatchIn(code)
        )
        assertFalse(
            "Script must not set tabindex — that disrupts D-pad order",
            code.contains("tabindex", ignoreCase = true) ||
                code.contains("tabIndex")
        )
    }

    @Test
    fun cleanupScriptDoesNotUseInlineEventHandlers() {
        // Inline event handlers (onclick=, onkeydown=) would intercept D-pad key events.
        val script = loadScript()
        val code = stripComments(script)
        assertFalse("Script must not set onclick handlers", code.contains("onclick"))
        assertFalse("Script must not set onkeydown handlers", code.contains("onkeydown"))
        assertFalse("Script must not set onkeyup handlers", code.contains("onkeyup"))
    }
}
