package com.nowshowing.tv.data.repository

import android.content.Context
import com.nowshowing.tv.diagnostics.MetadataMetrics
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.nowshowing.tv.data.db.RecentItem

data class GenreRowSeed(
    val id: String,
    val label: String,
    val seed: List<String>
)

class MediaRepository(
    private val context: Context,
    private val api: com.nowshowing.tv.data.api.OmdbApiService = ApiClient.service,
    private val db: NowShowingDatabase = NowShowingDatabase.getInstance(context)
) {
    private val gson = Gson()

    companion object {
        private val memoryCache = MetadataCache<OmdbTitleDetail>()
        private val requestLocks = Array(64) { Mutex() }
        private val networkSlots = Semaphore(2)
        private val keySelection = com.nowshowing.tv.data.api.MetadataKeySelection()

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

    private suspend fun <T> request(fetch: suspend (String) -> T, error: (T) -> String?): T =
        networkSlots.withPermit {
            MetadataHealth.check()
            val generation = MetadataHealth.generation
            val keys = ApiClient.metadataKeys(context)
            // Only a rejected/invalid credential permits failover. Quota, network, service and
            // no-results responses do not become a reason to cycle credentials.
            repeat(keys.size) {
                val key = keySelection.next(keys, generation)
                    ?: run {
                        if (MetadataHealth.generation == generation) MetadataHealth.record(MetadataFailure.KEY)
                        throw MetadataUnavailable(MetadataFailure.KEY)
                    }
                try {
                    MetadataMetrics.requests.incrementAndGet()
                    val started = android.os.SystemClock.elapsedRealtime()
                    val result = fetch(key)
                    error(result)?.let { throw MetadataUnavailable(classifyMetadataError(it)) }
                    MetadataMetrics.success(android.os.SystemClock.elapsedRealtime() - started)
                    if (MetadataHealth.generation == generation) MetadataHealth.success()
                    return@withPermit result
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (e: Exception) {
                    val failure = when {
                        e is MetadataUnavailable -> e.failure
                        e is java.io.IOException -> MetadataFailure.NETWORK
                        e is retrofit2.HttpException && e.code() == 401 -> MetadataFailure.KEY
                        e is retrofit2.HttpException && e.code() == 429 -> MetadataFailure.QUOTA
                        else -> MetadataFailure.SERVICE
                    }
                    if (failure == MetadataFailure.KEY && MetadataHealth.generation == generation) {
                        keySelection.reject(key, keys, generation)
                    } else {
                        if (MetadataHealth.generation == generation) MetadataHealth.record(failure)
                        throw MetadataUnavailable(failure)
                    }
                }
            }
            if (MetadataHealth.generation == generation) MetadataHealth.record(MetadataFailure.KEY)
            throw MetadataUnavailable(MetadataFailure.KEY)
        }

    suspend fun search(query: String, page: Int = 1, type: String? = null, year: Int? = null): OmdbSearchResponse =
        withContext(Dispatchers.IO) {
            val cacheKey = "search_${query.trim().lowercase()}_${page}_${type.orEmpty()}" + (year?.let { "_year_$it" } ?: "")
            requestLocks[(cacheKey.hashCode() and Int.MAX_VALUE) % requestLocks.size].withLock {
                fun cached(stale: Boolean): OmdbSearchResponse? = db.getCachedPayload(cacheKey, stale)?.let {
                    runCatching { gson.fromJson(it, OmdbSearchResponse::class.java) }.getOrNull()
                }
                cached(false)?.let { return@withContext it }
                try {
                    val response = request({ key -> api.searchTitles(
                        apiKey = key, query = query, page = page,
                        type = type?.takeUnless { it.isBlank() || it == "all" }, year = year
                    ) }) { if (it.response == "False") it.error ?: "Service error" else null }
                    db.setCachedPayload(cacheKey, gson.toJson(response), 60 * 60 * 1000L)
                    response
                } catch (failure: MetadataUnavailable) {
                    if (failure.failure != MetadataFailure.NO_RESULTS) cached(true)?.let {
                        MetadataMetrics.staleHits.incrementAndGet(); MetadataHealth.stale(); return@withContext it.copy(isStale = true)
                    }
                    OmdbSearchResponse(null, "0", "False", failure.failure.message)
                }
            }
        }

    private suspend fun cachedDetail(key: String, fetch: suspend (String) -> OmdbTitleDetail): OmdbTitleDetail? =
        withContext(Dispatchers.IO) {
            // Striped locks bound synchronization memory and coalesce matching concurrent lookups.
            requestLocks[(key.hashCode() and Int.MAX_VALUE) % requestLocks.size].withLock {
                memoryCache.get(key)?.let { MetadataMetrics.memoryHits.incrementAndGet(); return@withContext it }
                db.getCachedPayload(key)?.let { json ->
                    try {
                        val detail = gson.fromJson(json, OmdbTitleDetail::class.java)
                        if (detail != null && detail.response != "False") {
                            MetadataMetrics.diskHits.incrementAndGet()
                            // Disk expiry stays authoritative; do not extend it in the memory tier.
                            return@withContext detail
                        }
                    } catch (_: Exception) { /* Invalid cached JSON: refetch. */ }
                }
                try {
                    val detail = request(fetch) { if (it.response == "False") it.error ?: "Service error" else null }
                    if (detail.response == "False") return@withContext null
                    db.setCachedPayload(key, gson.toJson(detail))
                    memoryCache.put(key, detail)
                    if (detail.imdbID.isNotBlank()) {
                        val idKey = "details_${detail.imdbID}"
                        db.setCachedPayload(idKey, gson.toJson(detail))
                        memoryCache.put(idKey, detail)
                    }
                    detail
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: MetadataUnavailable) {
                    if (failure.failure == MetadataFailure.NO_RESULTS) return@withContext null
                    db.getCachedPayload(key, allowStale = true)?.let { json ->
                        runCatching { gson.fromJson(json, OmdbTitleDetail::class.java) }.getOrNull()?.let {
                            MetadataMetrics.staleHits.incrementAndGet(); MetadataHealth.stale(); return@withContext it
                        }
                    }
                    null
                }
            }
        }

    suspend fun getDetails(imdbId: String): OmdbTitleDetail? = cachedDetail("details_$imdbId") { key ->
        api.getDetails(apiKey = key, imdbId = imdbId)
    }

    suspend fun getDetailsByTitle(title: String, type: String? = null): OmdbTitleDetail? {
        val normalizedType = type?.takeUnless { it.isBlank() || it == "all" }
        // Keep v1 disk-cache keys so upgrading does not discard warm metadata.
        val key = "title_${title.trim()}_${normalizedType ?: ""}"
        return cachedDetail(key) { credential ->
            api.getDetailsByTitle(credential, title.trim(), normalizedType)
        }
    }

    suspend fun lookupTitles(titles: List<String>, type: String? = null): List<OmdbTitleDetail> =
        coroutineScope {
            val deferred = titles.distinct().map { title ->
                async { getDetailsByTitle(title, type) }
            }
            deferred.awaitAll().filterNotNull().distinctBy { it.imdbID }
        }

    /** Publish warm cache first without network; then bounded batches in original seed order. */
    suspend fun lookupTitlesProgressively(
        titles: List<String>, type: String? = null,
        publish: suspend (List<OmdbTitleDetail>) -> Unit
    ): List<OmdbTitleDetail> {
        val seeds = titles.distinct()
        val resolved = linkedMapOf<String, OmdbTitleDetail>()
        withContext(Dispatchers.IO) {
            seeds.forEach { title ->
                val key = "title_${title.trim()}_${type?.takeUnless { it.isBlank() || it == "all" }.orEmpty()}"
                val detail = memoryCache.get(key) ?: db.getCachedPayload(key)?.let {
                    runCatching { gson.fromJson(it, OmdbTitleDetail::class.java) }.getOrNull()
                }
                if (detail != null && detail.response != "False") resolved[title] = detail
            }
        }
        fun snapshot() = seeds.mapNotNull { resolved[it] }.distinctBy { it.imdbID }
        if (resolved.isNotEmpty()) publish(snapshot())
        seeds.filterNot { it in resolved }.chunked(2).forEach { batch ->
            val details = coroutineScope { batch.map { title -> async { title to getDetailsByTitle(title, type) } }.awaitAll() }
            details.forEach { (title, detail) -> if (detail != null) resolved[title] = detail }
            if (resolved.isNotEmpty()) publish(snapshot())
        }
        return snapshot()
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
            requestLocks[(cacheKey.hashCode() and Int.MAX_VALUE) % requestLocks.size].withLock {
                db.getCachedPayload(cacheKey)?.let {
                    runCatching { gson.fromJson(it, OmdbSeasonResponse::class.java) }.getOrNull()?.let { return@withContext it }
                }
                val response = request({ key -> api.getSeason(key, imdbId, season) }) {
                    if (it.response == "False") it.error ?: "Service error" else null
                }
                db.setCachedPayload(cacheKey, gson.toJson(response), 7 * 24 * 60 * 60 * 1000L)
                response
            }
        } catch (cancelled: CancellationException) { throw cancelled
        } catch (failure: MetadataUnavailable) {
            if (failure.failure == MetadataFailure.NO_RESULTS) return@withContext null
            db.getCachedPayload(cacheKey, true)?.let {
                runCatching { gson.fromJson(it, OmdbSeasonResponse::class.java) }.getOrNull()?.also { MetadataHealth.stale() }
            }
        }
    }

    suspend fun relatedTitles(detail: OmdbTitleDetail): List<OmdbTitleDetail> = withContext(Dispatchers.IO) {
        val genres = detail.genre.orEmpty().split(",").map { it.trim() }.filter { it.isNotBlank() && it != "N/A" }.toSet()
        db.cachedDetailPayloads().mapNotNull {
            runCatching { gson.fromJson(it, OmdbTitleDetail::class.java) }.getOrNull()
        }.distinctBy { it.imdbID }.filter {
            it.imdbID != detail.imdbID && it.type == detail.type &&
                it.genre.orEmpty().split(",").any { genre -> genre.trim() in genres }
        }.take(6)
    }

    suspend fun clearMetadataCache() = withContext(Dispatchers.IO) {
        // Serialize against every repository read/write so cleared entries cannot be repopulated by in-flight requests.
        suspend fun clearAt(index: Int) {
            if (index == requestLocks.size) { db.clearMetadata(); memoryCache.clear() }
            else requestLocks[index].withLock { clearAt(index + 1) }
        }
        clearAt(0)
    }

    // =========================================================================
    // Watchlist & History
    // =========================================================================

    suspend fun getRecentlyOpened(): List<RecentItem> = withContext(Dispatchers.IO) { db.getRecentlyOpened() }

    suspend fun recordRecentlyOpened(item: RecentItem) = withContext(Dispatchers.IO) { db.upsertRecentlyOpened(item) }

    suspend fun removeRecentlyOpened(imdbId: String) = withContext(Dispatchers.IO) { db.removeRecentlyOpened(imdbId) }

    suspend fun setRecentlyWatched(imdbId: String, watched: Boolean) =
        withContext(Dispatchers.IO) { db.setRecentlyWatched(imdbId, watched) }

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
