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
    private val lock = Mutex()
    private val client = OkHttpClient.Builder().callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private var checkedAt = 0L
    private var current: Map<String, List<String>>? = null

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

    suspend fun sections(context: Context): Map<String, List<String>> = withContext(Dispatchers.IO) {
        lock.withLock {
            val prefs = context.getSharedPreferences("cloud_catalog", Context.MODE_PRIVATE)
            if (current == null) current = runCatching { parse(prefs.getString("valid_json", "")!!) }.getOrNull()
            val now = android.os.SystemClock.elapsedRealtime()
            if (checkedAt != 0L && now - checkedAt < 15 * 60_000L) return@withLock current.orEmpty()
            checkedAt = now
            try {
                client.newCall(Request.Builder().url(URL).build()).execute().use { response ->
                    require(response.isSuccessful)
                    val body = requireNotNull(response.body)
                    require(body.contentLength() <= MAX_BYTES)
                    val source = body.source()
                    source.request(MAX_BYTES + 1)
                    require(source.buffer.size <= MAX_BYTES)
                    val json = source.readUtf8()
                    val parsed = parse(json)
                    prefs.edit().putString("valid_json", json).apply()
                    current = parsed
                }
            } catch (_: Exception) { /* Offline, oversized or malformed: keep the validated snapshot. */ }
            current.orEmpty()
        }
    }
}
