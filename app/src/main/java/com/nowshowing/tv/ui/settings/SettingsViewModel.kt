package com.nowshowing.tv.ui.settings

import android.app.Application
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.data.repository.MetadataHealth
import com.nowshowing.tv.playback.PlaybackPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.privacy.BlockingStatistics
import com.nowshowing.tv.privacy.FilterListManager
import com.nowshowing.tv.privacy.ProtectionPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val autoplay: Boolean = true,
    val metadataStatus: String = "No failure recorded; not a connectivity test",
    val diagnosticText: String = "Select Refresh diagnostics to collect device and cache information.",
    val maintenanceMessage: String? = null,
    val isClearingCache: Boolean = false,
    val isDomainValidationEnabled: Boolean = true,
    val isTrackerBlockingEnabled: Boolean = true,
    val isRedirectIsolationEnabled: Boolean = true,
    val isHttpsEnforced: Boolean = true,
    val totalThreatsBlocked: Long = 0L,
    val totalTrackersBlocked: Long = 0L,
    val totalRedirectsBlocked: Long = 0L,
    val exemptSources: Set<String> = emptySet(),
    val updateStatusMessage: String? = null,
    val isUpdatingFilters: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val playback = PlaybackPreferences(application)
    private val preferences = ProtectionPreferences(application)
    private val statistics = BlockingStatistics(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshState()
        viewModelScope.launch { MetadataHealth.failure.collect { failure ->
            _uiState.value = _uiState.value.copy(metadataStatus = failure?.message ?: "No failure recorded; not a connectivity test")
        } }
    }

    fun refreshState() {
        _uiState.value = _uiState.value.copy(
            autoplay = playback.autoplay,
            isDomainValidationEnabled = preferences.isDomainValidationEnabled,
            isTrackerBlockingEnabled = preferences.isTrackerBlockingEnabled,
            isRedirectIsolationEnabled = preferences.isRedirectIsolationEnabled,
            isHttpsEnforced = preferences.isHttpsEnforced,
            totalThreatsBlocked = statistics.totalThreatsBlocked,
            totalTrackersBlocked = statistics.totalTrackersBlocked,
            totalRedirectsBlocked = statistics.totalRedirectsBlocked,
            exemptSources = preferences.getExemptSources()
        )
    }

    fun toggleDomainValidation() {
        val newVal = !preferences.isDomainValidationEnabled
        preferences.isDomainValidationEnabled = newVal
        refreshState()
    }

    fun toggleTrackerBlocking() {
        val newVal = !preferences.isTrackerBlockingEnabled
        preferences.isTrackerBlockingEnabled = newVal
        refreshState()
    }

    fun toggleRedirectIsolation() {
        val newVal = !preferences.isRedirectIsolationEnabled
        preferences.isRedirectIsolationEnabled = newVal
        refreshState()
    }

    fun toggleHttpsEnforcement() {
        val newVal = !preferences.isHttpsEnforced
        preferences.isHttpsEnforced = newVal
        refreshState()
    }

    fun resetStatistics() {
        statistics.clearStats()
        refreshState()
    }

    fun clearSourceExceptions() {
        preferences.clearAllSourceExceptions()
        refreshState()
    }

    fun removeSourceExemption(sourceName: String) {
        preferences.setSourceExempt(sourceName, false)
        refreshState()
    }

    fun toggleAutoplay() { playback.autoplay = !playback.autoplay; refreshState() }

    fun saveMetadataKey(value: String) {
        val clean = value.trim()
        if (clean.isNotEmpty() && !clean.matches(Regex("[A-Za-z0-9]{6,64}"))) {
            _uiState.value = _uiState.value.copy(maintenanceMessage = "Enter a valid OMDb key (letters and numbers).")
            return
        }
        getApplication<Application>().getSharedPreferences("metadata", android.content.Context.MODE_PRIVATE)
            .edit().apply { if (clean.isBlank()) remove("key") else putString("key", clean) }.apply()
        MetadataHealth.retry()
        _uiState.value = _uiState.value.copy(maintenanceMessage = "Metadata configuration saved. Retry Home or Search.")
    }

    fun retryMetadata() {
        MetadataHealth.retry()
        _uiState.value = _uiState.value.copy(maintenanceMessage = "Metadata retries enabled. Return Home and select Retry, or search again.")
    }

    fun refreshDiagnostics() {
        viewModelScope.launch {
            val report = withContext(Dispatchers.IO) { diagnosticReport(getApplication()) }
            _uiState.value = _uiState.value.copy(diagnosticText = report)
        }
    }

    fun clearCaches() {
        if (_uiState.value.isClearingCache) return
        _uiState.value = _uiState.value.copy(isClearingCache = true, maintenanceMessage = "Clearing metadata and image caches…")
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) { runCatching {
                MediaRepository(getApplication()).clearMetadataCache()
                coil.Coil.imageLoader(getApplication()).memoryCache?.clear()
                coil.Coil.imageLoader(getApplication()).diskCache?.clear()
            }.isSuccess }
            _uiState.value = _uiState.value.copy(isClearingCache = false, maintenanceMessage =
                if (success) "Metadata and image caches cleared. Watchlist, history, watched labels and settings preserved. Visible screens may retain loaded titles until reopened."
                else "Cache clearing could not complete. Your saved data was preserved.")
            refreshDiagnostics()
        }
    }

    fun updateFiltersOnline() {
        if (_uiState.value.isUpdatingFilters) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdatingFilters = true, updateStatusMessage = "Checking filter updates…")
            val result = FilterListManager.updateRulesOnline(getApplication())
            _uiState.value = _uiState.value.copy(isUpdatingFilters = false, updateStatusMessage = when (result) {
                com.nowshowing.tv.privacy.FilterUpdateResult.COMPLETE -> "Both filter lists updated."
                com.nowshowing.tv.privacy.FilterUpdateResult.PARTIAL -> "One list updated; the other failed. Its known-good rules remain active. Retry later."
                com.nowshowing.tv.privacy.FilterUpdateResult.FAILED -> "Update failed. Known-good rules remain active. Check the connection and retry."
            })
        }
    }
}
