package com.nowshowing.tv.privacy

enum class FilterUpdateResult { COMPLETE, PARTIAL, FAILED }

internal fun validFilterContent(text: String): Boolean {
    if (text.toByteArray(Charsets.UTF_8).size > 512 * 1024 || '<' in text || '>' in text) return false
    val rules = text.lineSequence().map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("!") && !it.startsWith("[") }.toList()
    return rules.size >= 5 && rules.all {
        it.length <= 2048 && (it.startsWith("||") || it.startsWith("@@||") || it.startsWith("/") || it.startsWith("*"))
    }
}
