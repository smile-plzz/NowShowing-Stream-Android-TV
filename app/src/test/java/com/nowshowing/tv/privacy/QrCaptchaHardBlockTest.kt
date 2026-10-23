package com.nowshowing.tv.privacy

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verifies that Monetag/PropellerAds QR-CAPTCHA delivery infrastructure is hard-blocked
 * by ContentBlockEngine regardless of user shield preferences.
 *
 * Root cause: embed providers (vidsrc.to, autoembed, 2embed etc.) carry Monetag
 * in-page-push scripts that inject a "Confirm you're not a robot – scan the QR code"
 * overlay on top of the video player. The overlay is injected into the top document
 * and is removed by the cosmetic cleanup script; the network requests are blocked here.
 */
class QrCaptchaHardBlockTest {

    private lateinit var engine: ContentBlockEngine

    // Minimal valid rules so loadRules doesn't short-circuit the engine.
    private val minimalRules = (1..6).map { "||placeholder$it.example^" }

    @Before
    fun setUp() {
        engine = ContentBlockEngine()
        engine.loadRules(minimalRules, emptyList())
    }

    // --- Monetag hard-block tests ---

    @Test
    fun monetagIsBlockedWhenShieldsAreOff() {
        val result = engine.evaluate(
            "https://monetag.com/push/sw.js",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("monetag.com must be hard-blocked even with shields off", result.isBlocked)
        assertEquals(BlockType.AD, result.type)
    }

    @Test
    fun monetagSubdomainIsHardBlocked() {
        val result = engine.evaluate("https://cdn.monetag.com/tag.js")
        assertTrue("cdn.monetag.com must be blocked (subdomain of monetag.com)", result.isBlocked)
    }

    @Test
    fun adspyglassIsHardBlocked() {
        val result = engine.evaluate(
            "https://adspyglass.com/push/redirect?id=12345",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("adspyglass.com must be hard-blocked", result.isBlocked)
    }

    @Test
    fun traffectiveIsHardBlocked() {
        val result = engine.evaluate(
            "https://traffective.com/ad/script.js",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("traffective.com must be hard-blocked", result.isBlocked)
    }

    @Test
    fun acengineIsHardBlocked() {
        val result = engine.evaluate("https://acengine.com/api/push")
        assertTrue("acengine.com must be hard-blocked", result.isBlocked)
    }

    @Test
    fun bestpbnIsHardBlocked() {
        val result = engine.evaluate("https://bestpbn.xyz/qr.png")
        assertTrue("bestpbn.xyz must be hard-blocked", result.isBlocked)
    }

    // --- Previously confirmed hostile redirect is still blocked ---

    @Test
    fun gurlleviterIsStillHardBlocked() {
        val result = engine.evaluate(
            "https://unswung.gurlleviter.cyou/ri/146205",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("gurlleviter.cyou must still be hard-blocked", result.isBlocked)
        assertNotNull(result.matchedRule)
    }

    // --- Provider whitelist hosts must not be caught by hard-block ---

    @Test
    fun providerHostsAreNotHardBlocked() {
        val providers = listOf(
            "https://vidsrc.to/embed/movie/tt1375666",
            "https://autoembed.co/movie/imdb/tt1375666",
            "https://www.2embed.cc/embed/tt1375666",
            "https://vidsrc.icu/embed/movie/tt1375666",
            "https://embed.su/embed/movie/tt1375666",
            "https://vidsrc.mov/embed/movie/tt1375666",
        )
        providers.forEach { url ->
            val result = engine.evaluate(url, isAdBlockEnabled = false, isTrackerBlockEnabled = false)
            assertFalse("Provider URL must NOT be hard-blocked: $url", result.isBlocked)
        }
    }
}
