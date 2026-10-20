package com.nowshowing.tv.domain.recommendation

import com.google.gson.Gson
import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.domain.mood.MoodRecommendationEngine
import org.junit.Assert.*
import org.junit.Test

class LocalRelevanceTest {
    private fun title(id: String, genre: String, plot: String, type: String = "movie"): OmdbTitleDetail =
        Gson().fromJson(Gson().toJson(mapOf("imdbID" to id, "Genre" to genre, "Plot" to plot,
            "Type" to type, "Title" to "Fixture", "Year" to "2024", "Response" to "True")), OmdbTitleDetail::class.java)

    @Test fun exactYearHandlesSeriesAndRejectsMalformedValues() {
        assertEquals(2024, LocalRelevance.startYear("2024"))
        assertEquals(2011, LocalRelevance.startYear("2011–2019"))
        assertEquals(2022, LocalRelevance.startYear("2022–"))
        listOf("N/A", "", "2024oops", "2024-abc", "24").forEach { assertNull(LocalRelevance.startYear(it)) }
    }
    @Test fun everyMoodHasPositiveEvidenceAndRejectsUnrelatedMetadata() {
        MoodRecommendationEngine.MOOD_MAP.forEach { (name, rule) ->
            assertTrue(name, LocalRelevance.moodScore(title("tt0000001", rule.genres.first(),
                "A story about ${rule.keywords.first()}."), rule) > 0)
            assertEquals(name, 0, LocalRelevance.moodScore(title("tt0000002", "Western", "A cattle drive."), rule))
        }
    }
    @Test fun specificMoodsRequireThemeEvidence() {
        listOf("heartbreak", "complicated_love", "friendship", "paranoia", "redemption", "underdog").forEach { name ->
            val rule = MoodRecommendationEngine.MOOD_MAP.getValue(name)
            assertEquals(name, 0, LocalRelevance.moodScore(title("tt0000001", rule.genres.joinToString(), "Ordinary life."), rule))
        }
    }
    @Test fun themeOutweighsGenericGenreAndCastCannotQualify() {
        val source = title("tt0000001", "Mystery, Thriller", "A detective investigates a murder.")
        val good = title("tt0000002", "Mystery, Thriller", "An investigation reveals a killer.")
        val weak = title("tt0000003", "Mystery, Thriller", "An ordinary family moves home.")
        assertTrue(LocalRelevance.relatedScore(source, good) > LocalRelevance.relatedScore(source, weak))
        assertEquals(0, LocalRelevance.relatedScore(source, weak))
        assertEquals(0, LocalRelevance.relatedScore(source, source))
        assertEquals(0, LocalRelevance.relatedScore(source, good.copy(type = "series")))
        assertEquals(0, LocalRelevance.relatedScore(source, good.copy(plot = "A murderous person with detectivework.")))
    }
}
