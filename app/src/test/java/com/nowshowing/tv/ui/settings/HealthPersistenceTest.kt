package com.nowshowing.tv.ui.settings

import android.app.Application
import com.nowshowing.tv.data.repository.MetadataHealth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class HealthPersistenceTest {
    private val context get() = RuntimeEnvironment.getApplication()
    @After fun cleanup() { HealthReports.delete(context); MetadataHealth.retry(); context.getSharedPreferences("metadata", 0).edit().clear().commit() }
    @Test fun invalidLocalReportIsUnavailableInsteadOfCrashing() {
        context.getSharedPreferences("health_report", 0).edit().putString("last", "{}").commit()
        assertNull(HealthReports.load(context))
    }
    @Test fun frozenReportSurvivesReopeningAndDoesNotExposeConfiguredKey() {
        context.getSharedPreferences("metadata", 0).edit().putString("key", "private_key_must_not_appear").commit()
        val first = HealthReports.capture(context)
        val reopened = HealthReports.load(context)!!
        assertEquals(first, reopened)
        assertEquals(first.capturedAt, reopened.capturedAt)
        assertFalse(reopened.text().contains("private_key_must_not_appear"))
        assertTrue(reopened.text().contains("Network probe: skipped"))
        HealthReports.delete(context)
        assertNull(HealthReports.load(context))
    }
}
