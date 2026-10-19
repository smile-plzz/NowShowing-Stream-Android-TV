package com.nowshowing.tv.ui.settings

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import com.nowshowing.tv.ui.theme.NowShowingTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w960dp-h540dp")
class HealthPhotoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun remotePageActionsKeepSnapshotIdentityAndEvidenceFits() {
        val report = HealthReport("frozen1", "2026-10-19 09:00:00 Asia/Dhaka", "1.6.0-rc.1 (16000)",
            photoPages("Evidence", "W".repeat(480)))
        compose.setContent { NowShowingTheme { HealthPhotoDialog(report) {} } }
        compose.onNodeWithText("App health · Page 1 of 3").assertExists()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("health-photo-body").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty())
        assertFalse("Evidence must fit without clipping", layouts.first().hasVisualOverflow)
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("App health · Page 2 of 3").assertExists()
        compose.onNodeWithText("Report frozen1 · 1.6.0-rc.1 (16000)\n2026-10-19 09:00:00 Asia/Dhaka").assertExists()
        compose.onNodeWithText("Previous").performClick()
        compose.onNodeWithText("App health · Page 1 of 3").assertExists()
    }
}
