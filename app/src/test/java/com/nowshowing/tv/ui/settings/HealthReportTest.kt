package com.nowshowing.tv.ui.settings

import org.junit.Assert.*
import org.junit.Test

class HealthReportTest {
    @Test fun longEvidenceIsPreservedAcrossContinuationPages() {
        val value = "certificate:" + "AB:".repeat(100)
        val pages = photoPages("Certificate", value)
        assertTrue(pages.size > 1)
        assertTrue(pages.all { it.body.lines().size <= 5 })
        assertTrue(pages.flatMap { it.body.lines() }.all { it.length <= 32 })
        assertEquals(value, pages.joinToString("") { it.body.replace("\n", "") })
    }
    @Test fun everyExportedPageHasReportIdentityAndOriginalTimestamp() {
        val report = HealthReport("abc", "2026-10-19 09:00 Asia/Dhaka", "1.6.0 (16000)", photoPages("Memory", "Unavailable"))
        assertTrue(report.text().contains("abc · 2026-10-19 09:00 Asia/Dhaka"))
        assertTrue(report.text().contains("Page 1/1: Memory"))
        assertTrue(report.text().contains("Unavailable"))
    }
}
