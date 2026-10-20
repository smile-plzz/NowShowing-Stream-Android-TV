package com.nowshowing.tv.ui.mood

import android.app.Application
import com.google.gson.Gson
import com.nowshowing.tv.data.api.OmdbApiService
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.model.*
import com.nowshowing.tv.data.repository.*
import com.nowshowing.tv.domain.mood.MoodRecommendationEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class MoodLoadingTest {
    private lateinit var db: NowShowingDatabase
    private lateinit var vm: MoodViewModel
    private val api = FakeApi()
    private fun mood(name: String) = MoodRecommendationEngine.MOODS.first { it.name == name }
    @Before fun setup() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        MetadataHealth.retry()
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("metadata", 0).edit().clear().commit()
        db = NowShowingDatabase(app)
        val repository = MediaRepository(app, api, db)
        repository.clearMetadataCache()
        vm = MoodViewModel(app, repository)
    }
    @After fun teardown() {
        api.gate.complete(Unit)
        db.close()
        MetadataHealth.retry()
        Dispatchers.resetMain()
    }
    @Test fun yearChangesAreLocalAndRestoreOriginalRanking() = runBlocking {
        vm.selectMood(mood("Happy"))
        val loaded = withTimeout(10000) { vm.uiState.first { !it.isLoading && it.selectedMood != null } }
        assertNull(loaded.selectedYear)
        assertTrue(loaded.recommendations.isNotEmpty())
        val original = loaded.recommendations.map { it.imdbID }
        val calls = api.calls.get()
        vm.selectYear(2024)
        assertTrue(vm.uiState.value.recommendations.all { it.year == "2024" })
        vm.selectYear(1901)
        assertTrue(vm.uiState.value.recommendations.isEmpty())
        assertTrue(vm.uiState.value.statusMessage.contains("current collection"))
        vm.selectYear(null)
        assertEquals(original, vm.uiState.value.recommendations.map { it.imdbID })
        vm.selectMood(mood("Happy"))
        assertEquals(calls, api.calls.get())
        assertTrue("Mood hydration budget", calls <= 4)
    }
    @Test fun yearDuringLoadingIsAppliedToLatestSnapshot() = runBlocking {
        api.block = true
        vm.selectMood(mood("Happy"))
        withTimeout(5000) { api.started.await() }
        vm.selectYear(2020)
        api.gate.complete(Unit)
        val loaded = withTimeout(10000) { vm.uiState.first { !it.isLoading } }
        assertEquals(2020, loaded.selectedYear)
        assertTrue(loaded.recommendations.isNotEmpty())
        assertTrue(loaded.recommendations.all { it.year == "2020" })
        vm.selectMood(mood("Playful"))
        val changed = withTimeout(10000) { vm.uiState.first { !it.isLoading } }
        assertEquals(2020, changed.selectedYear)
    }
    @Test fun cancelledMoodCannotReplaceNewSelection() = runBlocking {
        api.block = true
        vm.selectMood(mood("Happy"))
        withTimeout(5000) { api.started.await() }
        vm.selectMood(mood("Angry"))
        api.gate.complete(Unit)
        val loaded = withTimeout(10000) { vm.uiState.first { !it.isLoading } }
        assertEquals("Angry", loaded.selectedMood?.name)
        assertTrue(loaded.recommendations.isNotEmpty())
        assertTrue(loaded.recommendations.all { it.title.startsWith("Action") })
    }
    private class FakeApi : OmdbApiService {
        val calls = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        @Volatile var block = false
        private val comfortIds = setOf("tt0382932", "tt2096673", "tt0266543", "tt0126029")
        override suspend fun getDetails(apiKey: String, imdbId: String, plot: String): OmdbTitleDetail {
            calls.incrementAndGet()
            started.complete(Unit)
            if (block) gate.await()
            val comfort = imdbId in comfortIds
            val fields = mapOf("Title" to (if (comfort) "Comedy" else "Action") + imdbId,
                "Year" to if (imdbId == "tt0382932") "2020" else "2024",
                "imdbID" to imdbId, "Type" to "movie", "Response" to "True",
                "Genre" to if (comfort) "Comedy, Animation, Family" else "Action, Thriller",
                "Plot" to if (comfort) "A heartwarming and whimsical adventure." else "A revenge mission.")
            return Gson().fromJson(Gson().toJson(fields), OmdbTitleDetail::class.java)
        }
        override suspend fun searchTitles(apiKey: String, query: String, page: Int, type: String?, year: Int?): OmdbSearchResponse = error("Mood discovery must not issue title-word searches")
        override suspend fun getDetailsByTitle(apiKey: String, title: String, type: String?, plot: String): OmdbTitleDetail = error("Mood candidates use IMDb IDs")
        override suspend fun getSeason(apiKey: String, imdbId: String, season: Int): OmdbSeasonResponse = error("unused")
    }
}
