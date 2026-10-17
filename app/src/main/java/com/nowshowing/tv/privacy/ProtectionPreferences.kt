package com.nowshowing.tv.privacy

import android.content.Context
import android.content.SharedPreferences

class ProtectionPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "nowshowing_protection_prefs"
        private const val KEY_DOMAIN_VALIDATION = "pref_domain_validation"
        private const val KEY_TRACKER_BLOCKING = "pref_tracker_blocking"
        private const val KEY_REDIRECT_ISOLATION = "pref_redirect_isolation"
        private const val KEY_HTTPS_ENFORCEMENT = "pref_https_enforcement"
        private const val KEY_EXEMPT_SOURCES = "pref_exempt_sources"
    }

    var isDomainValidationEnabled: Boolean
        get() = prefs.getBoolean(KEY_DOMAIN_VALIDATION, true)
        set(value) = prefs.edit().putBoolean(KEY_DOMAIN_VALIDATION, value).apply()

    var isTrackerBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKER_BLOCKING, true)
        set(value) = prefs.edit().putBoolean(KEY_TRACKER_BLOCKING, value).apply()

    var isRedirectIsolationEnabled: Boolean
        get() = prefs.getBoolean(KEY_REDIRECT_ISOLATION, true)
        set(value) = prefs.edit().putBoolean(KEY_REDIRECT_ISOLATION, value).apply()

    var isHttpsEnforced: Boolean
        get() = prefs.getBoolean(KEY_HTTPS_ENFORCEMENT, true)
        set(value) = prefs.edit().putBoolean(KEY_HTTPS_ENFORCEMENT, value).apply()

    fun isSourceExempt(sourceName: String): Boolean {
        val exempt = prefs.getStringSet(KEY_EXEMPT_SOURCES, emptySet()) ?: emptySet()
        return exempt.contains(sourceName)
    }

    fun setSourceExempt(sourceName: String, exempt: Boolean) {
        val current = (prefs.getStringSet(KEY_EXEMPT_SOURCES, emptySet()) ?: emptySet()).toMutableSet()
        if (exempt) {
            current.add(sourceName)
        } else {
            current.remove(sourceName)
        }
        prefs.edit().putStringSet(KEY_EXEMPT_SOURCES, current).apply()
    }

    fun getExemptSources(): Set<String> {
        return prefs.getStringSet(KEY_EXEMPT_SOURCES, emptySet()) ?: emptySet()
    }

    fun clearAllSourceExceptions() {
        prefs.edit().remove(KEY_EXEMPT_SOURCES).apply()
    }
}
