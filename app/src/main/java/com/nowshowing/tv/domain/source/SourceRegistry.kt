package com.nowshowing.tv.domain.source

import android.content.Context
import android.content.SharedPreferences

object SourceRegistry {

    private const val PREFS_NAME = "nowshowing_source_prefs"
    private const val KEY_PREFERRED_SOURCE = "preferred_source_name"
    private const val KEY_CUSTOM_STREAM_URL = "custom_direct_stream_url"

    // Primary Native-Playable Media Sources (Verified HLS, DASH, and MP4 direct streams)
    val NATIVE_SOURCES = listOf(
        VideoSource(
            id = "native_mux_hls",
            name = "Native HLS (Mux Stream)",
            type = SourceType.NATIVE_STREAM,
            directStreamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            description = "Multi-bitrate adaptive HLS stream with subtitle tracks"
        ),
        VideoSource(
            id = "native_tears_hls",
            name = "Tears of Steel (HLS Demo)",
            type = SourceType.AUTHORIZED_SAMPLE,
            directStreamUrl = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            description = "High-definition 1080p HLS stream via Unified Streaming"
        ),
        VideoSource(
            id = "native_bbb_mp4",
            name = "Big Buck Bunny (MP4)",
            type = SourceType.AUTHORIZED_SAMPLE,
            directStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            description = "Full HD progressive MP4 stream (Blender Open Movie)"
        ),
        VideoSource(
            id = "native_sintel_mp4",
            name = "Sintel (MP4)",
            type = SourceType.AUTHORIZED_SAMPLE,
            directStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            description = "High-definition MP4 stream (Blender Foundation)"
        )
    )

    // Web player providers: rendered in the sandboxed WebView, never passed to ExoPlayer.
    val UNSUPPORTED_WEB_SOURCES = listOf(
        VideoSource(
            id = "vidsrc_to",
            name = "VidSrc.to",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://vidsrc.to/embed/movie/",
            tvUrl = "https://vidsrc.to/embed/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "autoembed",
            name = "AutoEmbed",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://autoembed.co/movie/imdb/",
            tvUrl = "https://autoembed.co/tv/imdb/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "twoembed",
            name = "2Embed",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://www.2embed.cc/embed/",
            tvUrl = "https://www.2embed.cc/embedtv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "twoembed_stream",
            name = "2Embed.stream",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://www.2embed.stream/embed/movie/",
            tvUrl = "https://www.2embed.stream/embed/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "vidsrc_icu",
            name = "VidSrc.icu",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://vidsrc.icu/embed/movie/",
            tvUrl = "https://vidsrc.icu/embed/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "vidsrc_xyz",
            name = "VidSrc.xyz",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://vidsrc.xyz/embed/movie/",
            tvUrl = "https://vidsrc.xyz/embed/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "vidsrc_me",
            name = "VidSrc.me",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://v2.vidsrc.me/embed/",
            tvUrl = "https://v2.vidsrc.me/embed/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "moviesapi",
            name = "MoviesAPI",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://moviesapi.club/movie/",
            tvUrl = "https://moviesapi.club/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "multiembed",
            name = "MultiEmbed",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://multiembed.mov/?video_id=",
            tvUrl = "https://multiembed.mov/?video_id=",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "embedsu",
            name = "EmbedSu",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://embed.su/embed/movie/",
            tvUrl = "https://embed.su/embed/tv/",
            description = "Web player • availability depends on the provider"
        ),
        VideoSource(
            id = "vidsrc_mov",
            name = "VidSrc.mov",
            type = SourceType.UNSUPPORTED_WEB_EMBED,
            url = "https://vidsrc.mov/embed/movie/",
            tvUrl = "https://vidsrc.mov/embed/tv/",
            description = "Web player • availability depends on the provider"
        )
    )

    // Complete registry combining native and classified legacy sources
    val SOURCES = NATIVE_SOURCES + UNSUPPORTED_WEB_SOURCES

    fun getNativeCompatibleSources(): List<VideoSource> {
        return SOURCES.filter { it.isNativeCompatible }
    }

    fun isNativeCompatible(source: VideoSource): Boolean {
        return source.isNativeCompatible
    }

    /**
     * Resolves direct media stream URL for native-compatible sources.
     * Returns null for web-only embeds to prevent iframe/browser rendering.
     */
    fun buildNativeMediaUrl(
        source: VideoSource,
        imdbId: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false
    ): String? {
        if (!source.isNativeCompatible) {
            return null
        }
        return source.directStreamUrl
    }

    /**
     * Legacy URL builder retained for reference and external compatibility checks.
     */
    fun buildVideoUrl(
        source: VideoSource,
        imdbId: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false
    ): String? {
        if (source.isNativeCompatible) {
            return source.directStreamUrl
        }
        if (isSeries) {
            val base = source.tvUrl ?: return null
            return when {
                base.contains("vidsrc.me") -> "$base$imdbId/$season-$episode/"
                base.contains("multiembed.mov") -> "$base$imdbId&s=$season&e=$episode"
                base.contains("2embed.cc") -> "$base$imdbId&s=$season&e=$episode"
                base.contains("autoembed") -> "$base$imdbId/$season/$episode"
                base.contains("moviesapi") -> "$base$imdbId/$season/$episode"
                else -> "$base$imdbId/$season/$episode"
            }
        } else {
            val base = source.url ?: return null
            return when {
                base.contains("vidsrc.me") -> "$base$imdbId/"
                base.contains("multiembed.mov") -> "$base$imdbId"
                else -> "$base$imdbId"
            }
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPreferredSource(context: Context): VideoSource {
        val defaultName = UNSUPPORTED_WEB_SOURCES.first().name
        val name = getPrefs(context).getString(KEY_PREFERRED_SOURCE, defaultName)
        return SOURCES.firstOrNull { it.name == name } ?: UNSUPPORTED_WEB_SOURCES.first()
    }

    fun setPreferredSource(context: Context, sourceName: String) {
        getPrefs(context).edit().putString(KEY_PREFERRED_SOURCE, sourceName).apply()
    }

    fun getNextCompatibleSource(currentSource: VideoSource): VideoSource {
        val compatible = getNativeCompatibleSources()
        if (compatible.isEmpty()) return currentSource
        val idx = compatible.indexOfFirst { it.name == currentSource.name }
        if (idx == -1 || idx >= compatible.lastIndex) {
            return compatible.first()
        }
        return compatible[idx + 1]
    }

    fun getNextSource(currentSource: VideoSource): VideoSource {
        val idx = SOURCES.indexOfFirst { it.name == currentSource.name }
        if (idx == -1 || idx >= SOURCES.lastIndex) {
            return SOURCES.first()
        }
        return SOURCES[idx + 1]
    }
}
