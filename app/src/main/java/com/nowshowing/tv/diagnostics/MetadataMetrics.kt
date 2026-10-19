package com.nowshowing.tv.diagnostics

import java.util.concurrent.atomic.AtomicLong

/** Process counters only. Never retain queries, identifiers, credentials or URLs. */
object MetadataMetrics {
    val requests = AtomicLong()
    val memoryHits = AtomicLong()
    val diskHits = AtomicLong()
    val staleHits = AtomicLong()
    @Volatile private var lastSuccess: Pair<Long, Long>? = null
    fun success(latencyMs: Long) { lastSuccess = System.currentTimeMillis() to latencyMs }
    fun summary(): String {
        val success = lastSuccess
        return "Process network attempts: ${requests.get()}\nDetail memory hits: ${memoryHits.get()}\nDetail disk hits: ${diskHits.get()}\nStale fallback uses: ${staleHits.get()}\n" +
            (success?.let { "Last measured network success: ${java.time.Instant.ofEpochMilli(it.first)}\nRequest latency: ${it.second} ms" }
                ?: "Last measured network success: unavailable") +
            "\nCounters reset on process restart. Cache hits are not connectivity tests."
    }
}
