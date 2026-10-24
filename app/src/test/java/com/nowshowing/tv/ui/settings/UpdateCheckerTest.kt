package com.nowshowing.tv.ui.settings

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun higherMajorVersionIsNewer() {
        assertTrue(UpdateChecker.isNewerVersion("v2.0.0", "1.6.0"))
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"))
    }

    @Test
    fun higherMinorVersionIsNewer() {
        assertTrue(UpdateChecker.isNewerVersion("v1.7.0", "1.6.0"))
        assertTrue(UpdateChecker.isNewerVersion("1.7.0", "1.6.9"))
    }

    @Test
    fun higherPatchVersionIsNewer() {
        assertTrue(UpdateChecker.isNewerVersion("1.6.1", "1.6.0"))
        assertTrue(UpdateChecker.isNewerVersion("v1.6.5", "1.6.4"))
    }

    @Test
    fun sameOrLowerVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewerVersion("1.6.0", "1.6.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.6.0", "1.6.0"))
        assertFalse(UpdateChecker.isNewerVersion("1.5.0", "1.6.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.5.9", "1.6.0"))
    }

    @Test
    fun releaseCandidateProgressionIsRecognized() {
        assertTrue(UpdateChecker.isNewerVersion("v1.6.0-rc.4", "1.6.0-rc.2"))
        assertFalse(UpdateChecker.isNewerVersion("v1.6.0-rc.2", "1.6.0-rc.4"))
        assertFalse(UpdateChecker.isNewerVersion("v1.6.0-rc.2", "1.6.0-rc.2"))
    }

    @Test
    fun stableReleaseIsNewerThanReleaseCandidate() {
        assertTrue(UpdateChecker.isNewerVersion("v1.6.0", "1.6.0-rc.4"))
        assertFalse(UpdateChecker.isNewerVersion("v1.6.0-rc.4", "1.6.0"))
    }

    @Test
    fun invalidOrBlankVersionReturnsFalse() {
        assertFalse(UpdateChecker.isNewerVersion("", "1.6.0"))
        assertFalse(UpdateChecker.isNewerVersion("v1.6.0", ""))
        assertFalse(UpdateChecker.isNewerVersion("invalid", "invalid"))
    }

    @Test
    fun parseReleaseJsonIdentifiesUpdateAvailable() {
        val json = """
            {
                "tag_name": "v1.6.0-rc.4",
                "name": "1.6.0-rc.4 — Daily-use reliability",
                "body": "Daily-use reliability release candidate.",
                "html_url": "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.4"
            }
        """.trimIndent()

        val result = UpdateChecker.parseReleaseJson(json, "1.6.0-rc.2")
        assertTrue(result is UpdateCheckResult.UpdateAvailable)
        val update = result as UpdateCheckResult.UpdateAvailable
        assertEquals("v1.6.0-rc.4", update.release.tagName)
        assertEquals("1.6.0-rc.4 — Daily-use reliability", update.release.name)
        assertEquals("https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.4", update.release.htmlUrl)
    }

    @Test
    fun parseReleaseJsonIdentifiesUpToDate() {
        val json = """
            {
                "tag_name": "v1.6.0-rc.2",
                "name": "1.6.0-rc.2",
                "body": "Current release.",
                "html_url": "https://github.com/smile-plzz/NowShowing-Stream-Android-TV/releases/tag/v1.6.0-rc.2"
            }
        """.trimIndent()

        val result = UpdateChecker.parseReleaseJson(json, "1.6.0-rc.2")
        assertTrue(result is UpdateCheckResult.UpToDate)
    }

    @Test
    fun parseReleaseJsonHandlesMalformedJsonGracefully() {
        val result = UpdateChecker.parseReleaseJson("not valid json", "1.6.0")
        assertTrue(result is UpdateCheckResult.Error)
    }
}
