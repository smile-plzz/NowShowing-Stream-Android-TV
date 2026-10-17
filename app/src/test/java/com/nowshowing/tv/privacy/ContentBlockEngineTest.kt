package com.nowshowing.tv.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentBlockEngineTest {

    private lateinit var engine: ContentBlockEngine

    private val sampleAdRules = listOf(
        "||doubleclick.net^",
        "||popads.net^",
        "||adservice.google.com^",
        "||deloton.com^",
        "/ad/banner/*",
        "*popunder*",
        "@@||vidsrc.to^",
        "@@||autoembed.co^",
        "@@||streamamg.com^"
    )

    private val sampleTrackerRules = listOf(
        "||google-analytics.com^",
        "||hotjar.com^",
        "||scorecardresearch.com^",
        "*user_tracker*"
    )

    @Before
    fun setUp() {
        engine = ContentBlockEngine()
        engine.loadRules(sampleAdRules, sampleTrackerRules)
    }

    @Test
    fun testKnownAdNetworksBlocked() {
        val decision1 = engine.evaluate("https://doubleclick.net/ad/test.js")
        assertTrue("DoubleClick must be blocked", decision1.isBlocked)
        assertEquals(BlockType.AD, decision1.type)

        val decision2 = engine.evaluate("https://subdomain.popads.net/serve")
        assertTrue("PopAds subdomain must be blocked", decision2.isBlocked)
        assertEquals(BlockType.AD, decision2.type)
    }

    @Test
    fun testKnownTrackersBlocked() {
        val decision = engine.evaluate("https://www.google-analytics.com/analytics.js")
        assertTrue("Google Analytics must be blocked", decision.isBlocked)
        assertEquals(BlockType.TRACKER, decision.type)

        val decision2 = engine.evaluate("https://static.hotjar.com/c/hotjar-123.js")
        assertTrue("Hotjar must be blocked", decision2.isBlocked)
        assertEquals(BlockType.TRACKER, decision2.type)
    }

    @Test
    fun testWhitelistExceptionsAllowed() {
        val decision = engine.evaluate("https://vidsrc.to/embed/movie/tt1375666")
        assertFalse("Whitelisted stream source must not be blocked", decision.isBlocked)

        val decision2 = engine.evaluate("https://autoembed.co/movie/imdb/tt1375666")
        assertFalse("Whitelisted autoembed must not be blocked", decision2.isBlocked)
    }

    @Test
    fun testMediaStreamsAllowedOnCleanCdns() {
        val decision = engine.evaluate("https://video-cdn.example.com/hls/segment_001.ts")
        assertFalse("TS media chunk must not be blocked", decision.isBlocked)

        val decision2 = engine.evaluate("https://video-cdn.example.com/hls/master.m3u8")
        assertFalse("HLS m3u8 playlist must not be blocked", decision2.isBlocked)

        val decision3 = engine.evaluate("https://commondatastorage.googleapis.com/sample/BigBuckBunny.mp4")
        assertFalse("MP4 stream must not be blocked", decision3.isBlocked)
    }

    @Test
    fun testPathAndWildcardRules() {
        val decision = engine.evaluate("https://random-site.com/ad/banner/promo.png")
        assertTrue("Path rule /ad/banner/* must be blocked", decision.isBlocked)

        val decision2 = engine.evaluate("https://random-site.com/scripts/popunder.js")
        assertTrue("Wildcard *popunder* must be blocked", decision2.isBlocked)
    }

    @Test
    fun testNormalContentAllowed() {
        val decision = engine.evaluate("https://m.media-amazon.com/images/M/poster.jpg")
        assertFalse("Clean content and posters must not be blocked", decision.isBlocked)
    }

    @Test
    fun testThirdPartyDetection() {
        assertTrue(engine.isThirdParty("https://ad.network.com/tag.js", "https://vidsrc.to/player"))
        assertFalse(engine.isThirdParty("https://vidsrc.to/script.js", "https://vidsrc.to/player"))
        assertFalse(engine.isThirdParty("https://sub.vidsrc.to/script.js", "https://vidsrc.to/player"))
    }
}
