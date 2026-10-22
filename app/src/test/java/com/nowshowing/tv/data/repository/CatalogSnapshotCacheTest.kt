package com.nowshowing.tv.data.repository

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CatalogSnapshotCacheTest {
    @Test fun invalidUpdateAndOfflineRestartKeepLastGoodSnapshot() = runBlocking {
        val app = RuntimeEnvironment.getApplication<Application>()
        app.getSharedPreferences("cloud_catalog", 0).edit().clear().commit()
        var now = 0L
        var calls = 0
        var json = """{"version":1,"sections":{"movies":["tt1375666"]}}"""
        val cache = CatalogSnapshotCache(clock = { now }) { calls++; json }
        assertEquals(listOf("tt1375666"), cache.sections(app)["movies"])
        json = "broken json"
        assertEquals(listOf("tt1375666"), cache.sections(app)["movies"])
        assertEquals(1, calls)
        now = 900001
        assertEquals(listOf("tt1375666"), cache.sections(app)["movies"])
        assertEquals(2, calls)
        val restarted = CatalogSnapshotCache { throw java.io.IOException("offline") }
        assertEquals(listOf("tt1375666"), restarted.sections(app)["movies"])
        json = """{"version":1,"sections":{"movies":["tt0133093"]}}"""
        now += 900001
        assertEquals(listOf("tt0133093"), cache.sections(app)["movies"])
    }
}
