package com.nowshowing.tv.data.repository

import com.nowshowing.tv.data.api.OmdbApiService
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.model.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class MetadataRepositoryTest {
    private lateinit var db: NowShowingDatabase
    private lateinit var repository: MediaRepository
    private val api = FakeApi()
    @Before fun setup() {
        MetadataHealth.retry()
        val context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(NowShowingDatabase.DATABASE_NAME)
        db = NowShowingDatabase(context)
        repository = MediaRepository(context, api, db)
    }
    @After fun teardown() { db.close(); MetadataHealth.retry() }
    @Test fun quotaFailureSuppressesLaterNetworkCalls() = runBlocking {
        api.error = "Request limit reached!"
        assertTrue(repository.search("first").error!!.contains("quota"))
        repository.search("second")
        assertEquals(1, api.calls)
    }
    @Test fun concurrentSearchesCoalesceAndCache() = runBlocking {
        coroutineScope { List(8) { async { repository.search("same") } }.awaitAll() }
        assertEquals(1, api.calls)
        repository.search("same"); assertEquals(1, api.calls)
    }
    @Test fun networkFailureKeepsExpiredSearchResults() = runBlocking {
        val cached = """{"Search":[{"Title":"Warm title","Year":"2026","imdbID":"tt100","Type":"movie","Poster":""}],"Response":"True","totalResults":"1"}"""
        db.setCachedPayload("search_warm_1_", cached, -1000)
        api.networkFailure = true
        val response = repository.search("warm")
        assertEquals("Warm title", response.search!!.single().title)
        assertTrue(response.isStale)
        assertEquals(MetadataFailure.NETWORK, MetadataHealth.failure.value)
        assertTrue(MetadataHealth.usedStaleCache)
    }
    @Test fun noResultsDoesNotPreventAnotherSearch() = runBlocking {
        api.error = "Movie not found!"; repository.search("absent")
        api.error = null; assertEquals("True", repository.search("other").response)
        assertEquals(2, api.calls)
    }
    private class FakeApi : OmdbApiService {
        var error: String? = null
        var calls = 0
        var networkFailure = false
        override suspend fun searchTitles(apiKey: String, query: String, page: Int, type: String?): OmdbSearchResponse {
            calls++; delay(5)
            if (networkFailure) throw java.io.IOException("URL with secret must never reach diagnostics")
            return OmdbSearchResponse(emptyList(), "0", if (error == null) "True" else "False", error)
        }
        override suspend fun getDetails(apiKey: String, imdbId: String, plot: String): OmdbTitleDetail = error("unused")
        override suspend fun getDetailsByTitle(apiKey: String, title: String, type: String?, plot: String): OmdbTitleDetail = error("unused")
        override suspend fun getSeason(apiKey: String, imdbId: String, season: Int): OmdbSeasonResponse = error("unused")
    }
}
