package com.nowshowing.tv.ui.mood

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nowshowing.tv.data.repository.MediaRepository
import com.nowshowing.tv.domain.mood.MoodItem
import com.nowshowing.tv.domain.mood.MoodRecommendationEngine
import com.nowshowing.tv.domain.mood.RankedMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.nowshowing.tv.data.repository.MetadataHealth

data class MoodUiState(
    val selectedMood: MoodItem? = null,
    val selectedYear: Int? = null,
    val isLoading: Boolean = false,
    val statusMessage: String = "Choose a mood to explore this collection.",
    val recommendations: List<RankedMovie> = emptyList()
)

class MoodViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: MediaRepository = MediaRepository(application)
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    private var allResults: List<RankedMovie> = emptyList()
    private val snapshots = mutableMapOf<String, List<RankedMovie>>()
    private var requestId = 0L
    private var recommendationJob: Job? = null
    fun selectYear(year: Int?) {
        publishResults(_uiState.value.copy(selectedYear = year))
    }
    private fun publishResults(state: MoodUiState = _uiState.value) {
        val results = allResults.filter { state.selectedYear == null ||
            com.nowshowing.tv.domain.recommendation.LocalRelevance.startYear(it.year) == state.selectedYear }
        _uiState.value = state.copy(recommendations = results,
            statusMessage = if (results.isEmpty()) "No matches in the current collection. Try All Years or another mood."
                else "${state.selectedMood?.label.orEmpty()} · Curated and metadata matches")
    }
    fun retry() { MetadataHealth.retry(); _uiState.value.selectedMood?.let { snapshots.remove(it.name); selectMood(it) } }

    fun selectMood(mood: MoodItem) {
        val identity = ++requestId
        recommendationJob?.cancel()
        snapshots[mood.name]?.let {
            allResults = it
            publishResults(_uiState.value.copy(selectedMood = mood, isLoading = false))
            return
        }
        recommendationJob = viewModelScope.launch {
            val sameMood = _uiState.value.selectedMood?.name == mood.name
            if (!sameMood) allResults = emptyList()
            _uiState.value = _uiState.value.copy(
                recommendations = if (sameMood) _uiState.value.recommendations else emptyList(),
                selectedMood = mood,
                isLoading = true,
                statusMessage = "Finding collection matches for ${mood.label}…"
            )

            try {
            val results = MoodRecommendationEngine.fetchMoodRecommendations(
                repository = repository,
                mood = mood.name
            )

            if (identity != requestId) return@launch
            snapshots[mood.name] = results
            allResults = results
            publishResults(_uiState.value.copy(isLoading = false))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (_: Exception) {
                if (identity == requestId) _uiState.value = _uiState.value.copy(isLoading = false,
                    statusMessage = "Could not load mood matches. Please retry.")
            }
        }
    }

    fun surpriseMe() {
        val random = MoodRecommendationEngine.MOODS.random()
        selectMood(random)
    }

    fun searchMoodByText(text: String) {
        val guessedName = MoodRecommendationEngine.guessMoodFromText(text)
        val found = MoodRecommendationEngine.MOODS.firstOrNull { it.name == guessedName }
            ?: MoodRecommendationEngine.MOODS.first()
        selectMood(found)
    }
}
