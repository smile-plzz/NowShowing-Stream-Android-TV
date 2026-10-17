package com.nowshowing.tv.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MetadataFailure(val message: String) {
    QUOTA("Metadata quota reached. Cached titles remain available. Retry later or configure your own OMDb key in Settings."),
    KEY("Metadata API key is invalid or inactive. Configure an OMDb key in Settings, then retry."),
    NETWORK("Metadata connection failed. Check the TV's internet connection, then retry."),
    SERVICE("Metadata service is unavailable. Please retry later."),
    NO_RESULTS("No matching titles found.")
}

fun classifyMetadataError(error: String?): MetadataFailure = when {
    error.orEmpty().contains("limit", true) || error.orEmpty().contains("quota", true) -> MetadataFailure.QUOTA
    error.orEmpty().contains("key", true) -> MetadataFailure.KEY
    error.orEmpty().contains("not found", true) || error.orEmpty().contains("no results", true) -> MetadataFailure.NO_RESULTS
    else -> MetadataFailure.SERVICE
}

class MetadataUnavailable(val failure: MetadataFailure) : Exception(failure.message)

/** Process-wide bounded circuit breaker. Only safe categories are retained, never URLs or keys. */
object MetadataHealth {
    private val mutable = MutableStateFlow<MetadataFailure?>(null)
    val failure = mutable.asStateFlow()
    private var blockedUntil = 0L
    private val errors = ArrayDeque<String>()
    @Volatile var usedStaleCache = false
        private set

    @Synchronized fun check(now: Long = System.currentTimeMillis()) {
        if (now < blockedUntil) throw MetadataUnavailable(mutable.value ?: MetadataFailure.SERVICE)
    }
    @Synchronized fun record(failure: MetadataFailure, now: Long = System.currentTimeMillis()) {
        if (failure == MetadataFailure.NO_RESULTS) return
        // Successful or unrelated responses cannot erase an established credential failure.
        if (mutable.value in listOf(MetadataFailure.KEY, MetadataFailure.QUOTA) && now < blockedUntil) return
        mutable.value = failure
        blockedUntil = now + if (failure == MetadataFailure.KEY || failure == MetadataFailure.QUOTA) 900_000L else 10_000L
        errors.addLast("$now: ${failure.name}")
        while (errors.size > 12) errors.removeFirst()
    }
    @Synchronized fun success(now: Long = System.currentTimeMillis()) {
        if (now >= blockedUntil) mutable.value = null
    }
    @Synchronized fun retry() { blockedUntil = 0; mutable.value = null; usedStaleCache = false }
    @Synchronized fun stale() { usedStaleCache = true }
    @Synchronized fun recentErrors(): List<String> = errors.toList()
}
