package com.nowshowing.tv.data.repository

import android.content.Context
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Small, data-only manifest. A bad update never replaces the last valid catalog. */
object CloudCatalog {
    const val URL = "https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream-Android-TV/main/catalog/catalog.json"
    private const val MAX_BYTES = 128 * 1024L
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private val cache = CatalogSnapshotCache { download() }

    fun parse(json: String): Map<String, List<String>> {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES)
        val root = JsonParser.parseString(json).asJsonObject
        require(root.get("version")?.toString() == "1") { "Unsupported catalog version" }
        val sections = root.getAsJsonObject("sections")
        require(sections.size() in 1..64)
        return sections.entrySet().associate { (name, value) ->
            require(name.matches(Regex("[A-Za-z_]+")))
            val array = value.asJsonArray
            require(array.size() in 1..100)
            name to array.map { entry ->
                require(entry.isJsonPrimitive && entry.asJsonPrimitive.isString)
                entry.asString.also { require(it.matches(Regex("tt[0-9]{7,10}"))) }
            }.distinct()
        }
    }

    suspend fun sections(context: Context): Map<String, List<String>> = cache.sections(context)

    private fun download(): String = client.newCall(Request.Builder().url(URL).build()).execute().use { response ->
        require(response.isSuccessful)
        val body = requireNotNull(response.body)
        require(body.contentLength() <= MAX_BYTES)
        val source = body.source()
        source.request(MAX_BYTES + 1)
        require(source.buffer.size <= MAX_BYTES)
        source.readUtf8()
    }
}

internal class CatalogSnapshotCache(
    private val clock: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val fetch: () -> String
) {
    private val lock = Mutex()
    private var checkedAt: Long? = null
    private var current: Map<String, List<String>>? = null

    suspend fun sections(context: Context): Map<String, List<String>> = withContext(Dispatchers.IO) {
        lock.withLock {
            val prefs = context.getSharedPreferences("cloud_catalog", Context.MODE_PRIVATE)
            if (current == null) current = runCatching { CloudCatalog.parse(prefs.getString("valid_json", "").orEmpty()) }.getOrNull()
            val now = clock()
            if (checkedAt?.let { now - it < 15 * 60_000L } == true) return@withLock current.orEmpty()
            checkedAt = now
            try {
                val json = fetch()
                val parsed = CloudCatalog.parse(json)
                prefs.edit().putString("valid_json", json).apply()
                current = parsed
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) { /* Offline or malformed: keep the validated snapshot. */ }
            current.orEmpty()
        }
    }
}
