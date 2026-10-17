package com.nowshowing.tv.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodRecommendationEngineTest {

    @Test
    fun testAll29MoodsPresent() {
        assertEquals("Must contain 29 moods matching web app", 29, MoodRecommendationEngine.MOODS.size)
        assertEquals("Must contain mappings for all 29 moods", 29, MoodRecommendationEngine.MOOD_MAP.size)
    }

    @Test
    fun testMoodMappingStructure() {
        MoodRecommendationEngine.MOODS.forEach { mood ->
            val rule = MoodRecommendationEngine.MOOD_MAP[mood.name]
            assertNotNull("Mood rule must exist for ${mood.name}", rule)
            assertTrue("Mood ${mood.name} must have genres", rule!!.genres.isNotEmpty())
            assertTrue("Mood ${mood.name} must have keywords", rule.keywords.isNotEmpty())
        }
    }

    @Test
    fun testKeywordMoodGuessing() {
        assertEquals("Happy", MoodRecommendationEngine.guessMoodFromText("I am feeling great today"))
        assertEquals("Sad", MoodRecommendationEngine.guessMoodFromText("I feel so lonely and blue"))
        assertEquals("Angry", MoodRecommendationEngine.guessMoodFromText("I am so furious right now"))
        assertEquals("Romantic", MoodRecommendationEngine.guessMoodFromText("Feeling the love in the air"))
        assertEquals("Happy", MoodRecommendationEngine.guessMoodFromText("Some completely random statement"))
    }
}
