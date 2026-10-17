package com.nowshowing.tv.privacy

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object FilterListManager {

    private val updateMutex = Mutex()

    val engine = ContentBlockEngine()

    @Volatile
    private var isInitialized = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) { updateMutex.withLock {
        if (isInitialized) return@withLock

        val adRules = loadRules(context, "filters_adblock.txt")
        val trackerRules = loadRules(context, "filters_tracking.txt")

        engine.loadRules(adRules, trackerRules)
        isInitialized = true
    } }

    private fun loadRules(context: Context, fileName: String): List<String> {
        val rules = mutableListOf<String>()

        // 1. Check if an updated version exists in local storage
        val customFile = File(context.filesDir, "filters/$fileName")
        if (customFile.exists() || File(customFile.path + ".bak").exists()) {
            try {
                val text = AtomicFile(customFile).openRead().use { readBounded(it) }
                if (text != null && validFilterContent(text)) {
                    return text.lineSequence().map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("!") }.toList()
                }
            } catch (_: Exception) { /* Keep the bundled baseline. */ }
        }

        // 2. Fall back to bundled asset rules
        try {
            context.assets.open("filters/$fileName").use { stream ->
                BufferedReader(InputStreamReader(stream)).forEachLine { line ->
                    if (line.isNotBlank() && !line.startsWith("!")) {
                        rules.add(line)
                    }
                }
            }
        } catch (_: Exception) {}

        return rules
    }

    suspend fun updateRulesOnline(
        context: Context,
        adRulesUrl: String = "https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream-Android-TV/main/app/src/main/assets/filters/filters_adblock.txt",
        trackerRulesUrl: String = "https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream-Android-TV/main/app/src/main/assets/filters/filters_tracking.txt"
    ): FilterUpdateResult = withContext(Dispatchers.IO) { updateMutex.withLock {
        val filterDir = File(context.filesDir, "filters")
        if (!filterDir.isDirectory && !filterDir.mkdirs()) return@withLock FilterUpdateResult.FAILED
        fun update(url: String, name: String): Boolean {
            val text = fetchUrlContent(url) ?: return false
            if (!validFilterContent(text)) return false
            val destination = AtomicFile(File(filterDir, name))
            var stream: java.io.FileOutputStream? = null
            return try {
                stream = destination.startWrite()
                stream.write(text.toByteArray(Charsets.UTF_8))
                destination.finishWrite(stream)
                destination.openRead().use { readBounded(it) } == text
            } catch (_: Exception) {
                runCatching { destination.failWrite(stream) }
                false
            }
        }
        val ad = update(adRulesUrl, "filters_adblock.txt")
        val tracker = update(trackerRulesUrl, "filters_tracking.txt")
        if (ad || tracker) {
            engine.loadRules(loadRules(context, "filters_adblock.txt"), loadRules(context, "filters_tracking.txt"))
            isInitialized = true
        }
        when {
            ad && tracker -> FilterUpdateResult.COMPLETE
            ad || tracker -> FilterUpdateResult.PARTIAL
            else -> FilterUpdateResult.FAILED
        }
    } }

    private fun readBounded(stream: java.io.InputStream): String? {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() <= 512 * 1024) {
            val count = stream.read(buffer, 0, minOf(buffer.size, 512 * 1024 + 1 - output.size()))
            if (count < 0) break
            output.write(buffer, 0, count)
        }
        val bytes = output.toByteArray()
        return if (bytes.size > 512 * 1024) null else bytes.toString(Charsets.UTF_8)
    }

    private fun fetchUrlContent(url: String): String? = try {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.let { body ->
                // Enforce the cap while reading, including unknown Content-Length responses.
                body.byteStream().use { readBounded(it) }
            }
        }
    } catch (_: Exception) { null }
}
