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
        // Confirmed hostile QR-CAPTCHA redirect landing domains
        "unswung.gurlleviter.cyou",
        "gurlleviter.cyou",
        // Verified Monetag and in-page push delivery networks
        "monetag.com",
        "inpageads.com",
        "ad-stir.com"
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
