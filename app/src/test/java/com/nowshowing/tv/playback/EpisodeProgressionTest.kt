package com.nowshowing.tv.playback

import org.junit.Assert.*
import org.junit.Test

class EpisodeProgressionTest {
    @Test fun usesActualEpisodeNumbers() {
        assertEquals(EpisodeStep.Play(1, 4), nextEpisodeStep(1, 2, 2, listOf(4, 2, 1, 4)))
    }
    @Test fun rollsOverOnlyAtKnownBoundary() {
        assertEquals(EpisodeStep.LoadSeason(2), nextEpisodeStep(1, 4, 2, listOf(1, 4)))
        assertEquals(EpisodeStep.Complete, nextEpisodeStep(2, 4, 2, listOf(1, 4)))
    }
    @Test fun missingMetadataNeverInventsEpisodes() {
        assertEquals(EpisodeStep.Unavailable, nextEpisodeStep(1, 1, 2, emptyList()))
        assertEquals(EpisodeStep.Unavailable, nextEpisodeStep(1, 3, 2, listOf(1, 2)))
        assertEquals(EpisodeStep.Unavailable, nextEpisodeStep(3, 1, 2, listOf(1)))
    }
}
