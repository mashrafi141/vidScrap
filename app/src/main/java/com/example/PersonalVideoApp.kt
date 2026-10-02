package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import com.example.data.preferences.AppPreferences
import com.example.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PersonalVideoApp : Application(), ImageLoaderFactory {

    val appScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize background synchronization according to stored preferences
        appScope.launch {
            val prefs = AppPreferences(applicationContext)
            val isEnabled = prefs.isBackgroundSyncEnabled.first()
            if (isEnabled) {
                val hours = prefs.syncIntervalHours.first()
                val wifiOnly = prefs.isWifiOnly.first()
                WorkScheduler.schedulePeriodicSync(applicationContext, hours, wifiOnly)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("thumbnail_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB disk cache
                    .build()
            }
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
