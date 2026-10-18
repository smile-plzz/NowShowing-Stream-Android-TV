package com.nowshowing.tv.data.repository

/** Bounded access-order cache with expiry; clock injection keeps tests deterministic. */
internal class MetadataCache<T>(
    private val capacity: Int = 128,
    private val ttlMs: Long = 24 * 60 * 60 * 1000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Entry<T>(val value: T, val expiresAt: Long)
    private val entries = LinkedHashMap<String, Entry<T>>(16, 0.75f, true)

    @Synchronized fun clear() { entries.clear() }

    @Synchronized fun get(key: String): T? {
        val entry = entries[key] ?: return null
        if (entry.expiresAt <= now()) { entries.remove(key); return null }
        return entry.value
    }
    @Synchronized fun put(key: String, value: T) {
        entries[key] = Entry(value, now() + ttlMs)
        while (entries.size > capacity.coerceAtLeast(1)) {
            val iterator = entries.entries.iterator()
            iterator.next()
            iterator.remove()
        }
    }
}
