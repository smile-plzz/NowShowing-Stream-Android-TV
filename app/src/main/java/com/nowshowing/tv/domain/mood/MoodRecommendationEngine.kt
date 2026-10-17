package com.nowshowing.tv.domain.mood

import com.nowshowing.tv.data.repository.MediaRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object MoodRecommendationEngine {

    val MOODS = listOf(
        MoodItem("Happy", "😊", "Happy vibes", "Light-hearted fun and feel-good stories."),
        MoodItem("Sad", "😢", "Emotional depth", "Films that let you feel it all, deeply."),
        MoodItem("Excited", "🤩", "Pure excitement", "High-energy films that get your pulse going."),
        MoodItem("Relaxed", "😌", "Calm & peaceful", "Gentle, easy films for winding down."),
        MoodItem("Angry", "😤", "Fueled by fire", "Intense stories that channel the rage."),
        MoodItem("Scared", "😱", "Edge-of-seat fear", "Horror and thrillers for the brave."),
        MoodItem("Inspired", "✨", "Inspired & lifted", "Films that spark something inside you."),
        MoodItem("Nostalgic", "🕰️", "Throwback feels", "Classic stories from eras gone by."),
        MoodItem("Curious", "🔍", "Curious mind", "Mysteries, puzzles and strange worlds."),
        MoodItem("Romantic", "💕", "Love in the air", "Stories of connection, longing, and love."),
        MoodItem("Adventurous", "🌍", "Adventure calls", "Epic journeys and far-flung worlds."),
        MoodItem("Thoughtful", "🤔", "Deep thoughts", "Films that leave you pondering life."),
        MoodItem("Anxious", "😰", "On edge", "Comforting, gentle picks to ease the feeling."),
        MoodItem("Hopeful", "🌅", "Full of hope", "Stories that restore your faith in everything."),
        MoodItem("Playful", "🎮", "Playful energy", "Fun, silly, and wonderfully light."),
        MoodItem("Adult", "🎭", "Mature themes", "Complex, grown-up cinema for serious viewing."),
        MoodItem("Wired", "⚡", "Wired & buzzed", "Fast-paced, kinetic, caffeine cinema."),
        MoodItem("Vengeful", "🗡️", "Revenge mode", "Cold, calculated payback stories."),
        MoodItem("Goofy", "🤪", "Pure goofiness", "Absurd comedy that makes no apologies."),
        MoodItem("Eerie", "👁️", "Unsettling vibes", "Slow-burn dread and atmospheric horror."),
        MoodItem("Empowered", "💪", "Feeling powerful", "Characters who rise, overcome, and win."),
        MoodItem("Jaded", "😒", "Seen it all", "Cynical, sharp, and darkly funny films."),
        MoodItem("Awestruck", "🤯", "Mind blown", "Visuals and ideas that defy imagination."),
        MoodItem("Challenged", "🧩", "Brain engaged", "Films that demand your full attention."),
        MoodItem("Giddy", "🥳", "Giddy and joyful", "Pure celebration and contagious happiness."),
        MoodItem("Focused", "🎯", "Laser focused", "Tight, precise, no-fluff storytelling."),
        MoodItem("Escapist", "🚀", "Escape hatch", "Worlds so good you forget this one."),
        MoodItem("Intrigued", "🕵️", "Deeply intrigued", "Slow reveals and layered mysteries."),
        MoodItem("Tense", "😬", "Tension maxed", "Nerve-shredding, palm-sweating cinema.")
    )

    val MOOD_MAP = mapOf(
        "Happy" to MoodRule(listOf("Comedy", "Musical", "Family", "Adventure"), listOf("feel good", "lighthearted", "heartwarming", "joyful", "uplifting")),
        "Sad" to MoodRule(listOf("Drama", "Romance"), listOf("poignant", "tearjerker", "bittersweet", "melancholy", "reflective", "indie", "slice of life")),
        "Excited" to MoodRule(listOf("Action", "Thriller", "Sci-Fi", "Sport"), listOf("high octane", "edge of your seat", "explosive", "pulse pounding", "adrenaline")),
        "Relaxed" to MoodRule(listOf("Documentary", "Romance", "Animation"), listOf("calming", "soothing", "gentle", "low stakes", "peaceful", "slice of life", "nature")),
        "Angry" to MoodRule(listOf("Action", "Crime", "Thriller"), listOf("vengeful", "raw", "intense", "dark", "confrontational", "revenge")),
        "Scared" to MoodRule(listOf("Horror", "Mystery", "Thriller"), listOf("spine chilling", "paranormal", "haunting", "tense", "nightmare", "supernatural")),
        "Inspired" to MoodRule(listOf("Biography", "Drama", "Musical", "Fantasy", "Sci-Fi", "Documentary"), listOf("motivational", "uplifting", "triumphant", "visionary", "aspirational")),
        "Nostalgic" to MoodRule(listOf("Romance", "Family", "History"), listOf("retro", "vintage", "memory lane", "throwback", "sentimental", "coming of age", "classic")),
        "Curious" to MoodRule(listOf("Mystery", "Documentary", "Sci-Fi", "Thriller"), listOf("investigative", "enigmatic", "mind bending", "clue", "sleuth", "psychological")),
        "Romantic" to MoodRule(listOf("Romance", "Comedy", "Musical", "Drama"), listOf("love story", "meet cute", "tender", "passionate", "swoon", "period romance")),
        "Adventurous" to MoodRule(listOf("Adventure", "Action", "Fantasy", "Sci-Fi"), listOf("journey", "quest", "uncharted", "bold", "sweeping", "epic")),
        "Thoughtful" to MoodRule(listOf("Drama", "Biography", "History"), listOf("introspective", "contemplative", "layered", "intellectual", "philosophical", "indie", "psychological")),
        "Anxious" to MoodRule(listOf("Comedy", "Animation", "Documentary", "Family"), listOf("comfort", "gentle", "uplifting", "wholesome", "calming", "feel good", "nature")),
        "Hopeful" to MoodRule(listOf("Drama", "Sport", "Family", "Musical", "Fantasy"), listOf("optimistic", "triumph", "feel good", "against the odds", "heartening", "inspirational")),
        "Playful" to MoodRule(listOf("Comedy", "Animation", "Family", "Musical", "Adventure"), listOf("whimsical", "imaginative", "silly", "joyful", "colorful")),
        "Adult" to MoodRule(listOf("Drama", "Romance", "Crime", "Thriller", "Biography"), listOf("mature", "provocative", "sensual", "complex", "gritty", "psychological")),
        "Wired" to MoodRule(listOf("Thriller", "Action", "Sci-Fi"), listOf("fast-paced", "high energy", "intense", "futuristic", "complex plot", "cyberpunk")),
        "Vengeful" to MoodRule(listOf("Action", "Crime", "Thriller", "Film-Noir"), listOf("retribution", "dark justice", "unrelenting", "payback", "gritty", "revenge", "neo-noir")),
        "Goofy" to MoodRule(listOf("Comedy", "Animation"), listOf("absurd", "silly", "zany", "nonsensical", "laugh out loud", "slapstick", "parody", "cult")),
        "Eerie" to MoodRule(listOf("Horror", "Thriller", "Mystery"), listOf("foreboding", "unsettling", "atmospheric", "suspenseful", "chilling", "gothic", "psychological")),
        "Empowered" to MoodRule(listOf("Biography", "Drama", "Action"), listOf("strong female lead", "triumph", "courage", "overcoming obstacles", "determination", "inspirational")),
        "Jaded" to MoodRule(listOf("Crime", "Thriller", "Comedy", "Film-Noir"), listOf("skeptical", "world-weary", "dark humor", "pessimistic", "anti-hero", "neo-noir", "cynical")),
        "Awestruck" to MoodRule(listOf("Sci-Fi", "Fantasy", "Documentary"), listOf("mind-blowing", "spectacular", "grand scale", "magical", "immersive", "epic", "nature")),
        "Challenged" to MoodRule(listOf("Thriller", "Mystery", "Drama"), listOf("puzzle", "cerebral", "complex", "thought-provoking", "decode", "psychological", "mind-bender")),
        "Giddy" to MoodRule(listOf("Comedy", "Romance", "Musical", "Animation"), listOf("euphoric", "bubbly", "hilarious", "excitable", "charming", "screwball", "lighthearted")),
        "Focused" to MoodRule(listOf("Documentary", "History", "Biography", "Drama"), listOf("detailed", "analytical", "uninterrupted", "fact-based", "in-depth", "intense")),
        "Escapist" to MoodRule(listOf("Fantasy", "Sci-Fi", "Adventure", "Musical"), listOf("otherworldly", "distraction", "pure imagination", "spectacle", "immersive world", "high adventure")),
        "Intrigued" to MoodRule(listOf("Mystery", "Thriller"), listOf("secrets", "unraveling", "hidden agenda", "investigation", "whodunit", "spy", "conspiracy", "period mystery")),
        "Tense" to MoodRule(listOf("Thriller", "Horror"), listOf("high stakes", "nerve-wracking", "tight spot", "pressure", "suspenseful buildup", "suspense", "survival", "psychological"))
    )

    private val KEYWORD_MOOD_MAP = mapOf(
        "Happy" to listOf("happy", "great", "good mood", "joy", "glad", "cheerful"),
        "Sad" to listOf("sad", "down", "blue", "crying", "lonely", "heartbroken"),
        "Angry" to listOf("angry", "mad", "furious", "annoyed", "pissed"),
        "Scared" to listOf("scared", "afraid", "anxious", "nervous", "terrified"),
        "Excited" to listOf("excited", "pumped", "thrilled", "hyped"),
        "Relaxed" to listOf("relaxed", "calm", "chill", "tired", "sleepy", "exhausted"),
        "Romantic" to listOf("love", "romantic", "crush", "in love"),
        "Nostalgic" to listOf("nostalgic", "miss the old days", "reminisc"),
        "Focused" to listOf("stressed", "overwhelmed", "long week", "burnt out")
    )

    fun guessMoodFromText(text: String): String {
        val lower = text.lowercase()
        for ((mood, keywords) in KEYWORD_MOOD_MAP) {
            if (keywords.any { lower.contains(it) }) return mood
        }
        return "Happy"
    }

    suspend fun fetchMoodRecommendations(
        repository: MediaRepository,
        mood: String,
        type: String? = null
    ): List<RankedMovie> = coroutineScope {
        val rule = MOOD_MAP[mood] ?: MoodRule(listOf(mood), listOf(mood))
        val termsPool = (rule.genres + rule.keywords).shuffled()
        val selectedTerms = if (termsPool.size >= 3) termsPool.take(3) else listOf(mood)

        // Step 1: Search titles across selected terms in parallel
        val searchDeferred = selectedTerms.map { term ->
            async { repository.search(term, 1, type) }
        }
        val searchResults = searchDeferred.awaitAll()

        // Step 2: Merge and deduplicate candidates by imdbID
        val candidatesMap = mutableMapOf<String, com.nowshowing.tv.data.model.OmdbTitleShort>()
        for (res in searchResults) {
            res.search?.forEach { item ->
                if (!candidatesMap.containsKey(item.imdbID)) {
                    candidatesMap[item.imdbID] = item
                }
            }
        }
        val candidates = candidatesMap.values.toList()
        if (candidates.isEmpty()) return@coroutineScope emptyList<RankedMovie>()

        // Step 3: Verify real genres and ratings for top candidates
        val targetGenres = rule.genres.map { it.lowercase() }
        val scoped = candidates.take(24)

        val detailDeferred = scoped.map { movie ->
            async { repository.getDetails(movie.imdbID) }
        }
        val details = detailDeferred.awaitAll()

        val rankedList = scoped.mapIndexed { idx, movie ->
            val detail = details[idx]
            val genreStr = (detail?.genre ?: "").lowercase()
            var matchScore = 0
            for (g in targetGenres) {
                if (genreStr.contains(g)) matchScore++
            }
            val rating = detail?.imdbRating?.toFloatOrNull() ?: 0f

            RankedMovie(
                imdbID = movie.imdbID,
                title = movie.title,
                poster = if (movie.poster != "N/A") movie.poster else "",
                year = movie.year,
                type = movie.type,
                matchScore = matchScore,
                rating = rating
            )
        }

        // Step 4: Rank by match score DESC, then rating DESC
        rankedList.sortedWith(
            compareByDescending<RankedMovie> { it.matchScore }
                .thenByDescending { it.rating }
        )
    }
}
