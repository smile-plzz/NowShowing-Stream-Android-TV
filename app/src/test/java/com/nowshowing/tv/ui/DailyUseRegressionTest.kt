package com.nowshowing.tv.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.google.gson.Gson
import com.nowshowing.tv.data.api.OmdbApiService
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.model.*
import com.nowshowing.tv.data.repository.*
import com.nowshowing.tv.ui.details.DetailsViewModel
import com.nowshowing.tv.ui.player.PlayerViewModel
import com.nowshowing.tv.ui.search.SearchViewModel
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
import java.util.Collections

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class DailyUseRegressionTest {
    private lateinit var db: NowShowingDatabase
    private lateinit var repository: MediaRepository
    private lateinit var app: Application
    private val api = FakeApi()
    private val models = ViewModelStore()

    @Before fun setup() = runBlocking<Unit> {
        Dispatchers.setMain(Dispatchers.Unconfined)
        MetadataHealth.retry()
        api.failPageTwo = false
        api.failSeason = false
        api.seasonGate = null
        api.detailGate = null
        api.pages.clear()
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("metadata", 0).edit().clear().commit()
        db = NowShowingDatabase(app)
        repository = MediaRepository(app, api, db) { emptyMap() }
        repository.clearMetadataCache()
    }

    private fun <T : ViewModel> track(vm: T): T {
        models.put(vm.javaClass.name, vm)
        return vm
    }

    @After fun teardown() {
        models.clear()
        api.seasonGate?.complete(Unit)
        api.detailGate?.complete(Unit)
        api.failPageTwo = false
        api.failSeason = false
        api.seasonGate = null
        api.detailGate = null
        api.pages.clear()
        db.close()
        MetadataHealth.retry()
        Dispatchers.resetMain()
    }

    @Test fun failedLoadMoreRetriesSamePageWithoutDiscardingResults() = runBlocking<Unit> {
        val vm = track(SearchViewModel(app, repository))
        vm.executeSearch("Test")
        withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.results.isNotEmpty() } }
        api.failPageTwo = true
        vm.executeSearch(reset = false)
        withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.errorMessage != null } }
        assertEquals(1, vm.uiState.value.results.size)
        api.failPageTwo = false
        vm.retrySearch()
        val state = withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.currentPage == 2 } }
        assertEquals(listOf("tt0000001", "tt0000002"), state.results.map { it.imdbID })
        assertEquals(listOf(1, 2, 2), api.pages.toList())
    }

    @Test fun searchPublishesBusyStateBeforeHistoryIoStarts() {
        Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())
        try {
            val vm = track(SearchViewModel(app, repository))
            vm.executeSearch("Test")
            assertTrue(vm.uiState.value.isLoading)
            assertEquals("Test", vm.uiState.value.query)
            vm.onClear()
            assertFalse(vm.uiState.value.isLoading)
            assertEquals("", vm.uiState.value.resultQuery)
        } finally {
            Dispatchers.setMain(Dispatchers.Unconfined)
        }
    }

    @Test fun changingTypeWithBlankDraftClearsOldResultsAndErrors() = runBlocking<Unit> {
        val vm = track(SearchViewModel(app, repository))
        vm.executeSearch("Test")
        withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.results.isNotEmpty() } }
        vm.setQuery("")
        vm.setTypeFilter("series")
        assertTrue(vm.uiState.value.results.isEmpty())
        assertEquals("", vm.uiState.value.resultQuery)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun backspaceClearsObsoleteSearchError() = runBlocking<Unit> {
        val vm = track(SearchViewModel(app, repository))
        api.failPageTwo = true
        try {
            vm.executeSearch("Test")
            withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.results.isNotEmpty() } }
            vm.executeSearch(reset = false)
            withTimeout(15_000) { vm.uiState.first { !it.isLoading && it.errorMessage != null } }
            vm.onBackspace()
            assertNull(vm.uiState.value.errorMessage)
            assertEquals("Tes", vm.uiState.value.query)
        } finally {
            api.failPageTwo = false
        }
    }

    @Test fun episodeFailureOffersRetryAndRestoresList() = runBlocking<Unit> {
        val vm = track(DetailsViewModel(app, repository))
        api.failSeason = true
        try {
            vm.loadDetails("tt0000010", "series")
            withTimeout(15_000) { vm.uiState.first { it.episodeError != null } }
            assertFalse(vm.uiState.value.episodesLoading)
            api.failSeason = false
            vm.retryEpisodes()
            val state = withTimeout(15_000) { vm.uiState.first { !it.episodesLoading && it.episodes.isNotEmpty() } }
            assertNull(state.episodeError)
            assertEquals(listOf("2", "3"), state.episodes.map { it.episode })
            assertEquals(2, state.selectedEpisode)
        } finally {
            api.failSeason = false
        }
    }

    @Test fun changingSeasonClearsOldEpisodesAndShowsLoadingImmediately() = runBlocking<Unit> {
        val vm = track(DetailsViewModel(app, repository))
        vm.loadDetails("tt0000010", "series")
        withTimeout(15_000) { vm.uiState.first { it.episodes.isNotEmpty() } }
        api.seasonGate = CompletableDeferred()
        vm.selectSeason(2)
        assertTrue(vm.uiState.value.episodesLoading)
        assertTrue(vm.uiState.value.episodes.isEmpty())
        vm.selectSeason(99)
        assertEquals(2, vm.uiState.value.selectedSeason)
        api.seasonGate?.complete(Unit)
        withTimeout(15_000) { vm.uiState.first { !it.episodesLoading } }
    }

    @Test fun switchingFromSeriesToMovieClearsEpisodeStateImmediately() = runBlocking<Unit> {
        val vm = track(PlayerViewModel(app, repository))
        vm.initialize("tt0000010", 1, 2, "series")
        withTimeout(15_000) { vm.uiState.first { it.episodeList.isNotEmpty() } }
        api.detailGate = CompletableDeferred()
        vm.initialize("tt0000020", 1, 1, "movie")
        val state = vm.uiState.value
        assertEquals("movie", state.type)
        assertTrue(state.episodeList.isEmpty())
        assertEquals("", state.episodeTitle)
        assertNull(state.webEmbedUrl)
        assertFalse(state.episodesLoading)
        api.detailGate?.complete(Unit)
        withTimeout(15_000) { vm.uiState.first { !it.isLoading } }
    }

    private class FakeApi : OmdbApiService {
        val pages = Collections.synchronizedList(mutableListOf<Int>())
        @Volatile var failPageTwo = false
        @Volatile var failSeason = false
        @Volatile var seasonGate: CompletableDeferred<Unit>? = null
        @Volatile var detailGate: CompletableDeferred<Unit>? = null
        override suspend fun searchTitles(apiKey: String, query: String, page: Int, type: String?, year: Int?): OmdbSearchResponse {
            pages.add(page)
            if (page == 2 && failPageTwo) throw java.io.IOException("Offline")
            return OmdbSearchResponse(listOf(OmdbTitleShort("Title $page", "2024", "tt000000$page", "movie", "N/A")), "2", "True", null)
        }
        override suspend fun getDetails(apiKey: String, imdbId: String, plot: String): OmdbTitleDetail {
            detailGate?.await()
            return Gson().fromJson("""{"Title":"Test","Year":"2024","imdbID":"$imdbId","Type":"series","totalSeasons":"2","Response":"True"}""", OmdbTitleDetail::class.java)
        }
        override suspend fun getSeason(apiKey: String, imdbId: String, season: Int): OmdbSeasonResponse {
            seasonGate?.await()
            if (failSeason) throw java.io.IOException("Offline")
            return OmdbSeasonResponse("Test", "$season", "2", listOf(
                OmdbEpisode("Third", null, "3", null, "tt0000003"),
                OmdbEpisode("Bad", null, "N/A", null, "tt0000000"),
                OmdbEpisode("Second", null, "2", null, "tt0000002"),
                OmdbEpisode("Duplicate", null, "2", null, "tt0000002")
            ), "True", null)
        }
        override suspend fun getDetailsByTitle(apiKey: String, title: String, type: String?, plot: String): OmdbTitleDetail = error("unused")
    }
}
