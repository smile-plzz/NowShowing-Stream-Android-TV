package com.nowshowing.tv.ui

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.nowshowing.tv.data.repository.MetadataHealth
import com.nowshowing.tv.ui.common.TvTopBar
import com.nowshowing.tv.ui.player.TvPlayerActionButton
import com.nowshowing.tv.ui.settings.SettingsScreen
import com.nowshowing.tv.ui.settings.SettingsViewModel
import com.nowshowing.tv.ui.theme.NowShowingTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "w960dp-h540dp")
class TvPolishTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()
    @Before fun setup() { MetadataHealth.retry() }
    @After fun teardown() { store.clear(); MetadataHealth.retry() }

    @Test fun narrowNavigationKeepsSettingsReachable() {
        var destination = ""
        compose.setContent { NowShowingTheme { Box(Modifier.width(640.dp)) { TvTopBar("home", { destination = it }) } } }
        compose.onNodeWithTag("primary-navigation").performScrollToNode(hasText("Settings"))
        compose.onNodeWithText("Settings").assertIsDisplayed().performClick()
        assertEquals("settings", destination)
    }

    @Test fun busyActionIsDisabled() {
        compose.setContent { NowShowingTheme { TvPlayerActionButton("Updating…", null, enabled = false) {} } }
        compose.onNodeWithText("Updating…").assertIsNotEnabled()
    }

    private fun settings() {
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(RuntimeEnvironment.getApplication())
        val vm = ViewModelProvider(store, factory)[SettingsViewModel::class.java]
        compose.setContent { NowShowingTheme { SettingsScreen(vm) {} } }
    }

    @Test fun settingsSeparatesEverydayControlsFromPrivacy() {
        settings()
        compose.onNodeWithText("Autoplay next episode").assertExists()
        compose.onNodeWithText("Privacy").performClick()
        compose.onNodeWithText("Tracker blocking").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Autoplay next episode").assertDoesNotExist()
        compose.onNodeWithText("Privacy").assertIsSelected()
    }

    @Test fun blankKeyCannotAccidentallyReplaceSavedCredential() {
        settings()
        compose.onNodeWithText("Metadata").performClick()
        compose.onNodeWithText("Save personal key").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Enter OMDb key").performScrollTo().performTextInput("abcdef12")
        compose.onNodeWithText("Save personal key").performScrollTo().assertIsEnabled()
    }
}
