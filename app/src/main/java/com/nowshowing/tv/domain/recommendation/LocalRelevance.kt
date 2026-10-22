package com.nowshowing.tv.domain.recommendation

import com.nowshowing.tv.data.model.OmdbTitleDetail
import com.nowshowing.tv.domain.mood.MoodRule

/** Deliberately conservative local heuristics, not semantic AI or full-catalog discovery. */
object LocalRelevance {
    private fun normalize(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    private fun genres(detail: OmdbTitleDetail) = detail.genre.orEmpty().split(",")
        .map(::normalize).filter { it.isNotBlank() && it != "n a" }.toSet()
    fun startYear(value: String): Int? = Regex("^\\d{4}(?:[---](?:\\d{4})?)?$").matchEntire(value.trim())
        ?.value?.take(4)?.toIntOrNull()
    private fun contains(text: String, phrase: String) = " ${normalize(text)} ".contains(" ${normalize(phrase)} ")
    fun moodScore(detail: OmdbTitleDetail, rule: MoodRule, curated: Boolean = false): Int {
        val actualGenres = genres(detail)
        if (actualGenres.any { it in rule.excludedGenres.map(::normalize) }) return 0
        val overlap = actualGenres.intersect(rule.genres.map(::normalize).toSet()).size
        if (overlap == 0) return 0
        val evidence = rule.keywords.count { contains(detail.plot.orEmpty(), it) }
        if (evidence == 0 && rule.requirePlotEvidence && !curated) return 0
        return (if (curated) 40 else 0) + evidence * 8 + overlap
    }
    private val themes = listOf(
        listOf("revenge", "vengeance", "payback"), listOf("murder", "killer", "detective", "investigation"),
        listOf("memory", "memories", "amnesia"), listOf("conspiracy", "spy", "undercover"),
        listOf("space", "planet", "astronaut"), listOf("war", "soldier", "military"),
        listOf("haunted", "ghost", "supernatural"), listOf("friendship", "friends"),
        listOf("marriage", "divorce", "affair"), listOf("survive", "survival", "stranded"),
        listOf("dream", "dreams", "reality"), listOf("prison", "escape"))
    fun relatedScore(source: OmdbTitleDetail, candidate: OmdbTitleDetail): Int {
        if (source.imdbID == candidate.imdbID || source.type == null || source.type != candidate.type) return 0
        val overlap = genres(source).intersect(genres(candidate)).size
        if (overlap == 0) return 0
        val shared = themes.count { words -> words.any { contains(source.plot.orEmpty(), it) } && words.any { contains(candidate.plot.orEmpty(), it) } }
        if (shared == 0) return 0
        val director = source.director?.takeUnless { it == "N/A" || it.isBlank() }
        return shared * 10 + overlap * 2 + if (director != null && director == candidate.director) 1 else 0
    }
}


