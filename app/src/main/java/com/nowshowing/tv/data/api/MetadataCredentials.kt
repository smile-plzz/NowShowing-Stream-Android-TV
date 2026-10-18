package com.nowshowing.tv.data.api

/** Ordered, bounded local credentials. Never include credentials in errors or diagnostics. */
object MetadataCredentials {
    const val MAX_KEYS = 5

    fun parse(value: String): List<String>? {
        if (value.isBlank()) return emptyList()
        val keys = value.trim().split(Regex("[,;\\s]+"))
        if (keys.any { !it.matches(Regex("[A-Za-z0-9]{6,64}")) }) return null
        return keys.distinct().takeIf { it.size <= MAX_KEYS }
    }
}

/** Retains rejected credentials for this configuration/session, avoiding repeated failed requests. */
class MetadataKeySelection {
    private var generation = -1L
    private var configuration: List<String> = emptyList()
    private val rejected = mutableSetOf<String>()

    @Synchronized fun next(keys: List<String>, session: Long): String? {
        if (session < generation) return null
        resetIfChanged(keys, session)
        return keys.firstOrNull { it !in rejected }
    }

    @Synchronized fun reject(key: String, keys: List<String>, session: Long) {
        // An older in-flight response must not overwrite a newer configuration.
        if (session == generation && keys == configuration) rejected.add(key)
    }

    private fun resetIfChanged(keys: List<String>, session: Long) {
        if (session > generation || (session == generation && keys != configuration)) {
            generation = session
            configuration = keys.toList()
            rejected.clear()
        }
    }
}
