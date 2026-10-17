package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.MediaFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceValidationTest {

    @Test
    fun testProtocolSecurityEnforcesHttps() {
        assertTrue(SourceValidation.isProtocolSecureOrLocal("https://test-streams.mux.dev/stream.m3u8"))
        assertTrue(SourceValidation.isProtocolSecureOrLocal("https://commondatastorage.googleapis.com/sample.mp4"))

        // HTTP for external domains must be rejected
        assertFalse(SourceValidation.isProtocolSecureOrLocal("http://external-streaming-site.com/video.mp4"))

        // Untrusted schemes must be rejected
        assertFalse(SourceValidation.isProtocolSecureOrLocal("intent://play?url=abc#Intent;scheme=http;package=com.nowshowing.tv;end"))
        assertFalse(SourceValidation.isProtocolSecureOrLocal("javascript:alert(1)"))
        assertFalse(SourceValidation.isProtocolSecureOrLocal("file:///sdcard/malware.apk"))
        assertFalse(SourceValidation.isProtocolSecureOrLocal("market://details?id=com.random.app"))
    }

    @Test
    fun testLocalNetworkHostExemption() {
        // Local network HTTP endpoints are permitted for home media servers
        assertTrue(SourceValidation.isLocalNetworkHost("localhost"))
        assertTrue(SourceValidation.isLocalNetworkHost("127.0.0.1"))
        assertTrue(SourceValidation.isLocalNetworkHost("192.168.1.50"))
        assertTrue(SourceValidation.isLocalNetworkHost("10.0.0.15"))
        assertTrue(SourceValidation.isLocalNetworkHost("172.20.1.100"))

        assertFalse(SourceValidation.isLocalNetworkHost("8.8.8.8"))
        assertFalse(SourceValidation.isLocalNetworkHost("vidsrc.to"))

        assertTrue(SourceValidation.isProtocolSecureOrLocal("http://192.168.1.100:8080/movie.mp4"))
        assertTrue(SourceValidation.isProtocolSecureOrLocal("http://localhost:8000/stream.m3u8"))
    }

    @Test
    fun testMediaFormatDetection() {
        assertEquals(MediaFormat.HLS, SourceValidation.detectMediaFormat("https://domain.com/video.m3u8"))
        assertEquals(MediaFormat.HLS, SourceValidation.detectMediaFormat("https://domain.com/stream", "application/vnd.apple.mpegurl"))
        assertEquals(MediaFormat.HLS, SourceValidation.detectMediaFormat("https://domain.com/stream", "application/x-mpegURL"))

        assertEquals(MediaFormat.DASH, SourceValidation.detectMediaFormat("https://domain.com/manifest.mpd"))
        assertEquals(MediaFormat.DASH, SourceValidation.detectMediaFormat("https://domain.com/stream", "application/dash+xml"))

        assertEquals(MediaFormat.MP4, SourceValidation.detectMediaFormat("https://domain.com/sample.mp4"))
        assertEquals(MediaFormat.MP4, SourceValidation.detectMediaFormat("https://domain.com/sample", "video/mp4"))

        assertEquals(MediaFormat.UNKNOWN, SourceValidation.detectMediaFormat("https://domain.com/index.html"))
        assertEquals(MediaFormat.UNKNOWN, SourceValidation.detectMediaFormat("https://vidsrc.to/embed/movie/tt12345"))
    }

    @Test
    fun testRedirectHopLimitValidation() {
        // Valid redirect within limit
        val validResult = SourceValidation.validateRedirect("https://cdn.example.com/stream.m3u8", hopCount = 2, maxHops = 5)
        assertTrue(validResult is RedirectValidationResult.Accepted)

        // Redirect exceeding maximum allowed hops
        val loopResult = SourceValidation.validateRedirect("https://cdn.example.com/loop.m3u8", hopCount = 6, maxHops = 5)
        assertTrue(loopResult is RedirectValidationResult.Rejected)
        assertEquals("Exceeded maximum redirect depth of 5 hops", (loopResult as RedirectValidationResult.Rejected).reason)

        // Redirect to insecure protocol
        val insecureResult = SourceValidation.validateRedirect("http://evil-tracker.com/pixel", hopCount = 1)
        assertTrue(insecureResult is RedirectValidationResult.Rejected)
    }
}
