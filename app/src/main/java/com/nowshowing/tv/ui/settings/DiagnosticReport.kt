package com.nowshowing.tv.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebView
import com.nowshowing.tv.BuildConfig
import com.nowshowing.tv.data.db.NowShowingDatabase
import com.nowshowing.tv.data.repository.MetadataHealth
import java.security.MessageDigest

/** No viewing history, provider URLs, keys, serial numbers, or account identifiers. Call on IO. */
@Suppress("DEPRECATION")
fun diagnosticReport(context: Context): String = buildString {
    appendLine("NowShowing TV ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    appendLine("Commit: ${BuildConfig.BUILD_COMMIT}")
    appendLine("Variant: ${BuildConfig.BUILD_TYPE}; debug=${BuildConfig.DEBUG}")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE}; API ${Build.VERSION.SDK_INT}")
    appendLine("WebView: ${runCatching { WebView.getCurrentWebViewPackage()?.versionName }.getOrNull() ?: "Unavailable"}")
    val fingerprint = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName,
            if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES)
        val certificates = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        certificates?.firstOrNull()?.let { signature ->
            MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString(":") { "%02X".format(it) }
        }
    }.getOrNull()
    appendLine("Installed certificate SHA-256: ${fingerprint ?: "Unavailable"}")
    appendLine("Signing continuity: compare certificate with the previous installed APK; permanence is not inferred.")
    appendLine("Last crash/recovery event: ${com.nowshowing.tv.diagnostics.CrashDiagnostics.last(context)}")
    appendLine("Metadata: ${MetadataHealth.failure.value?.message ?: "No failure recorded; not a connectivity test"}")
    appendLine("Expired cached metadata used this session: ${MetadataHealth.usedStaleCache}")
    appendLine("Metadata payload bytes: ${NowShowingDatabase.getInstance(context).metadataBytes()}")
    appendLine("Image disk cache bytes: ${coil.Coil.imageLoader(context).diskCache?.size ?: 0}")
    val runtime = Runtime.getRuntime()
    appendLine("App heap used/max bytes: ${runtime.totalMemory() - runtime.freeMemory()}/${runtime.maxMemory()}")
    appendLine("Recent metadata errors (time/category only, max 12):")
    MetadataHealth.recentErrors().forEach { appendLine(it) }
    appendLine("Provider page loading does not verify video playback. Web completion events are not supported.")
}
