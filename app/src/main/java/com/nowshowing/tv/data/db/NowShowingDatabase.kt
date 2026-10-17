package com.nowshowing.tv.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class WatchlistItem(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val dateAdded: Long = System.currentTimeMillis()
)

data class ContinueItem(
    val imdbID: String,
    val title: String,
    val poster: String,
    val year: String,
    val type: String,
    val season: Int = 1,
    val episode: Int = 1,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class NowShowingDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "nowshowing_tv.db"
        const val DATABASE_VERSION = 1

        private const val TABLE_WATCHLIST = "watchlist"
        private const val TABLE_CONTINUE = "continue_watching"
        private const val TABLE_CACHE = "metadata_cache"
        private const val TABLE_SEARCH_HISTORY = "search_history"

        @Volatile
        private var instance: NowShowingDatabase? = null

        fun getInstance(context: Context): NowShowingDatabase {
            return instance ?: synchronized(this) {
                instance ?: NowShowingDatabase(context.applicationContext).also { instance = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WATCHLIST (
                imdb_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                poster TEXT,
                year TEXT,
                type TEXT,
                date_added INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CONTINUE (
                imdb_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                poster TEXT,
                year TEXT,
                type TEXT,
                season INTEGER DEFAULT 1,
                episode INTEGER DEFAULT 1,
                position_ms INTEGER DEFAULT 0,
                duration_ms INTEGER DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHE (
                cache_key TEXT PRIMARY KEY,
                payload TEXT NOT NULL,
                expires_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SEARCH_HISTORY (
                query TEXT PRIMARY KEY,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WATCHLIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONTINUE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SEARCH_HISTORY")
        onCreate(db)
    }

    // =========================================================================
    // Watchlist Operations
    // =========================================================================

    fun getWatchlist(): List<WatchlistItem> {
        val list = mutableListOf<WatchlistItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_WATCHLIST ORDER BY date_added DESC", null)
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("imdb_id")
            val titleCol = it.getColumnIndexOrThrow("title")
            val posterCol = it.getColumnIndexOrThrow("poster")
            val yearCol = it.getColumnIndexOrThrow("year")
            val typeCol = it.getColumnIndexOrThrow("type")
            val dateCol = it.getColumnIndexOrThrow("date_added")

            while (it.moveToNext()) {
                list.add(
                    WatchlistItem(
                        imdbID = it.getString(idCol),
                        title = it.getString(titleCol),
                        poster = it.getString(posterCol) ?: "",
                        year = it.getString(yearCol) ?: "",
                        type = it.getString(typeCol) ?: "movie",
                        dateAdded = it.getLong(dateCol)
                    )
                )
            }
        }
        return list
    }

    fun isInWatchlist(imdbID: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_WATCHLIST WHERE imdb_id = ? LIMIT 1", arrayOf(imdbID))
        cursor.use { return it.moveToFirst() }
    }

    fun addToWatchlist(item: WatchlistItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("imdb_id", item.imdbID)
            put("title", item.title)
            put("poster", item.poster)
            put("year", item.year)
            put("type", item.type)
            put("date_added", item.dateAdded)
        }
        db.insertWithOnConflict(TABLE_WATCHLIST, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFromWatchlist(imdbID: String) {
        val db = writableDatabase
        db.delete(TABLE_WATCHLIST, "imdb_id = ?", arrayOf(imdbID))
    }

    // =========================================================================
    // Continue Watching Operations
    // =========================================================================

    fun getContinueWatching(): List<ContinueItem> {
        val list = mutableListOf<ContinueItem>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CONTINUE ORDER BY updated_at DESC LIMIT 20", null)
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("imdb_id")
            val titleCol = it.getColumnIndexOrThrow("title")
            val posterCol = it.getColumnIndexOrThrow("poster")
            val yearCol = it.getColumnIndexOrThrow("year")
            val typeCol = it.getColumnIndexOrThrow("type")
            val seasonCol = it.getColumnIndexOrThrow("season")
            val epCol = it.getColumnIndexOrThrow("episode")
            val posCol = it.getColumnIndexOrThrow("position_ms")
            val durCol = it.getColumnIndexOrThrow("duration_ms")
            val updatedCol = it.getColumnIndexOrThrow("updated_at")

            while (it.moveToNext()) {
                list.add(
                    ContinueItem(
                        imdbID = it.getString(idCol),
                        title = it.getString(titleCol),
                        poster = it.getString(posterCol) ?: "",
                        year = it.getString(yearCol) ?: "",
                        type = it.getString(typeCol) ?: "movie",
                        season = it.getInt(seasonCol),
                        episode = it.getInt(epCol),
                        positionMs = it.getLong(posCol),
                        durationMs = it.getLong(durCol),
                        updatedAt = it.getLong(updatedCol)
                    )
                )
            }
        }
        return list
    }

    fun upsertContinueWatching(item: ContinueItem) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("imdb_id", item.imdbID)
            put("title", item.title)
            put("poster", item.poster)
            put("year", item.year)
            put("type", item.type)
            put("season", item.season)
            put("episode", item.episode)
            put("position_ms", item.positionMs)
            put("duration_ms", item.durationMs)
            put("updated_at", item.updatedAt)
        }
        db.insertWithOnConflict(TABLE_CONTINUE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeFromContinueWatching(imdbID: String) {
        val db = writableDatabase
        db.delete(TABLE_CONTINUE, "imdb_id = ?", arrayOf(imdbID))
    }

    // =========================================================================
    // Search History Operations
    // =========================================================================

    fun getRecentSearches(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT query FROM $TABLE_SEARCH_HISTORY ORDER BY timestamp DESC LIMIT 8", null)
        cursor.use {
            val col = it.getColumnIndexOrThrow("query")
            while (it.moveToNext()) {
                list.add(it.getString(col))
            }
        }
        return list
    }

    fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val db = writableDatabase
        val values = ContentValues().apply {
            put("query", clean)
            put("timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_SEARCH_HISTORY, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearSearchHistory() {
        val db = writableDatabase
        db.delete(TABLE_SEARCH_HISTORY, null, null)
    }

    // =========================================================================
    // Metadata Cache
    // =========================================================================

    fun getCachedPayload(key: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT payload FROM $TABLE_CACHE WHERE cache_key = ? AND expires_at > ? LIMIT 1",
            arrayOf(key, System.currentTimeMillis().toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow("payload"))
            }
        }
        return null
    }

    fun setCachedPayload(key: String, payload: String, ttlMs: Long = 24 * 60 * 60 * 1000L) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("cache_key", key)
            put("payload", payload)
            put("expires_at", System.currentTimeMillis() + ttlMs)
        }
        db.insertWithOnConflict(TABLE_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
