package com.nowshowing.tv.ui.settings

import android.app.Application
import com.nowshowing.tv.diagnostics.CrashDiagnostics
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class CrashDiagnosticsTest {
    @Test fun storesCategoryWithoutExceptionMessagesOrExternalFrames() {
        val app = RuntimeEnvironment.getApplication<Application>()
        val error = IllegalStateException("secret-key-and-private-url")
        error.stackTrace = arrayOf(StackTraceElement("com.nowshowing.tv.Test", "load", "Test.kt", 7),
            StackTraceElement("private.library.UserName", "run", "Secret.kt", 1))
        CrashDiagnostics.record(app, "Test failure", error)
        val record = CrashDiagnostics.last(app)
        assertTrue(record.contains("Test failure"))
        assertTrue(record.contains("IllegalStateException"))
        assertTrue(record.contains("com.nowshowing.tv.Test.load:7"))
        assertFalse(record.contains("secret-key"))
        assertFalse(record.contains("private.library"))
        assertTrue(record.length <= 2000)
    }
}
