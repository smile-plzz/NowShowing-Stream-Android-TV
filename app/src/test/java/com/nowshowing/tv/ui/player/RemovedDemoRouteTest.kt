package com.nowshowing.tv.ui.player

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class RemovedDemoRouteTest {
    @Test fun legacyDemoRoutesFailWithoutResolvingUnrelatedPlayback() {
        val vm = PlayerViewModel(RuntimeEnvironment.getApplication())
        listOf("native_mux_hls", "native_tears_hls", "native_bbb_mp4", "native_sintel_mp4").forEach { id ->
            vm.initialize(id, 1, 1, "diagnostic")
            vm.retry()
            vm.retryPlayback()
            assertFalse(vm.uiState.value.isLoading)
            assertNull(vm.uiState.value.playableSource)
            assertNull(vm.uiState.value.webEmbedUrl)
            assertTrue(vm.uiState.value.errorMessage!!.contains("no longer available"))
        }
    }
}
