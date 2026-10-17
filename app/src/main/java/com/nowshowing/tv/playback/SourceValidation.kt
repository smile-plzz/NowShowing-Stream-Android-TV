package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.MediaFormat
import com.nowshowing.tv.privacy.BlockType
import com.nowshowing.tv.privacy.BlockingStatistics
import com.nowshowing.tv.privacy.ContentBlockEngine
import java.net.URI

sealed class RedirectValidationResult {
    object Accepted : RedirectValidationResult()
    data class Rejected(val reason: String) : RedirectValidationResult()
}

object SourceValidation {

    /**
     * Enforces HTTPS for all remote media endpoints.
     * Allows HTTP only for verified local-network IP addresses (NAS, local media server, DLNA).
     */
    fun isProtocolSecureOrLocal(urlString: String): Boolean {
        return try {
            val uri = URI(urlString)
            val scheme = uri.scheme?.lowercase() ?: return false
            if (scheme == "https") return true
            if (scheme == "http") {
                val host = uri.host?.lowercase() ?: return false
                isLocalNetworkHost(host)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a host is on an isolated local network (localhost, 127.0.0.1, 192.168.x.x, 10.x.x.x, 172.16-31.x.x).
     */
    fun isLocalNetworkHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1") return true
        if (host.startsWith("192.168.") || host.startsWith("10.")) return true
        if (host.startsWith("172.")) {
            val parts = host.split(".")
            if (parts.size >= 2) {
                val second = parts[1].toIntOrNull() ?: return false
                return second in 16..31
            }
        }
        return false
    }

    /**
     * Detects media format based on URL path or MIME content type.
     */
    fun detectMediaFormat(urlString: String, contentType: String? = null): MediaFormat {
        val lowerUrl = urlString.lowercase()
        val lowerType = contentType?.lowercase() ?: ""

        return when {
            lowerUrl.contains(".m3u8") ||
                    lowerType.contains("application/x-mpegurl") ||
                    lowerType.contains("application/vnd.apple.mpegurl") -> MediaFormat.HLS

            lowerUrl.contains(".mpd") ||
                    lowerType.contains("application/dash+xml") -> MediaFormat.DASH

            lowerUrl.contains(".mp4") ||
                    lowerType.contains("video/mp4") -> MediaFormat.MP4

            lowerUrl.contains(".webm") ||
                    lowerType.contains("video/webm") -> MediaFormat.MP4

            else -> MediaFormat.UNKNOWN
        }
    }

    /**
     * Validates whether a URI represents a secure and playable media format.
     */
    fun isPlayableMediaUri(urlString: String): Boolean {
        if (!isProtocolSecureOrLocal(urlString)) return false
        val format = detectMediaFormat(urlString)
        return format != MediaFormat.UNKNOWN
    }

    /**
     * Validates redirect destination during network requests to prevent redirect loops,
     * protocol switching, and unwanted ad/tracker redirection.
     */
    fun validateRedirect(
        targetUrl: String,
        hopCount: Int,
        maxHops: Int = 5,
        contentBlockEngine: ContentBlockEngine? = null,
        statistics: BlockingStatistics? = null
    ): RedirectValidationResult {
        if (hopCount > maxHops) {
            statistics?.recordRedirectBlocked()
            return RedirectValidationResult.Rejected("Exceeded maximum redirect depth of $maxHops hops")
        }

        if (!isProtocolSecureOrLocal(targetUrl)) {
            statistics?.recordRedirectBlocked()
            return RedirectValidationResult.Rejected("Insecure or disallowed protocol: $targetUrl")
        }

        // Verify destination is not a known advertising or tracking domain
        if (contentBlockEngine != null) {
            val blockDecision = contentBlockEngine.evaluate(targetUrl)
            if (blockDecision.isBlocked) {
                if (blockDecision.type == BlockType.TRACKER) {
                    statistics?.recordTrackerBlocked()
                } else {
                    statistics?.recordThreatBlocked()
                }
                return RedirectValidationResult.Rejected("Redirect target is a blocked tracker/ad domain: ${blockDecision.matchedRule}")
            }
        }

        return RedirectValidationResult.Accepted
    }
}
