package com.nowshowing.tv.diagnostics

import android.content.Context

object CrashDiagnostics {
    fun record(context: Context, category: String, error: Throwable? = null) {
        runCatching {
            val frames = error?.stackTrace.orEmpty().filter { it.className.startsWith("com.nowshowing.tv.") }
                .take(8).joinToString("\n") { "${it.className}.${it.methodName}:${it.lineNumber}" }
            val text = "${System.currentTimeMillis()} � $category\n${error?.javaClass?.simpleName.orEmpty()}\n$frames"
            context.getSharedPreferences("crash_diagnostics", 0).edit().putString("last", text.take(2000)).commit()
        }
    }
    fun last(context: Context): String = context.getSharedPreferences("crash_diagnostics", 0)
        .getString("last", "No event recorded").orEmpty()
    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            record(context, "Unhandled application exception", error)
            previous?.uncaughtException(thread, error)
        }
    }
}

