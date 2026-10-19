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
        context.getSharedPreferences("metadata", 0).edit().clear().commit()
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
    private fun configureKeys() {
        RuntimeEnvironment.getApplication().getSharedPreferences("metadata", 0).edit()
            .putString("key", "first123,second123,third123").commit()
        MetadataHealth.retry()
    }
    @Test fun invalidKeyFallsBackAndRemainsSkipped() = runBlocking {
        configureKeys()
        api.rejectedKeys.add("first123")
        assertEquals("True", repository.search("first").response)
        assertEquals("True", repository.search("second").response)
        assertEquals(listOf("first123", "second123", "second123"), api.usedKeys)
        assertNull(MetadataHealth.failure.value)
    }
    @Test fun allInvalidKeysStopAndDoNotLeakCredentials() = runBlocking {
        configureKeys()
        api.error = "Invalid API key!"
        assertTrue(repository.search("first").error!!.contains("invalid"))
        repository.search("second")
        assertEquals(3, api.calls)
        assertEquals(MetadataFailure.KEY, MetadataHealth.failure.value)
        assertFalse(MetadataHealth.recentErrors().joinToString().contains("first123"))
    }
    @Test fun quotaDoesNotCycleConfiguredCredentials() = runBlocking {
        configureKeys()
        api.error = "Request limit reached!"
        repository.search("first"); repository.search("second")
        assertEquals(listOf("first123"), api.usedKeys)
    }
    @Test fun networkAndNoResultsDoNotCycleKeys() = runBlocking {
        configureKeys()
        api.networkFailure = true
        repository.search("first")
        assertEquals(1, api.calls)
        MetadataHealth.retry(); api.networkFailure = false; api.error = "Movie not found!"
        repository.search("second")
        assertEquals(listOf("first123", "first123"), api.usedKeys)
    }
    @Test fun explicitRetryRechecksRejectedCredential() = runBlocking {
        configureKeys(); api.rejectedKeys.add("first123")
        repository.search("first")
        MetadataHealth.retry(); api.rejectedKeys.clear()
        repository.search("second")
        assertEquals(listOf("first123", "second123", "first123"), api.usedKeys)
    }
    @Test fun exhaustedKeysKeepExpiredSearchResults() = runBlocking {
        configureKeys()
        db.setCachedPayload("search_warm_1_", """{"Search":[],"Response":"True","totalResults":"0"}""", -1000)
        api.error = "Invalid API key!"
        assertTrue(repository.search("warm").isStale)
        assertEquals(3, api.calls)
    }
    private class FakeApi : OmdbApiService {
        val rejectedKeys = mutableSetOf<String>()
        val usedKeys = mutableListOf<String>()
        var error: String? = null
        var calls = 0
        var networkFailure = false
        override suspend fun searchTitles(apiKey: String, query: String, page: Int, type: String?, year: Int?): OmdbSearchResponse {
            calls++; usedKeys.add(apiKey); delay(5)
            if (networkFailure) throw java.io.IOException("URL with secret must never reach diagnostics")
            val responseError = if (apiKey in rejectedKeys) "Invalid API key!" else error
            return OmdbSearchResponse(emptyList(), "0", if (responseError == null) "True" else "False", responseError)
        }
        override suspend fun getDetails(apiKey: String, imdbId: String, plot: String): OmdbTitleDetail = error("unused")
        override suspend fun getDetailsByTitle(apiKey: String, title: String, type: String?, plot: String): OmdbTitleDetail = error("unused")
        override suspend fun getSeason(apiKey: String, imdbId: String, season: Int): OmdbSeasonResponse = error("unused")
    }
}
