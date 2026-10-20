package com.nowshowing.tv.ui.browse

import android.app.Application
import com.google.gson.Gson
import com.nowshowing.tv.data.api.OmdbApiService
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.model.*
import com.nowshowing.tv.data.repository.*
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
class BrowseLoadingTest {
    private lateinit var db: NowShowingDatabase
    private lateinit var vm: BrowseViewModel
    private lateinit var repository: MediaRepository
    private val api = FakeApi()
    @Before fun setup() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        MetadataHealth.retry()
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("metadata", 0).edit().clear().commit()
        db = NowShowingDatabase(context)
        repository = MediaRepository(context, api, db)
        repository.clearMetadataCache()
        vm = BrowseViewModel(context, repository)
    }
    @After fun teardown() {
        api.gate.complete(Unit)
        db.close()
        MetadataHealth.retry()
        Dispatchers.resetMain()
    }
    @Test fun sortingDuringInitialLoadKeepsLoadingState() = runBlocking {
        api.block = true
        vm.loadBrowse("movies")
        withTimeout(5000) { api.started.await() }
        vm.setSortMode(SortMode.NEWEST)
        assertTrue(vm.uiState.value.isLoading)
        api.gate.complete(Unit)
        val loaded = withTimeout(10000) { vm.uiState.first { !it.isLoading } }
        assertEquals(MediaRepository.POPULAR_MOVIES.size, loaded.items.size)
        assertEquals(SortMode.NEWEST, loaded.sortMode)
    }
    @Test fun forcedRetryCompletesPartiallyPopulatedCollection() = runBlocking {
        api.failAfterTwo = true
        vm.loadBrowse("movies")
        val partial = withTimeout(10000) { vm.uiState.first { !it.isLoading } }
        assertEquals(2, partial.items.size)
        api.failAfterTwo = false
        vm.retryBrowse("movies")
        val complete = withTimeout(10000) { vm.uiState.first { !it.isLoading && it.items.size > 2 } }
        assertEquals(MediaRepository.POPULAR_MOVIES.size, complete.items.size)
    }
    private class FakeApi : OmdbApiService {
        val calls = AtomicInteger()
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        @Volatile var block = false
        @Volatile var failAfterTwo = false
        override suspend fun getDetailsByTitle(apiKey: String, title: String, type: String?, plot: String): OmdbTitleDetail {
            val call = calls.incrementAndGet()
            started.complete(Unit)
            if (block) gate.await()
            if (failAfterTwo && call > 2) throw MetadataUnavailable(MetadataFailure.QUOTA)
            return Gson().fromJson("""{"Title":"$title","Year":"2024","imdbID":"tt${title.hashCode().toLong() and 0xffffffffL}","Type":"$type","Response":"True"}""", OmdbTitleDetail::class.java)
        }
        override suspend fun searchTitles(apiKey: String, query: String, page: Int, type: String?, year: Int?): OmdbSearchResponse = error("unused")
        override suspend fun getDetails(apiKey: String, imdbId: String, plot: String): OmdbTitleDetail = error("unused")
        override suspend fun getSeason(apiKey: String, imdbId: String, season: Int): OmdbSeasonResponse = error("unused")
    }
}
