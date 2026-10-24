package com.nowshowing.tv.privacy

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Verifies that confirmed hostile redirects and verified Monetag/in-page push
 * delivery networks are hard-blocked by ContentBlockEngine regardless of user
 * shield preferences.
 *
 * Generic push services (e.g. webpushr, izooto) are tested in filter list
 * evaluation where user shield toggles are respected.
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

    // --- Hard-block tests (shields off) ---

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
    fun inpageAdsIsHardBlocked() {
        val result = engine.evaluate(
            "https://inpageads.com/script.js",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("inpageads.com must be hard-blocked", result.isBlocked)
    }

    @Test
    fun adStirIsHardBlocked() {
        val result = engine.evaluate(
            "https://ad-stir.com/push",
            isAdBlockEnabled = false,
            isTrackerBlockEnabled = false
        )
        assertTrue("ad-stir.com must be hard-blocked", result.isBlocked)
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

    // --- Generic push services are NOT hard-blocked (shields-off allows them) ---

    @Test
    fun genericPushServicesAreNotHardBlockedWhenShieldsOff() {
        val genericServices = listOf(
            "https://webpushr.com/sw.js",
            "https://izooto.com/push",
            "https://pushengage.com/sw.js"
        )
        genericServices.forEach { url ->
            val result = engine.evaluate(url, isAdBlockEnabled = false, isTrackerBlockEnabled = false)
            assertFalse("Generic push service must respect user shield off: $url", result.isBlocked)
        }
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