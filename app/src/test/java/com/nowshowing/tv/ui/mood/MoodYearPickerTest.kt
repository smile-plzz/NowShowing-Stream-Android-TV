package com.nowshowing.tv.ui.mood

import android.app.Application
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nowshowing.tv.ui.theme.NowShowingTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w960dp-h540dp")
class MoodYearPickerTest {
    @get:Rule val compose = createComposeRule()
    @Test fun currentYearIsVisibleFocusedAndOkAppliesIt() {
        var applied: Int? = null
        compose.setContent { NowShowingTheme { YearPicker(2020, {}, { applied = it }) } }
        compose.onNodeWithText("2020").assertIsDisplayed().assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(2020, applied) }
    }
    @Test fun backCancelsWithoutApplyingSelection() {
        var applied = false
        var dismissed = false
        compose.setContent { NowShowingTheme { YearPicker(null, { dismissed = true }, { applied = true }) } }
        compose.onNodeWithText("All Years").assertIsFocused().performKeyInput { pressKey(Key.Back) }
        compose.runOnIdle {
            assertTrue(dismissed)
            assertFalse(applied)
        }
    }
}
