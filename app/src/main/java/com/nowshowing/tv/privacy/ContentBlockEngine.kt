package com.nowshowing.tv.privacy

import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.regex.Pattern

enum class BlockType {
    AD,
    TRACKER,
    POPUP
}

data class BlockDecision(
    val isBlocked: Boolean,
    val type: BlockType? = null,
    val matchedRule: String? = null
)

class ContentBlockEngine {

    private val exactBlockedDomains = mutableSetOf<String>()
    private val suffixBlockedDomains = mutableSetOf<String>()
    private val pathPatterns = mutableListOf<Pattern>()
    private val keywordPatterns = mutableListOf<String>()
    private val whitelistDomains = mutableSetOf<String>()
    private val trackerDomains = mutableSetOf<String>()

    // Bounded LRU Cache for sub-millisecond decision lookups on Android TV
    private val decisionCache: MutableMap<String, BlockDecision> = Collections.synchronizedMap(
        object : LinkedHashMap<String, BlockDecision>(500, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BlockDecision>?): Boolean {
                return size > 1000
            }
        }
    )

    @Synchronized
    fun loadRules(adRules: List<String>, trackingRules: List<String>) {
        exactBlockedDomains.clear()
        suffixBlockedDomains.clear()
        pathPatterns.clear()
        keywordPatterns.clear()
        whitelistDomains.clear()
        trackerDomains.clear()
        decisionCache.clear()

        parseRules(adRules, isTracker = false)
        parseRules(trackingRules, isTracker = true)
    }

    private fun parseRules(rules: List<String>, isTracker: Boolean) {
        for (raw in rules) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) {
                continue
            }

            // Whitelist exception: @@||domain^ or @@domain
            if (line.startsWith("@@")) {
                val clean = line.removePrefix("@@")
                    .removePrefix("||")
                    .removeSuffix("^")
                    .lowercase(Locale.ROOT)
                if (clean.isNotEmpty()) {
                    whitelistDomains.add(clean)
                }
                continue
            }

            // Domain anchor: ||domain.com^
            if (line.startsWith("||")) {
                val domain = line.removePrefix("||")
                    .removeSuffix("^")
                    .lowercase(Locale.ROOT)
                if (domain.isNotEmpty()) {
                    suffixBlockedDomains.add(domain)
                    if (isTracker) {
                        trackerDomains.add(domain)
                    }
                }
                continue
            }

            // Keyword rule: e.g. *popunder* or *user_tracker*
            if (line.startsWith("*") && line.endsWith("*") && line.length > 2) {
                val kw = line.substring(1, line.length - 1).lowercase(Locale.ROOT)
                keywordPatterns.add(kw)
                continue
            }

            // Path pattern rule: e.g. /ad/banner/*
            if (line.contains("/") || line.contains("*")) {
                val clean = line.trim('*').lowercase(Locale.ROOT)
                if (clean.isNotEmpty()) {
                    keywordPatterns.add(clean)
                }
                try {
                    val regex = ".*" + line
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .replace("^", "([/?&]|$).*") + ".*"
                    val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
                    pathPatterns.add(pattern)
                } catch (_: Exception) {}
                continue
            }

            // Plain domain
            val plain = line.lowercase(Locale.ROOT)
            exactBlockedDomains.add(plain)
            if (isTracker) {
                trackerDomains.add(plain)
            }
        }
    }

    @Synchronized
    fun evaluate(
        url: String,
        pageUrl: String? = null,
        isAdBlockEnabled: Boolean = true,
        isTrackerBlockEnabled: Boolean = true
    ): BlockDecision {
        if (!isAdBlockEnabled && !isTrackerBlockEnabled) {
            return BlockDecision(isBlocked = false)
        }

        val cacheKey = "$isAdBlockEnabled:$isTrackerBlockEnabled:$url"
        // Cache hit check
        decisionCache[cacheKey]?.let { return it }

        val lowerUrl = url.lowercase(Locale.ROOT)
        val host = extractHost(url)
        val path = extractPath(url)

        // 1. Whitelist Check
        for (w in whitelistDomains) {
            if (host == w || host.endsWith(".$w")) {
                val allowed = BlockDecision(isBlocked = false)
                decisionCache[cacheKey] = allowed
                return allowed
            }
        }

        // 3. Domain Check (Exact & Subdomain suffix)
        val matchedDomain = findMatchingDomain(host)
        if (matchedDomain != null) {
            val isTracker = trackerDomains.contains(matchedDomain)
            if (isTracker && !isTrackerBlockEnabled) {
                val allowed = BlockDecision(isBlocked = false)
                decisionCache[cacheKey] = allowed
                return allowed
            }
            if (!isTracker && !isAdBlockEnabled) {
                val allowed = BlockDecision(isBlocked = false)
                decisionCache[cacheKey] = allowed
                return allowed
            }

            val decision = BlockDecision(
                isBlocked = true,
                type = if (isTracker) BlockType.TRACKER else BlockType.AD,
                matchedRule = "||$matchedDomain^"
            )
            decisionCache[cacheKey] = decision
            return decision
        }

        // 4. Keyword & Path Pattern Check
        if (isAdBlockEnabled) {
            for (kw in keywordPatterns) {
                if (lowerUrl.contains(kw) || path.contains(kw)) {
                    val decision = BlockDecision(
                        isBlocked = true,
                        type = BlockType.AD,
                        matchedRule = kw
                    )
                    decisionCache[cacheKey] = decision
                    return decision
                }
            }

            for (p in pathPatterns) {
                if (p.matcher(url).find() || p.matcher(path).find()) {
                    val decision = BlockDecision(
                        isBlocked = true,
                        type = BlockType.AD,
                        matchedRule = p.pattern()
                    )
                    decisionCache[cacheKey] = decision
                    return decision
                }
            }
        }

        val allowed = BlockDecision(isBlocked = false)
        decisionCache[cacheKey] = allowed
        return allowed
    }

    private fun findMatchingDomain(host: String): String? {
        if (host.isEmpty()) return null
        if (exactBlockedDomains.contains(host)) return host
        if (suffixBlockedDomains.contains(host)) return host

        // Check parent domains
        var remaining = host
        while (remaining.contains(".")) {
            remaining = remaining.substringAfter(".")
            if (suffixBlockedDomains.contains(remaining)) {
                return remaining
            }
        }
        return null
    }

    fun extractHost(url: String): String {
        val clean = url.trim()
        val noScheme = when {
            clean.startsWith("https://", ignoreCase = true) -> clean.substring(8)
            clean.startsWith("http://", ignoreCase = true) -> clean.substring(7)
            clean.startsWith("//") -> clean.substring(2)
            else -> clean
        }
        val hostPort = noScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        return hostPort.substringBefore(':').lowercase(Locale.ROOT)
    }

    fun extractPath(url: String): String {
        val clean = url.trim()
        val noScheme = when {
            clean.startsWith("https://", ignoreCase = true) -> clean.substring(8)
            clean.startsWith("http://", ignoreCase = true) -> clean.substring(7)
            clean.startsWith("//") -> clean.substring(2)
            else -> clean
        }
        val slashIndex = noScheme.indexOf('/')
        return if (slashIndex != -1) noScheme.substring(slashIndex).lowercase(Locale.ROOT) else "/"
    }

    fun isThirdParty(requestUrl: String, pageUrl: String?): Boolean {
        if (pageUrl.isNullOrBlank()) return false
        val reqHost = extractHost(requestUrl)
        val pageHost = extractHost(pageUrl)
        if (reqHost.isEmpty() || pageHost.isEmpty()) return false
        return !reqHost.equals(pageHost, ignoreCase = true) && !reqHost.endsWith(".$pageHost", ignoreCase = true)
    }
}
