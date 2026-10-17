package com.nowshowing.tv.privacy

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object FilterListManager {

    val engine = ContentBlockEngine()

    @Volatile
    private var isInitialized = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        val adRules = loadRules(context, "filters_adblock.txt")
        val trackerRules = loadRules(context, "filters_tracking.txt")

        engine.loadRules(adRules, trackerRules)
        isInitialized = true
    }

    private fun loadRules(context: Context, fileName: String): List<String> {
        val rules = mutableListOf<String>()

        // 1. Check if an updated version exists in local storage
        val customFile = File(context.filesDir, "filters/$fileName")
        if (customFile.exists() && customFile.length() > 0) {
            try {
                customFile.forEachLine { line ->
                    if (line.isNotBlank() && !line.startsWith("!")) {
                        rules.add(line)
                    }
                }
                if (rules.isNotEmpty()) {
                    return rules
                }
            } catch (_: Exception) {}
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
        adRulesUrl: String = "https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream/main/android-tv/app/src/main/assets/filters/filters_adblock.txt",
        trackerRulesUrl: String = "https://raw.githubusercontent.com/smile-plzz/NowShowing-Stream/main/android-tv/app/src/main/assets/filters/filters_tracking.txt"
    ): Boolean = withContext(Dispatchers.IO) {
        val filterDir = File(context.filesDir, "filters").apply { mkdirs() }

        val newAdRules = fetchUrlContent(adRulesUrl)
        val newTrackerRules = fetchUrlContent(trackerRulesUrl)

        var success = false

        if (!newAdRules.isNullOrBlank() && newAdRules.lines().size > 10) {
            val tempFile = File(filterDir, "filters_adblock.txt.tmp")
            tempFile.writeText(newAdRules)
            val destFile = File(filterDir, "filters_adblock.txt")
            tempFile.renameTo(destFile)
            success = true
        }

        if (!newTrackerRules.isNullOrBlank() && newTrackerRules.lines().size > 5) {
            val tempFile = File(filterDir, "filters_tracking.txt.tmp")
            tempFile.writeText(newTrackerRules)
            val destFile = File(filterDir, "filters_tracking.txt")
            tempFile.renameTo(destFile)
            success = true
        }

        if (success) {
            isInitialized = false
            initialize(context)
        }

        return@withContext success
    }

    private fun fetchUrlContent(url: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
