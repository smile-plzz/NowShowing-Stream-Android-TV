package com.nowshowing.tv.domain.mood

import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MoodRecommendationEngine {

    val MOODS = listOf(
        MoodItem("Happy", "😊", "Need a Laugh", "Light-hearted fun and feel-good stories."),
        MoodItem("Sad", "😢", "Let Me Cry", "Films that let you feel it all, deeply."),
        MoodItem("Excited", "🤩", "Bring the Adrenaline", "High-energy films that get your pulse going."),
        MoodItem("Relaxed", "😌", "Help Me Unwind", "Gentle, easy films for winding down."),
        MoodItem("Angry", "😤", "Fuel My Rage", "Intense stories that channel the rage."),
        MoodItem("Scared", "😱", "Scare Me Tonight", "Horror and thrillers for the brave."),
        MoodItem("Inspired", "✨", "Get Me Motivated", "Films that spark something inside you."),
        MoodItem("Nostalgic", "🕰️", "Take Me Back", "Classic stories from eras gone by."),
        MoodItem("Curious", "🔍", "Feed My Curiosity", "Mysteries, puzzles and strange worlds."),
        MoodItem("Romantic", "💕", "Make Me Swoon", "Stories of connection, longing, and love."),
        MoodItem("Adventurous", "🌍", "Take Me Somewhere", "Epic journeys and far-flung worlds."),
        MoodItem("Thoughtful", "🤔", "Question My World", "Films that leave you pondering life."),
        MoodItem("Anxious", "😰", "Calm My Nerves", "Comforting, gentle picks to ease the feeling."),
        MoodItem("Hopeful", "🌅", "Give Me Hope", "Stories that restore your faith in everything."),
        MoodItem("Playful", "🎮", "Keep It Playful", "Fun, silly, and wonderfully light."),
        MoodItem("Adult", "🎭", "Keep It Grown", "Complex, grown-up cinema for serious viewing."),
        MoodItem("Wired", "⚡", "Keep Me Wired", "Fast-paced, kinetic, caffeine cinema."),
        MoodItem("Vengeful", "🗡️", "Make Them Pay", "Cold, calculated payback stories."),
        MoodItem("Goofy", "🤪", "Make It Ridiculous", "Absurd comedy that makes no apologies."),
        MoodItem("Eerie", "👁️", "Something Feels Wrong", "Slow-burn dread and atmospheric horror."),
        MoodItem("Empowered", "💪", "Make Me Fearless", "Characters who rise, overcome, and win."),
        MoodItem("Jaded", "😒", "Keep It Cynical", "Cynical, sharp, and darkly funny films."),
        MoodItem("Awestruck", "🤯", "Blow My Mind", "Visuals and ideas that defy imagination."),
        MoodItem("Challenged", "🧩", "Make Me Think", "Films that demand your full attention."),
        MoodItem("Giddy", "🥳", "Lift My Spirits", "Pure celebration and contagious happiness."),
        MoodItem("Focused", "🎯", "Keep Me Hooked", "Tight, precise, no-fluff storytelling."),
        MoodItem("Escapist", "🚀", "Get Me Away", "Worlds so good you forget this one."),
        MoodItem("Intrigued", "🕵️", "Keep Me Guessing", "Slow reveals and layered mysteries."),
        MoodItem("Tense", "😬", "Keep Me Tense", "Nerve-shredding, palm-sweating cinema."),
        MoodItem("heartbreak", "💔", "Break My Heart", "Relationship loss and heartbreak."),
        MoodItem("complicated_love", "💞", "Love Gets Complicated", "Messy relationships and conflicting commitments."),
        MoodItem("friendship", "🤝", "Friendship Hits Different", "Friendship, belonging and found family."),
        MoodItem("paranoia", "👀", "Trust Absolutely Nobody", "Conspiracies, distrust and hidden motives."),
        MoodItem("redemption", "🌤", "One Last Chance", "Rebuilding a life and earning a second chance."),
        MoodItem("underdog", "🏆", "Root For Underdogs", "Outsiders overcoming the odds.")
    )

    val MOOD_MAP = mapOf(
        "heartbreak" to MoodRule(listOf("Romance", "Drama"), listOf("breakup", "break up", "separation", "loss", "lost love", "divorce")),
        "complicated_love" to MoodRule(listOf("Romance", "Drama"), listOf("affair", "marriage", "divorce", "triangle", "unfaithful")),
        "friendship" to MoodRule(listOf("Drama", "Comedy", "Adventure"), listOf("friendship", "friends", "best friend", "bond")),
        "paranoia" to MoodRule(listOf("Thriller", "Mystery"), listOf("conspiracy", "conspiracies", "undercover", "betrayal", "surveillance")),
        "redemption" to MoodRule(listOf("Drama", "Crime"), listOf("redemption", "redeem", "second chance", "rebuild", "rehabilitation")),
        "underdog" to MoodRule(listOf("Sport", "Drama"), listOf("underdog", "underdogs", "against the odds", "unlikely", "struggling boxer")),
        "Happy" to MoodRule(listOf("Comedy", "Musical", "Family", "Adventure"), listOf("feel good", "lighthearted", "heartwarming", "joyful", "uplifting"), requirePlotEvidence = false, excludedGenres = setOf("Horror", "Thriller", "Crime", "War")),
        "Sad" to MoodRule(listOf("Drama", "Romance"), listOf("poignant", "tearjerker", "bittersweet", "melancholy", "reflective", "indie", "slice of life")),
        "Excited" to MoodRule(listOf("Action", "Thriller", "Sci-Fi", "Sport"), listOf("high octane", "edge of your seat", "explosive", "pulse pounding", "adrenaline")),
        "Relaxed" to MoodRule(listOf("Documentary", "Romance", "Animation"), listOf("calming", "soothing", "gentle", "low stakes", "peaceful", "slice of life", "nature"), requirePlotEvidence = false, excludedGenres = setOf("Horror", "Thriller", "Crime", "War")),
        "Angry" to MoodRule(listOf("Action", "Crime", "Thriller"), listOf("vengeful", "raw", "intense", "dark", "confrontational", "revenge")),
        "Scared" to MoodRule(listOf("Horror", "Mystery", "Thriller"), listOf("spine chilling", "paranormal", "haunting", "tense", "nightmare", "supernatural"), requirePlotEvidence = false),
        "Inspired" to MoodRule(listOf("Biography", "Drama", "Musical", "Fantasy", "Sci-Fi", "Documentary"), listOf("motivational", "uplifting", "triumphant", "visionary", "aspirational")),
        "Nostalgic" to MoodRule(listOf("Romance", "Family", "History"), listOf("retro", "vintage", "memory lane", "throwback", "sentimental", "coming of age", "classic")),
        "Curious" to MoodRule(listOf("Mystery", "Documentary", "Sci-Fi", "Thriller"), listOf("investigative", "enigmatic", "mind bending", "clue", "sleuth", "psychological")),
        "Romantic" to MoodRule(listOf("Romance"), listOf("love story", "meet cute", "tender", "passionate", "swoon", "period romance"), requirePlotEvidence = false),
        "Adventurous" to MoodRule(listOf("Adventure", "Action", "Fantasy", "Sci-Fi"), listOf("journey", "quest", "uncharted", "bold", "sweeping", "epic"), requirePlotEvidence = false),
        "Thoughtful" to MoodRule(listOf("Drama", "Biography", "History"), listOf("introspective", "contemplative", "layered", "intellectual", "philosophical", "indie", "psychological")),
        "Anxious" to MoodRule(listOf("Comedy", "Animation", "Documentary", "Family"), listOf("comfort", "gentle", "uplifting", "wholesome", "calming", "feel good", "nature"), requirePlotEvidence = false, excludedGenres = setOf("Horror", "Thriller", "Crime", "War")),
        "Hopeful" to MoodRule(listOf("Drama", "Sport", "Family", "Musical", "Fantasy"), listOf("optimistic", "triumph", "feel good", "against the odds", "heartening", "inspirational")),
        "Playful" to MoodRule(listOf("Comedy", "Animation", "Family", "Musical", "Adventure"), listOf("whimsical", "imaginative", "silly", "joyful", "colorful"), requirePlotEvidence = false, excludedGenres = setOf("Horror", "Thriller", "Crime", "War")),
        "Adult" to MoodRule(listOf("Drama", "Romance", "Crime", "Thriller", "Biography"), listOf("mature", "provocative", "sensual", "complex", "gritty", "psychological")),
        "Wired" to MoodRule(listOf("Thriller", "Action", "Sci-Fi"), listOf("fast-paced", "high energy", "intense", "futuristic", "complex plot", "cyberpunk")),
        "Vengeful" to MoodRule(listOf("Action", "Crime", "Thriller", "Film-Noir"), listOf("retribution", "dark justice", "unrelenting", "payback", "gritty", "revenge", "neo-noir")),
        "Goofy" to MoodRule(listOf("Comedy", "Animation"), listOf("absurd", "silly", "zany", "nonsensical", "laugh out loud", "slapstick", "parody", "cult"), requirePlotEvidence = false),
        "Eerie" to MoodRule(listOf("Horror", "Thriller", "Mystery"), listOf("foreboding", "unsettling", "atmospheric", "suspenseful", "chilling", "gothic", "psychological")),
        "Empowered" to MoodRule(listOf("Biography", "Drama", "Action"), listOf("strong female lead", "triumph", "courage", "overcoming obstacles", "determination", "inspirational")),
        "Jaded" to MoodRule(listOf("Crime", "Thriller", "Comedy", "Film-Noir"), listOf("skeptical", "world-weary", "dark humor", "pessimistic", "anti-hero", "neo-noir", "cynical")),
        "Awestruck" to MoodRule(listOf("Sci-Fi", "Fantasy", "Documentary"), listOf("mind-blowing", "spectacular", "grand scale", "magical", "immersive", "epic", "nature")),
        "Challenged" to MoodRule(listOf("Thriller", "Mystery", "Drama"), listOf("puzzle", "cerebral", "complex", "thought-provoking", "decode", "psychological", "mind-bender")),
        "Giddy" to MoodRule(listOf("Comedy", "Romance", "Musical", "Animation"), listOf("euphoric", "bubbly", "hilarious", "excitable", "charming", "screwball", "lighthearted"), requirePlotEvidence = false, excludedGenres = setOf("Horror", "Thriller", "Crime", "War")),
        "Focused" to MoodRule(listOf("Documentary", "History", "Biography", "Drama"), listOf("detailed", "analytical", "uninterrupted", "fact-based", "in-depth", "intense")),
        "Escapist" to MoodRule(listOf("Fantasy", "Sci-Fi", "Adventure", "Musical"), listOf("otherworldly", "distraction", "pure imagination", "spectacle", "immersive world", "high adventure"), requirePlotEvidence = false),
        "Intrigued" to MoodRule(listOf("Mystery", "Thriller"), listOf("secrets", "unraveling", "hidden agenda", "investigation", "whodunit", "spy", "conspiracy", "period mystery")),
        "Tense" to MoodRule(listOf("Thriller", "Horror"), listOf("high stakes", "nerve-wracking", "tight spot", "pressure", "suspenseful buildup", "suspense", "survival", "psychological"))
    )

    private val KEYWORD_MOOD_MAP = mapOf(
        "Happy" to listOf("happy", "great", "good mood", "joy", "glad", "cheerful"),
        "Sad" to listOf("sad", "down", "blue", "crying", "lonely", "heartbroken"),
        "Angry" to listOf("angry", "mad", "furious", "annoyed", "pissed"),
        "Anxious" to listOf("anxious", "nervous", "stressed", "overwhelmed"),
        "Scared" to listOf("scared", "afraid", "terrified"),
        "Excited" to listOf("excited", "pumped", "thrilled", "hyped"),
        "Relaxed" to listOf("relaxed", "calm", "chill", "tired", "sleepy", "exhausted"),
        "Romantic" to listOf("love", "romantic", "crush", "in love"),
        "Nostalgic" to listOf("nostalgic", "miss the old days", "reminisc"),
        "Focused" to listOf("stressed", "overwhelmed", "long week", "burnt out")
    )

    fun guessMoodFromText(text: String): String {
        val lower = text.lowercase()
        MOODS.firstOrNull { it.label.equals(text.trim(), ignoreCase = true) || it.name.equals(text.trim(), ignoreCase = true) }?.let { return it.name }
        for ((mood, keywords) in KEYWORD_MOOD_MAP) {
            if (keywords.any { Regex("(?<![a-z])" + Regex.escape(it) + "(?![a-z])").containsMatchIn(lower) }) return mood
        }
        return "Happy"
    }

    suspend fun fetchMoodRecommendations(
        repository: MediaRepository,
        mood: String,
        type: String? = null
    ): List<RankedMovie> {
        val rule = MOOD_MAP[mood] ?: return emptyList()
        val curated = repository.curatedMoodIds(mood).toSet()
        val candidates = repository.moodCandidates(mood)
        return withContext(Dispatchers.Default) { candidates.filter { type == null || it.type == type }
            .mapNotNull { detail ->
                val score = com.nowshowing.tv.domain.recommendation.LocalRelevance.moodScore(detail, rule, detail.imdbID in curated)
                if (score == 0) null else RankedMovie(detail.imdbID, detail.title,
                    detail.poster?.takeUnless { it == "N/A" }.orEmpty(), detail.year,
                    detail.type ?: "movie", score, detail.imdbRating?.toFloatOrNull() ?: 0f)
            }.sortedWith(compareByDescending<RankedMovie> { it.matchScore }
                .thenByDescending { it.rating }.thenBy { it.imdbID })
        }
    }
}
