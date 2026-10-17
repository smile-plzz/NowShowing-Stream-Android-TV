package com.nowshowing.tv.ui.player

import java.net.URI

/** Main-frame navigation is confined to the selected provider; HTTPS subframes may use its hosts. */
internal object WebEmbedPolicy {
    fun allowNavigation(root: String, target: String, mainFrame: Boolean, isolate: Boolean): Boolean =
        try {
            val origin = URI(root)
            val uri = URI(target)
            uri.scheme.equals("https", true) && !uri.host.isNullOrBlank() &&
                uri.userInfo == null && (!mainFrame || !isolate || uri.host.equals(origin.host, true))
        } catch (_: Exception) { false }
}

