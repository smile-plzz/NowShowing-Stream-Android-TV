package com.nowshowing.tv.ui.player

import org.junit.Assert.*
import org.junit.Test

class WebEmbedPolicyTest {
    private val root = "https://provider.example/embed/movie/tt123"

    @Test fun sameProviderNavigationAllowed() {
        assertTrue(WebEmbedPolicy.allowNavigation(root, "https://provider.example/player", true, true))
    }

    @Test fun adLandingPageCannotReplacePlayer() {
        assertFalse(WebEmbedPolicy.allowNavigation(root, "https://ads.example/", true, true))
        assertFalse(WebEmbedPolicy.allowNavigation(root, "https://provider.example.evil.test/", true, true))
    }

    @Test fun confirmedMaliciousHostIsBlockedForMainFrameAndSubframes() {
        assertTrue(WebEmbedPolicy.isBlockedHost("unswung.gurlleviter.cyou"))
        assertTrue(WebEmbedPolicy.isBlockedHost("gurlleviter.cyou"))
        assertTrue(WebEmbedPolicy.isBlockedHost("cdn.gurlleviter.cyou"))
        assertFalse(WebEmbedPolicy.isBlockedHost("provider.example"))
        assertFalse(WebEmbedPolicy.allowNavigation(root, "https://unswung.gurlleviter.cyou/ri/146205", true, false))
        assertFalse(WebEmbedPolicy.allowNavigation(root, "https://unswung.gurlleviter.cyou/qr.png", false, true))
    }

    @Test fun providerSubframesCanLoadOverHttps() {
        assertTrue(WebEmbedPolicy.allowNavigation(root, "https://cdn.example/player", false, true))
    }

    @Test fun nonNetworkSchemesAndCleartextAreDeniedEvenWithIsolationOff() {
        for (url in listOf("intent://player", "file:///data/private", "javascript:alert(1)", "http://provider.example/", "https://", "https://user@provider.example/")) {
            assertFalse(url, WebEmbedPolicy.allowNavigation(root, url, true, false))
        }
    }

    @Test fun isolationPreferenceOnlyRelaxesHttpsMainFrameHostCheck() {
        assertTrue(WebEmbedPolicy.allowNavigation(root, "https://other.example/player", true, false))
    }

    @Test fun verifiedHostileAndMonetagHostsAreBlocked() {
        // Verified hostile redirect and in-page push delivery hosts blocked from navigation
        val qrHosts = listOf(
            "https://monetag.com/push/sw.js",
            "https://cdn.monetag.com/tag.js",
            "https://unswung.gurlleviter.cyou/ri/146205",
            "https://gurlleviter.cyou/landing",
            "https://inpageads.com/script.js",
            "https://ad-stir.com/push",
        )
        qrHosts.forEach { url ->
            assertFalse(
                "QR/hostile host must be blocked from navigation: $url",
                WebEmbedPolicy.allowNavigation(root, url, mainFrame = true, isolate = false)
            )
            assertFalse(
                "QR/hostile host must be blocked from sub-frame navigation: $url",
                WebEmbedPolicy.allowNavigation(root, url, mainFrame = false, isolate = false)
            )
        }
    }

    @Test fun providerHostsAreStillAllowedAfterQrBlockExpansion() {
        val providers = listOf(
            "https://vidsrc.to/embed/movie/tt1375666",
            "https://autoembed.co/movie/imdb/tt1375666",
            "https://www.2embed.cc/embed/tt1375666",
            "https://vidsrc.icu/embed/movie/tt1375666",
            "https://embed.su/embed/movie/tt1375666",
        )
        providers.forEach { url ->
            assertTrue(
                "Provider URL must still be allowed after QR host expansion: $url",
                WebEmbedPolicy.allowNavigation(root, url, mainFrame = false, isolate = false)
            )
        }
    }
}
