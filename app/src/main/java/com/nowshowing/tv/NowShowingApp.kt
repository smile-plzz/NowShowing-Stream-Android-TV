package com.nowshowing.tv

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

import com.nowshowing.tv.privacy.FilterListManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NowShowingApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        com.nowshowing.tv.diagnostics.CrashDiagnostics.install(this)
        // Initialize baseline privacy shields asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.nowshowing.tv.data.db.NowShowingDatabase.getInstance(this@NowShowingApp).pruneMetadata()
            } catch (e: Exception) {
                com.nowshowing.tv.diagnostics.CrashDiagnostics.record(this@NowShowingApp, "Metadata maintenance failed", e)
            }
            FilterListManager.initialize(this@NowShowingApp)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizeBytes(16 * 1024 * 1024) // Bound decoded artwork on low-memory TVs.
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}


