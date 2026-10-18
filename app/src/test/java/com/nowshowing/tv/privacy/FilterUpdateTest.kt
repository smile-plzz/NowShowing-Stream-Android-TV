package com.nowshowing.tv.privacy

import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class FilterUpdateTest {
    private val server = MockWebServer()
    private val rules = (1..6).joinToString("\n") { "||ad$it.example^" }
    @Before fun setup() { server.start() }
    @After fun teardown() { server.shutdown() }
    @Test fun partialUpdatePreservesFailedList() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "filters").apply { mkdirs() }
        val tracker = File(dir, "filters_tracking.txt").apply { writeText(rules) }
        server.enqueue(MockResponse().setBody(rules))
        server.enqueue(MockResponse().setResponseCode(503))
        val result = FilterListManager.updateRulesOnline(context, server.url("/ad").toString(), server.url("/tracking").toString())
        assertEquals(FilterUpdateResult.PARTIAL, result)
        assertEquals(rules, tracker.readText())
        assertEquals(rules, File(dir, "filters_adblock.txt").readText())
    }
    @Test fun invalidResponsesLeaveBothKnownGoodFilesIntact() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val dir = File(context.filesDir, "filters").apply { mkdirs() }
        val ad = File(dir, "filters_adblock.txt").apply { writeText(rules) }
        val tracker = File(dir, "filters_tracking.txt").apply { writeText(rules) }
        server.enqueue(MockResponse().setBody("<html>temporarily unavailable</html>"))
        server.enqueue(MockResponse().setBody(""))
        assertEquals(FilterUpdateResult.FAILED, FilterListManager.updateRulesOnline(context,
            server.url("/ad").toString(), server.url("/tracking").toString()))
        assertEquals(rules, ad.readText()); assertEquals(rules, tracker.readText())
    }
}
