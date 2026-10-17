package com.nowshowing.tv.playback

import com.nowshowing.tv.domain.source.MediaFormat
import com.nowshowing.tv.domain.source.SourceRegistry
import com.nowshowing.tv.domain.source.VideoSource

class MediaSourceResolver {

    /**
     * Resolves a VideoSource and title information into a validated PlayableSource.
     * Web embeds are explicitly rejected with an IncompatibleSource result.
     */
    fun resolve(
        source: VideoSource,
        imdbId: String,
        title: String,
        season: Int = 1,
        episode: Int = 1,
        isSeries: Boolean = false,
        resumePositionMs: Long = 0L
    ): ResolutionResult {
        // Step 1: Reject web-only iframe embeds immediately
        if (!source.isNativeCompatible) {
            return ResolutionResult.IncompatibleSource(
                source = source,
                reason = "Native playback is unavailable for this source (${source.name}). It is an unsupported web embed. Try another source."
            )
        }

        // Step 2: Build direct media stream URL
        val streamUrl = SourceRegistry.buildNativeMediaUrl(
            source = source,
            imdbId = imdbId,
            season = season,
            episode = episode,
            isSeries = isSeries
        )

        if (streamUrl.isNullOrBlank()) {
            return ResolutionResult.IncompatibleSource(
                source = source,
                reason = "No media stream available for ${source.name}."
            )
        }

        // Step 3: Validate protocol security (HTTPS enforced)
        if (!SourceValidation.isProtocolSecureOrLocal(streamUrl)) {
            return ResolutionResult.Error("Insecure media stream protocol rejected: $streamUrl")
        }

        // Step 4: Validate media format
        val format = SourceValidation.detectMediaFormat(streamUrl)
        if (format == MediaFormat.UNKNOWN) {
            return ResolutionResult.IncompatibleSource(
                source = source,
                reason = "Unsupported media format at $streamUrl. Requires HLS, DASH, or MP4."
            )
        }

        return ResolutionResult.Success(
            PlayableSource(
                source = source,
                mediaUri = streamUrl,
                format = format,
                title = title,
                imdbId = imdbId,
                season = season,
                episode = episode,
                isSeries = isSeries,
                startPositionMs = resumePositionMs
            )
        )
    }

    /**
     * Finds the first available native-playable source as a fallback.
     */
    fun findFirstCompatibleSource(): VideoSource? {
        return SourceRegistry.getNativeCompatibleSources().firstOrNull()
    }
}
