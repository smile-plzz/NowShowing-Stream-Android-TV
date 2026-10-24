package com.nowshowing.tv.ui.settings

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String = "",
    @SerializedName("name") val name: String? = null,
    @SerializedName("body") val body: String? = null,
    @SerializedName("html_url") val htmlUrl: String = "",
    @SerializedName("published_at") val publishedAt: String? = null
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubRelease, val currentVersion: String) : UpdateCheckResult
    data class UpToDate(val currentVersion: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object UpdateChecker {
    private const val RELEASES_API_URL =
        "https://api.github.com/repos/smile-plzz/NowShowing-Stream-Android-TV/releases/latest"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val gson = Gson()

    /**
     * Compares remote release tag with local version.
     * Returns true if remoteVersion is strictly newer than currentVersion.
     */
    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        if (cleanRemote.isBlank() || cleanCurrent.isBlank()) return false
        if (cleanRemote == cleanCurrent) return false

        // Extract numeric semver components e.g. "1.6.0" from "1.6.0-rc.4"
        val remoteBase = cleanRemote.split("-", "+")[0]
        val currentBase = cleanCurrent.split("-", "+")[0]

        val remoteParts = remoteBase.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentBase.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }

        // Base numbers are equal: check pre-release suffix (e.g. rc.2 vs rc.4 or release vs rc)
        val remotePre = if (cleanRemote.contains("-")) cleanRemote.substringAfter("-") else ""
        val currentPre = if (cleanCurrent.contains("-")) cleanCurrent.substringAfter("-") else ""

        // A stable release (empty pre-release) is newer than a release candidate with the same base
        if (currentPre.isNotBlank() && remotePre.isBlank()) return true
        if (currentPre.isBlank() && remotePre.isNotBlank()) return false

        // If both have pre-releases, compare lexicographically / by rc number
        if (currentPre.isNotBlank() && remotePre.isNotBlank()) {
            return remotePre > currentPre
        }

        return false
    }

    fun parseReleaseJson(json: String, currentVersion: String): UpdateCheckResult {
        return try {
            val release = gson.fromJson(json, GitHubRelease::class.java)
            if (release == null || release.tagName.isBlank()) {
                UpdateCheckResult.Error("Invalid release information returned.")
            } else if (isNewerVersion(release.tagName, currentVersion)) {
                UpdateCheckResult.UpdateAvailable(release, currentVersion)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error("Failed to parse release information.")
        }
    }

    suspend fun checkForUpdate(
        currentVersion: String,
        apiUrl: String = RELEASES_API_URL
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "NowShowing-TV")
                .build()

            val response = httpClient.newCall(request).execute()
            response.use { res ->
                if (res.code == 403 || res.code == 429) {
                    return@withContext UpdateCheckResult.Error(
                        "GitHub rate limit reached. Please try checking again in a few minutes."
                    )
                }
                if (!res.isSuccessful) {
                    return@withContext UpdateCheckResult.Error(
                        "Unable to check for updates (HTTP ${res.code})."
                    )
                }

                val bodyStr = res.body?.string().orEmpty()
                parseReleaseJson(bodyStr, currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(
                if (e is java.net.UnknownHostException || e is java.io.IOException) {
                    "Network error. Please check your internet connection."
                } else {
                    "Error checking for updates: ${e.message ?: "Unknown error"}"
                }
            )
        }
    }
}
