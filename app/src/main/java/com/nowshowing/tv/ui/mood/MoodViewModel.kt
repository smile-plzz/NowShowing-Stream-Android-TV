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
    val isLoading: Boolean = false,
    val statusMessage: String = "Select how you're feeling to find precise genre-verified movies.",
    val recommendations: List<RankedMovie> = emptyList()
)

class MoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)
    private val _uiState = MutableStateFlow(MoodUiState())
    val uiState: StateFlow<MoodUiState> = _uiState.asStateFlow()

    private var recommendationJob: Job? = null
    fun retry() { MetadataHealth.retry(); _uiState.value.selectedMood?.let(::selectMood) }

    fun selectMood(mood: MoodItem) {
        recommendationJob?.cancel()
        recommendationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedMood = mood,
                isLoading = true,
                statusMessage = "Finding precise matches for ${mood.name.lowercase()} (${mood.label.lowercase()})…"
            )

            val results = MoodRecommendationEngine.fetchMoodRecommendations(
                repository = repository,
                mood = mood.name
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                statusMessage = if (results.isNotEmpty()) {
                    "Showing matches for: ${mood.name} — ${mood.description}"
                } else {
                    MetadataHealth.failure.value?.message ?: "No matches found for ${mood.name.lowercase()} right now. Try another mood."
                },
                recommendations = results
            )
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
