package com.nowshowing.tv.domain.source

enum class SourceType {
    NATIVE_STREAM,            // Direct HLS / DASH / MP4 media stream
    AUTHORIZED_SAMPLE,        // Verified public reference stream (e.g. Blender Foundation, Akamai, Mux)
    UNSUPPORTED_WEB_EMBED     // Legacy 3rd-party iframe/web-only embed (incompatible with native player)
}

enum class SourceCompatibility {
    NATIVE_PLAYABLE,
    REQUIRES_DIRECT_ENDPOINT,
    UNSUPPORTED_WEB_ONLY,
    UNAVAILABLE
}

enum class MediaFormat {
    HLS,
    DASH,
    MP4,
    UNKNOWN
}

data class VideoSource(
    val id: String = "",
    val name: String,
    val type: SourceType = SourceType.UNSUPPORTED_WEB_EMBED,
    val url: String? = null,
    val tvUrl: String? = null,
    val directStreamUrl: String? = null,
    val description: String = "",
    val compatibility: SourceCompatibility = when (type) {
        SourceType.NATIVE_STREAM, SourceType.AUTHORIZED_SAMPLE -> SourceCompatibility.NATIVE_PLAYABLE
        SourceType.UNSUPPORTED_WEB_EMBED -> SourceCompatibility.UNSUPPORTED_WEB_ONLY
    }
) {
    val isNativeCompatible: Boolean
        get() = compatibility == SourceCompatibility.NATIVE_PLAYABLE
}
