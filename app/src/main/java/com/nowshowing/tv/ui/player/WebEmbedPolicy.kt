package com.nowshowing.tv.ui.player

import java.net.URI

/**
 * Navigation policy for provider WebViews.
 *
 * The provider may need cross-origin HTTPS subframes for its actual player, but
 * known hostile/ad-redirect hosts must never become a main frame or resource.
 */
internal object WebEmbedPolicy {
    private val blockedHosts = setOf(
        // Confirmed hostile QR-CAPTCHA redirect domain (previously observed)
        "unswung.gurlleviter.cyou",
        "gurlleviter.cyou",
        // Monetag in-page push and push-notification delivery domains.
        // These serve the "Confirm you're not a robot / scan the QR code" overlay
        // scripts. The network-layer block in ContentBlockEngine covers request
        // interception; this list prevents main-frame and sub-frame navigation
        // to these hosts even when interception is bypassed.
        "monetag.com",
        "mongtag.com",
        "adspyglass.com",
        "traffective.com",
        "acengine.com",
        "bestpbn.xyz",
        "izooto.com",
        "webpushr.com",
        "pushengage.com",
        "adskeeper.co.uk",
        "inpageads.com",
        "ad-stir.com",
        "liveintent.com"
    )

    fun isBlockedHost(host: String?): Boolean {
        val normalized = host?.trim()?.trimEnd('.')?.lowercase() ?: return false
        return blockedHosts.any { normalized == it || normalized.endsWith(".$it") }
    }

    fun allowNavigation(root: String, target: String, mainFrame: Boolean, isolate: Boolean): Boolean =
        try {
            val origin = URI(root)
            val uri = URI(target)
            uri.scheme.equals("https", true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                !isBlockedHost(uri.host) &&
                (!mainFrame || !isolate || uri.host.equals(origin.host, true))
        } catch (_: Exception) {
            false
        }
}
