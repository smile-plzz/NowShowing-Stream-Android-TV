package com.nowshowing.tv.ui.settings

import android.app.Application
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

    private val preferences = ProtectionPreferences(application)
    private val statistics = BlockingStatistics(application)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshState()
    }

    fun refreshState() {
        _uiState.value = SettingsUiState(
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

    fun updateFiltersOnline() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingFilters = true,
                updateStatusMessage = "Checking online for latest domain rules updates…"
            )
            val success = FilterListManager.updateRulesOnline(getApplication())
            _uiState.value = _uiState.value.copy(
                isUpdatingFilters = false,
                updateStatusMessage = if (success) "Domain and tracker filter lists updated to latest version." else "Filter lists are already up to date (using bundled baseline)."
            )
        }
    }
}
