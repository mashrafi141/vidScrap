package com.example.data.remote.scraper

import com.example.core.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class PlaybackResolution {
    data class Playable(val streamUrl: String, val isHls: Boolean = false) : PlaybackResolution()
    data class Unavailable(val reason: String, val webUrl: String) : PlaybackResolution()
}

class PlaybackResolver(
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        private fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    suspend fun resolve(video: Video): PlaybackResolution = withContext(Dispatchers.IO) {
        // If we already have a direct media stream URL cached and valid
        val cached = video.playbackReference
        if (!cached.isNullOrBlank() && (cached.endsWith(".mp4") || cached.contains(".m3u8"))) {
            val isHls = cached.contains(".m3u8")
            return@withContext PlaybackResolution.Playable(cached, isHls)
        }

        try {
            val request = Request.Builder()
                .url(video.pageUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext PlaybackResolution.Unavailable(
                    reason = "Page response HTTP ${response.code}",
                    webUrl = video.pageUrl
                )
            }

            val html = response.body?.string().orEmpty()
            if (html.isBlank()) {
                return@withContext PlaybackResolution.Unavailable(
                    reason = "Empty page response",
                    webUrl = video.pageUrl
                )
            }

            val doc = Jsoup.parse(html, video.pageUrl)

            // 1. Direct <video src> or <source src>
            val directSrc = doc.selectFirst("video source")?.attr("abs:src")
                ?.ifBlank { doc.selectFirst("video")?.attr("abs:src") }
            if (!directSrc.isNullOrBlank() && directSrc.startsWith("http")) {
                return@withContext PlaybackResolution.Playable(directSrc, directSrc.contains(".m3u8"))
            }

            // 2. Stream patterns in HTML / JSON scripts
            val hlsMatcher = Pattern.compile("\"(?:hlsUrl|hls|contentUrl|file)\"\\s*:\\s*\"(https?:\\\\[/\\\\]/[^\" ]+)\"").matcher(html)
            if (hlsMatcher.find()) {
                val url = hlsMatcher.group(1)?.replace("\\/", "/")
                if (!url.isNullOrBlank()) {
                    return@withContext PlaybackResolution.Playable(url, url.contains(".m3u8"))
                }
            }

            val plainUrlMatcher = Pattern.compile("\"(https://[^\"]+\\.(?:m3u8|mp4)[^\"]*)\"").matcher(html)
            if (plainUrlMatcher.find()) {
                val url = plainUrlMatcher.group(1)?.replace("\\/", "/")
                if (!url.isNullOrBlank()) {
                    return@withContext PlaybackResolution.Playable(url, url.contains(".m3u8"))
                }
            }

            // If direct stream could not be extracted (e.g. requires dynamic JS execution or session cookies)
            PlaybackResolution.Unavailable(
                reason = "Direct media stream is protected or requires browser session",
                webUrl = video.pageUrl
            )
        } catch (e: Exception) {
            PlaybackResolution.Unavailable(
                reason = e.localizedMessage ?: "Failed to resolve playback",
                webUrl = video.pageUrl
            )
        }
    }
}
