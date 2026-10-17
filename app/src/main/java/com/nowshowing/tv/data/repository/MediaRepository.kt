package com.nowshowing.tv.data.repository

import android.content.Context
import com.google.gson.Gson
import com.nowshowing.tv.data.api.ApiClient
import com.nowshowing.tv.data.db.ContinueItem
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.db.WatchlistItem
import com.nowshowing.tv.data.model.OmdbSearchResponse
import com.nowshowing.tv.data.model.OmdbSeasonResponse
import com.nowshowing.tv.data.model.OmdbTitleDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class GenreRowSeed(
    val id: String,
    val label: String,
    val seed: List<String>
)

class MediaRepository(private val context: Context) {

    private val api = ApiClient.service
    private val db = NowShowingDatabase.getInstance(context)
    private val gson = Gson()
    private val memoryCache = ConcurrentHashMap<String, Any>()

    companion object {
        val TRENDING_SEED = listOf(
            "Dune: Part Two", "Oppenheimer", "The Bear", "Fallout", "Shogun",
            "Godzilla x Kong", "Civil War", "Baby Reindeer", "Ripley", "3 Body Problem"
        )

        val GENRE_ROWS = listOf(
            GenreRowSeed(
                id = "action",
                label = "Action & Adventure",
                seed = listOf("Avengers", "Mission Impossible", "John Wick", "Mad Max", "Fast Five", "Extraction", "The Bourne", "Gladiator")
            ),
            GenreRowSeed(
                id = "scifi",
                label = "Sci-Fi & Fantasy",
                seed = listOf("Dune", "Interstellar", "The Matrix", "Blade Runner", "Arrival", "Star Wars", "Guardians of the Galaxy", "Edge of Tomorrow")
            ),
            GenreRowSeed(
                id = "drama",
                label = "Acclaimed Drama",
                seed = listOf("Oppenheimer", "Parasite", "The Godfather", "Forrest Gump", "Whiplash", "Moonlight", "The Green Mile", "12 Angry Men")
            ),
            GenreRowSeed(
                id = "comedy",
                label = "Comedy",
                seed = listOf("Superbad", "Barbie", "The Grand Budapest Hotel", "Knives Out", "Booksmart", "Deadpool", "Free Guy", "Game Night")
            ),
            GenreRowSeed(
                id = "thriller",
                label = "Thrillers",
                seed = listOf("Se7en", "Gone Girl", "Prisoners", "Shutter Island", "Knives Out", "Fight Club", "Zodiac", "Nightcrawler")
            )
        )

        val POPULAR_MOVIES = listOf(
            "Inception", "The Matrix", "Interstellar", "The Avengers", "Avatar",
            "Titanic", "Jurassic Park", "Forrest Gump", "The Lion King", "Gladiator",
            "Pulp Fiction", "Fight Club", "The Lord of the Rings", "Star Wars", "Dune",
            "Parasite", "The Dark Knight", "Oppenheimer", "Barbie", "Top Gun: Maverick",
            "Everything Everywhere All at Once", "Spider-Man: No Way Home",
            "Black Panther", "Joker", "Knives Out", "Get Out", "La La Land",
            "The Grand Budapest Hotel", "Mad Max: Fury Road", "Whiplash", "Arrival", "Coco"
        )

        val POPULAR_TV = listOf(
            "Breaking Bad", "Game of Thrones", "The Office", "Friends", "The Simpsons",
            "Stranger Things", "The Mandalorian", "The Crown", "Westworld", "Chernobyl",
            "The Witcher", "Black Mirror", "Succession", "The Last of Us", "House of the Dragon",
            "Squid Game", "Ozark", "Better Call Saul", "Ted Lasso", "Severance",
            "Andor", "The Bear", "White Lotus", "Peaky Blinders", "Dark", "Money Heist", "Fleabag"
        )
    }

    suspend fun search(query: String, page: Int = 1, type: String? = null): OmdbSearchResponse =
        withContext(Dispatchers.IO) {
            try {
                api.searchTitles(
                    apiKey = ApiClient.DEFAULT_OMDB_KEY,
                    query = query,
                    page = page,
                    type = if (type.isNullOrBlank() || type == "all") null else type
                )
            } catch (e: Exception) {
                OmdbSearchResponse(search = null, totalResults = "0", response = "False", error = e.localizedMessage)
            }
        }

