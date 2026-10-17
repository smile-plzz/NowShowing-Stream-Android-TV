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
}
