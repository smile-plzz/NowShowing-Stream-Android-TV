package com.nowshowing.tv.domain.mood

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodRecommendationEngineTest {

    @Test fun everyMoodHasCuratedCandidatesAndTextUsesWholeWords() {
        assertEquals(MoodRecommendationEngine.MOOD_MAP.keys, MoodSeeds.ids.keys)
        MoodSeeds.ids.values.forEach { ids -> assertTrue(ids.isNotEmpty()) }
        assertEquals("Relaxed", MoodRecommendationEngine.guessMoodFromText("I am tired of madness"))
    }

    @Test
    fun stableKeysAndDisplayPhrasesAreUnique() {
        val moods = MoodRecommendationEngine.MOODS
        assertEquals(moods.size, moods.map { it.name }.distinct().size)
        assertEquals(moods.size, moods.map { it.label }.distinct().size)
        assertEquals(moods.map { it.name }.toSet(), MoodRecommendationEngine.MOOD_MAP.keys)
        assertEquals("Happy", MoodRecommendationEngine.guessMoodFromText("Need a Laugh"))
        assertEquals("Anxious", MoodRecommendationEngine.guessMoodFromText("I feel anxious"))
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