    suspend fun getDetails(imdbId: String): OmdbTitleDetail? = withContext(Dispatchers.IO) {
        val cacheKey = "details_$imdbId"
        if (memoryCache.containsKey(cacheKey)) {
            return@withContext memoryCache[cacheKey] as? OmdbTitleDetail
        }

        // Check SQLite cache
        val cachedJson = db.getCachedPayload(cacheKey)
        if (cachedJson != null) {
            try {
                val detail = gson.fromJson(cachedJson, OmdbTitleDetail::class.java)
                memoryCache[cacheKey] = detail
                return@withContext detail
            } catch (_: Exception) {}
        }

        try {
            val response = api.getDetails(apiKey = ApiClient.DEFAULT_OMDB_KEY, imdbId = imdbId)
            if (response.response != "False") {
                memoryCache[cacheKey] = response
                db.setCachedPayload(cacheKey, gson.toJson(response))
                response
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getDetailsByTitle(title: String, type: String? = null): OmdbTitleDetail? =
        withContext(Dispatchers.IO) {
            val cacheKey = "title_${title}_${type ?: ""}"
            if (memoryCache.containsKey(cacheKey)) {
                return@withContext memoryCache[cacheKey] as? OmdbTitleDetail
            }

            val cachedJson = db.getCachedPayload(cacheKey)
            if (cachedJson != null) {
                try {
                    val detail = gson.fromJson(cachedJson, OmdbTitleDetail::class.java)
                    memoryCache[cacheKey] = detail
                    return@withContext detail
                } catch (_: Exception) {}
            }

            try {
                val response = api.getDetailsByTitle(
                    apiKey = ApiClient.DEFAULT_OMDB_KEY,
                    title = title,
                    type = if (type.isNullOrBlank() || type == "all") null else type
                )
                if (response.response != "False") {
                    memoryCache[cacheKey] = response
                    db.setCachedPayload(cacheKey, gson.toJson(response))
                    response
                } else null
            } catch (_: Exception) {
                null
            }
        }

    suspend fun lookupTitles(titles: List<String>, type: String? = null): List<OmdbTitleDetail> =
        coroutineScope {
            val deferred = titles.map { title ->
                async { getDetailsByTitle(title, type) }
            }
            deferred.awaitAll().filterNotNull()
        }

    suspend fun getSeason(imdbId: String, season: Int): OmdbSeasonResponse? = withContext(Dispatchers.IO) {
        val cacheKey = "season_${imdbId}_$season"
        val cachedJson = db.getCachedPayload(cacheKey)
        if (cachedJson != null) {
            try {
                return@withContext gson.fromJson(cachedJson, OmdbSeasonResponse::class.java)
            } catch (_: Exception) {}
        }

        try {
            val response = api.getSeason(
                apiKey = ApiClient.DEFAULT_OMDB_KEY,
                imdbId = imdbId,
                season = season
            )
            if (response.response != "False") {
                db.setCachedPayload(cacheKey, gson.toJson(response), ttlMs = 7 * 24 * 60 * 60 * 1000L)
                response
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // =========================================================================
    // Watchlist & History
    // =========================================================================

    suspend fun getWatchlist(): List<WatchlistItem> = withContext(Dispatchers.IO) {
        db.getWatchlist()
    }

    suspend fun isInWatchlist(imdbId: String): Boolean = withContext(Dispatchers.IO) {
        db.isInWatchlist(imdbId)
    }

    suspend fun toggleWatchlist(detail: OmdbTitleDetail): Boolean = withContext(Dispatchers.IO) {
        val inList = db.isInWatchlist(detail.imdbID)
        if (inList) {
            db.removeFromWatchlist(detail.imdbID)
            false
        } else {
            db.addToWatchlist(
                WatchlistItem(
                    imdbID = detail.imdbID,
                    title = detail.title,
                    poster = if (detail.poster != "N/A") detail.poster ?: "" else "",
                    year = detail.year,
                    type = detail.type ?: "movie"
                )
            )
            true
        }
    }

    suspend fun removeFromWatchlist(imdbId: String) = withContext(Dispatchers.IO) {
        db.removeFromWatchlist(imdbId)
    }

    suspend fun getContinueWatching(): List<ContinueItem> = withContext(Dispatchers.IO) {
        db.getContinueWatching()
    }

    suspend fun updateContinueWatching(
        imdbId: String,
        title: String,
        poster: String,
        year: String,
        type: String,
        season: Int = 1,
        episode: Int = 1,
        positionMs: Long = 0L,
        durationMs: Long = 0L
    ) = withContext(Dispatchers.IO) {
        db.upsertContinueWatching(
            ContinueItem(
                imdbID = imdbId,
                title = title,
                poster = poster,
                year = year,
                type = type,
                season = season,
                episode = episode,
                positionMs = positionMs,
                durationMs = durationMs
            )
        )
    }

    suspend fun removeFromContinueWatching(imdbId: String) = withContext(Dispatchers.IO) {
        db.removeFromContinueWatching(imdbId)
    }

    suspend fun getRecentSearches(): List<String> = withContext(Dispatchers.IO) {
        db.getRecentSearches()
    }

    suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        db.addRecentSearch(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        db.clearSearchHistory()
    }
}
