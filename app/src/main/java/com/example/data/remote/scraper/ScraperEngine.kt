package com.example.data.remote.scraper

import com.example.core.model.ScrapeResult
import com.example.core.model.Source
import com.example.core.model.SourceType
import com.example.core.model.Video
import com.example.core.util.UrlUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ScraperEngine(
    private val client: OkHttpClient = defaultClient()
) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    suspend fun scrape(source: Source): ScrapeResult = withContext(Dispatchers.IO) {
        try {
            var targetUrl = source.url.trim()
            var pageData = fetchPage(targetUrl)

            // If 404 or failed on /playlists/, try recovering /my/favorites/videos/
            if (pageData == null && targetUrl.contains("/playlists/")) {
                val favUrl = targetUrl.replace("/playlists/", "/my/favorites/videos/")
                val favData = fetchPage(favUrl)
                if (favData != null) {
                    targetUrl = favUrl
                    pageData = favData
                }
            }

            if (pageData == null) {
                return@withContext ScrapeResult.Error("Could not connect to $targetUrl")
            }

            val (doc, html, finalUrl) = pageData

            // Check if redirected to login page (e.g. for /my/favorites)
            if (finalUrl.contains("/login") || finalUrl.contains("/signin")) {
                return@withContext ScrapeResult.Error(
                    "Personal playlist requires signing into your XHamster account. Tap 'Sign In to XHamster' to synchronize your private favorites."
                )
            }

            when (source.type) {
                SourceType.SINGLE_VIDEO -> scrapeSingleVideo(source.copy(url = targetUrl), doc, html)
                SourceType.PLAYLIST -> scrapeMultiPageCollection(source.copy(url = targetUrl), doc, html, isPlaylist = true)
                SourceType.PROFILE -> scrapeMultiPageCollection(source.copy(url = targetUrl), doc, html, isPlaylist = false)
            }
        } catch (e: Exception) {
            ScrapeResult.Error(
                message = e.localizedMessage ?: "Network error during scraping",
                exception = e,
                isRetryable = true
            )
        }
    }

    private fun fetchPage(url: String): Triple<Document, String, String>? {
        return try {
            val cookies = try {
                android.webkit.CookieManager.getInstance().getCookie(url)
            } catch (_: Exception) {
                null
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Referer", "https://xhamster.com/")

            if (!cookies.isNullOrBlank()) {
                requestBuilder.header("Cookie", cookies)
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) return null
            val finalUrl = response.request.url.toString()
            val html = response.body?.string().orEmpty()
            if (html.isBlank()) return null
            val doc = Jsoup.parse(html, url)
            Triple(doc, html, finalUrl)
        } catch (_: Exception) {
            null
        }
    }

    private fun scrapeSingleVideo(source: Source, doc: Document, rawHtml: String): ScrapeResult {
        val title = extractTitle(doc) ?: "Video ${UrlUtils.extractRemoteId(source.url)}"
        val thumbnail = extractThumbnail(doc)
        val duration = extractDuration(doc, rawHtml)
        val uploader = extractUploader(doc)
        val uploaderUrl = extractUploaderUrl(doc, source.url)
        val views = extractViews(doc, rawHtml)
        val publishedAt = extractPublishedDate(doc, rawHtml)
        val playbackUrl = extractPlaybackStream(doc, rawHtml)
        val remoteId = UrlUtils.extractRemoteId(source.url)

        val video = Video(
            remoteId = remoteId,
            sourceUrl = source.url,
            pageUrl = source.url,
            title = title,
            durationSeconds = duration,
            uploaderName = uploader,
            uploaderUrl = uploaderUrl,
            views = views,
            publishedAt = publishedAt,
            thumbnailUrl = thumbnail,
            playbackReference = playbackUrl
        )

        return ScrapeResult.Success(
            sourceName = title,
            videos = listOf(video),
            newVideosCount = 1
        )
    }

    /**
     * Multi-page collection scraper: retrieves all videos across multiple pages (up to 8 pages)
     * to capture full playlists and creator profiles with 100+ videos.
     */
    private suspend fun scrapeMultiPageCollection(
        source: Source,
        initialDoc: Document,
        initialHtml: String,
        isPlaylist: Boolean
    ): ScrapeResult = withContext(Dispatchers.IO) {
        val resolvedName = if (isPlaylist) {
            initialDoc.selectFirst("h1")?.text()?.trim()
                ?: initialDoc.selectFirst("meta[property='og:title']")?.attr("content")?.trim()
                ?: "Playlist"
        } else {
            initialDoc.selectFirst("h1")?.text()?.trim()
                ?: initialDoc.selectFirst(".user-info-name, .profile-name, [data-role='user-name']")?.text()?.trim()
                ?: initialDoc.selectFirst("meta[property='og:title']")?.attr("content")?.trim()
                ?: "Profile"
        }

        val allVideos = mutableListOf<Video>()
        val seenPageUrls = mutableSetOf<String>()

        // 1. Extract from first page
        val page1Videos = extractVideoList(initialDoc, initialHtml, source.url)
        for (v in page1Videos) {
            if (seenPageUrls.add(v.pageUrl)) {
                allVideos.add(v)
            }
        }

        // 2. Discover pagination URLs or try sequential pages
        val pagesToFetch = mutableListOf<String>()
        val baseClean = source.url.trimEnd('/')

        // Check if document has pager links
        val pagerLinks = initialDoc.select(".pager-container a, ul.pager a, a[data-page], a.pager-item")
        for (a in pagerLinks) {
            val href = a.attr("abs:href").ifBlank { a.attr("href") }
            if (href.isNotBlank() && !pagesToFetch.contains(href) && href != source.url) {
                pagesToFetch.add(href)
            }
        }

        // If no explicit pager links were found, generate standard page 2..6 URLs
        if (pagesToFetch.isEmpty()) {
            for (p in 2..6) {
                if (baseClean.contains("?")) {
                    pagesToFetch.add("$baseClean&page=$p")
                } else {
                    pagesToFetch.add("$baseClean/$p")
                }
            }
        }

        // Fetch up to 8 subsequent pages
        var consecutiveEmptyPages = 0
        for (pageUrl in pagesToFetch.take(8)) {
            if (consecutiveEmptyPages >= 2) break // Stop if pages keep returning no new videos

            val pageData = fetchPage(pageUrl)
            if (pageData == null) {
                consecutiveEmptyPages++
                continue
            }

            val (pageDoc, pageHtml, _) = pageData
            val pageVideos = extractVideoList(pageDoc, pageHtml, source.url)
            var addedOnThisPage = 0

            for (v in pageVideos) {
                if (seenPageUrls.add(v.pageUrl)) {
                    allVideos.add(v)
                    addedOnThisPage++
                }
            }

            if (addedOnThisPage == 0) {
                consecutiveEmptyPages++
            } else {
                consecutiveEmptyPages = 0
            }
        }

        ScrapeResult.Success(
            sourceName = resolvedName,
            videos = allVideos,
            newVideosCount = allVideos.size
        )
    }

    /**
     * Extracts videos from both HTML DOM elements and JSON payloads (such as window.initials)
     */
    private fun extractVideoList(doc: Document, rawHtml: String, baseUrl: String): List<Video> {
        val results = mutableListOf<Video>()
        val seenUrls = mutableSetOf<String>()

        // Method A: Extract from JSON payloads in scripts (window.initials or script#initials)
        extractFromJsonScripts(rawHtml, baseUrl, results, seenUrls)

        // Method B: Extract from HTML anchor elements & video card containers
        val videoAnchors = doc.select(
            "a[href*='/videos/'], a[href*='/video/'], [data-video-id] a, .video-thumb a, article a, .thumb-list__item a"
        )
        for (anchor in videoAnchors) {
            val href = anchor.attr("abs:href").ifBlank { anchor.attr("href") }
            if (href.isBlank() || !href.contains("/video")) continue

            val normalizedUrl = UrlUtils.normalizeUrl(resolveAbsolute(baseUrl, href))
            if (seenUrls.contains(normalizedUrl)) continue

            val container = anchor.closest(".video-thumb, .thumb, article, [data-video-id], .thumb-list__item") ?: anchor

            val title = container.selectFirst(".video-thumb-info__name, .thumb-title, .title, [data-role='video-title']")?.text()
                ?.ifBlank { null }
                ?: anchor.attr("title").ifBlank { null }
                ?: container.selectFirst("img")?.attr("alt")?.ifBlank { null }
                ?: "Video ${UrlUtils.extractRemoteId(normalizedUrl)}"

            val thumbImg = container.selectFirst("img")
            val thumbUrl = thumbImg?.attr("abs:src")?.ifBlank { thumbImg.attr("src") }
                ?.ifBlank { thumbImg.attr("data-src") }
                ?.ifBlank { thumbImg.attr("data-thumb") }
                ?.ifBlank { thumbImg.attr("data-preview") }

            val durationText = container.selectFirst(".duration, .badge-duration, [data-duration]")?.text()
            val durationSeconds = parseDurationString(durationText)

            val uploaderName = container.selectFirst(".uploader, .username, .user-name, a[href*='/users/'], a[href*='/creators/']")?.text()
            val remoteId = UrlUtils.extractRemoteId(normalizedUrl)

            seenUrls.add(normalizedUrl)
            results.add(
                Video(
                    remoteId = remoteId,
                    sourceUrl = baseUrl,
                    pageUrl = normalizedUrl,
                    title = title,
                    durationSeconds = durationSeconds,
                    uploaderName = uploaderName,
                    thumbnailUrl = thumbUrl
                )
            )
        }

        // Method C: Direct regex on HTML for any missed video links
        val regexLink = Pattern.compile("href=\"(/videos/[a-zA-Z0-9_-]+)\"").matcher(rawHtml)
        while (regexLink.find()) {
            val path = regexLink.group(1) ?: continue
            val fullUrl = UrlUtils.normalizeUrl(resolveAbsolute(baseUrl, path))
            if (seenUrls.add(fullUrl)) {
                val remoteId = UrlUtils.extractRemoteId(fullUrl)
                results.add(
                    Video(
                        remoteId = remoteId,
                        sourceUrl = baseUrl,
                        pageUrl = fullUrl,
                        title = "Video $remoteId"
                    )
                )
            }
        }

        return results
    }

    private fun extractFromJsonScripts(
        rawHtml: String,
        baseUrl: String,
        results: MutableList<Video>,
        seenUrls: MutableSet<String>
    ) {
        try {
            // Find JSON in script tag id="initials" or window.initials = {...}
            val initialsMatcher = Pattern.compile(
                "<script[^>]*id=[\"']initials[\"'][^>]*>([\\s\\S]*?)</script>|window\\.initials\\s*=\\s*(\\{[\\s\\S]*?\\});"
            ).matcher(rawHtml)

            if (initialsMatcher.find()) {
                val jsonString = initialsMatcher.group(1) ?: initialsMatcher.group(2)
                if (!jsonString.isNullOrBlank()) {
                    val root = JSONObject(jsonString)
                    parseVideosFromJsonObject(root, baseUrl, results, seenUrls)
                }
            }
        } catch (_: Exception) {
            // Safe fallback to DOM parsing
        }
    }

    private fun parseVideosFromJsonObject(
        jsonObj: JSONObject,
        baseUrl: String,
        results: MutableList<Video>,
        seenUrls: MutableSet<String>
    ) {
        val keys = jsonObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObj.opt(key)
            if (value is JSONArray) {
                for (i in 0 until value.length()) {
                    val item = value.optJSONObject(i) ?: continue
                    val pageUrl = item.optString("pageURL").ifBlank { item.optString("url") }
                    val title = item.optString("title")
                    if (pageUrl.isNotBlank() && (pageUrl.contains("/video") || title.isNotBlank())) {
                        val normalized = UrlUtils.normalizeUrl(resolveAbsolute(baseUrl, pageUrl))
                        if (seenUrls.add(normalized)) {
                            val duration = item.optLong("duration").takeIf { it > 0 }
                            val thumb = item.optString("thumbURL").ifBlank { item.optString("image") }.ifBlank { null }
                            val views = item.optLong("views").takeIf { it > 0 }
                            val author = item.optJSONObject("author")?.optString("name")
                                ?: item.optString("author")
                            val remoteId = item.optString("id").ifBlank { UrlUtils.extractRemoteId(normalized) }

                            results.add(
                                Video(
                                    remoteId = remoteId,
                                    sourceUrl = baseUrl,
                                    pageUrl = normalized,
                                    title = if (title.isNotBlank()) title else "Video $remoteId",
                                    durationSeconds = duration,
                                    uploaderName = author.ifBlank { null },
                                    views = views,
                                    thumbnailUrl = thumb
                                )
                            )
                        }
                    }
                }
            } else if (value is JSONObject) {
                parseVideosFromJsonObject(value, baseUrl, results, seenUrls)
            }
        }
    }

    private fun extractTitle(doc: Document): String? {
        val og = doc.selectFirst("meta[property='og:title']")?.attr("content")?.trim()
        if (!og.isNullOrBlank()) return cleanTitle(og)

        val twitter = doc.selectFirst("meta[name='twitter:title']")?.attr("content")?.trim()
        if (!twitter.isNullOrBlank()) return cleanTitle(twitter)

        val h1 = doc.selectFirst("h1")?.text()?.trim()
        if (!h1.isNullOrBlank()) return cleanTitle(h1)

        val pageTitle = doc.title().trim()
        if (pageTitle.isNotBlank()) return cleanTitle(pageTitle)

        return null
    }

    private fun cleanTitle(raw: String): String {
        return raw.replace(Regex("(?i) - xHamster.*$"), "")
            .replace(Regex("(?i) \\| xHamster.*$"), "")
            .replace(Regex("(?i) - Watch on xHamster.*$"), "")
            .trim()
    }

    private fun extractThumbnail(doc: Document): String? {
        val og = doc.selectFirst("meta[property='og:image']")?.attr("content")?.trim()
        if (!og.isNullOrBlank()) return og

        val twitter = doc.selectFirst("meta[name='twitter:image']")?.attr("content")?.trim()
        if (!twitter.isNullOrBlank()) return twitter

        val linkThumb = doc.selectFirst("link[rel='image_src']")?.attr("href")?.trim()
        if (!linkThumb.isNullOrBlank()) return linkThumb

        val poster = doc.selectFirst("video")?.attr("poster")?.trim()
        if (!poster.isNullOrBlank()) return poster

        return null
    }

    private fun extractDuration(doc: Document, rawHtml: String): Long? {
        val metaSec = doc.selectFirst("meta[property='video:duration']")?.attr("content")?.trim()
        if (!metaSec.isNullOrBlank()) {
            metaSec.toLongOrNull()?.let { return it }
        }

        val isoMatcher = Pattern.compile("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?").matcher(rawHtml)
        if (isoMatcher.find()) {
            val hours = isoMatcher.group(1)?.toLongOrNull() ?: 0L
            val minutes = isoMatcher.group(2)?.toLongOrNull() ?: 0L
            val seconds = isoMatcher.group(3)?.toLongOrNull() ?: 0L
            val total = hours * 3600 + minutes * 60 + seconds
            if (total > 0) return total
        }

        val durText = doc.selectFirst(".duration, .badge-duration")?.text()
        return parseDurationString(durText)
    }

    private fun extractUploader(doc: Document): String? {
        val authorMeta = doc.selectFirst("meta[name='author']")?.attr("content")?.trim()
        if (!authorMeta.isNullOrBlank()) return authorMeta

        val userLink = doc.selectFirst("a[href*='/users/'], a[href*='/channels/'], a[href*='/creators/'], [data-role='user-name']")?.text()?.trim()
        if (!userLink.isNullOrBlank()) return userLink

        return null
    }

    private fun extractUploaderUrl(doc: Document, baseUrl: String): String? {
        val userLink = doc.selectFirst("a[href*='/users/'], a[href*='/channels/'], a[href*='/creators/']")
        val href = userLink?.attr("href")
        return if (!href.isNullOrBlank()) resolveAbsolute(baseUrl, href) else null
    }

    private fun extractViews(doc: Document, rawHtml: String): Long? {
        val viewMatcher = Pattern.compile("([\\d,]+)\\s+views", Pattern.CASE_INSENSITIVE).matcher(rawHtml)
        if (viewMatcher.find()) {
            val numStr = viewMatcher.group(1)?.replace(",", "")
            numStr?.toLongOrNull()?.let { return it }
        }
        return null
    }

    private fun extractPublishedDate(doc: Document, rawHtml: String): String? {
        val uploadDateMatcher = Pattern.compile("\"uploadDate\"\\s*:\\s*\"([^\"]+)\"").matcher(rawHtml)
        if (uploadDateMatcher.find()) {
            return uploadDateMatcher.group(1)
        }
        val metaDate = doc.selectFirst("meta[property='video:release_date']")?.attr("content")
        return metaDate?.ifBlank { null }
    }

    private fun extractPlaybackStream(doc: Document, rawHtml: String): String? {
        val directSrc = doc.selectFirst("video source")?.attr("src")?.ifBlank { null }
        if (!directSrc.isNullOrBlank() && directSrc.startsWith("http")) {
            return directSrc
        }

        val hlsMatcher = Pattern.compile("\"(?:hlsUrl|hls|contentUrl|file)\"\\s*:\\s*\"(https?:\\\\[/\\\\]/[^\" ]+)\"").matcher(rawHtml)
        if (hlsMatcher.find()) {
            val found = hlsMatcher.group(1)
            return found?.replace("\\/", "/")
        }

        val plainUrlMatcher = Pattern.compile("\"(https://[^\"]+\\.(?:m3u8|mp4)[^\"]*)\"").matcher(rawHtml)
        if (plainUrlMatcher.find()) {
            return plainUrlMatcher.group(1)?.replace("\\/", "/")
        }

        return null
    }

    private fun parseDurationString(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val parts = text.trim().split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            else -> null
        }
    }

    private fun resolveAbsolute(baseUrl: String, relative: String): String {
        return try {
            val base = URI(baseUrl)
            base.resolve(relative).toString()
        } catch (_: Exception) {
            relative
        }
    }
}
