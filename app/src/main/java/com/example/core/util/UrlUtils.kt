package com.example.core.util

import java.net.URI
import java.security.MessageDigest

object UrlUtils {

    fun isValidUrl(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        return try {
            val uri = URI(trimmed)
            !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeUrl(url: String): String {
        val processed = url.trim()
        return try {
            val uri = URI(processed)
            val scheme = (uri.scheme ?: "https").lowercase()
            val host = (uri.host ?: "").lowercase()
            val port = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            var path = uri.rawPath ?: ""
            if (path.isEmpty()) path = "/"
            // Remove trailing slash for uniformity except root
            if (path.length > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length - 1)
            }
            // Strip common analytics query parameters
            val cleanQuery = uri.rawQuery?.split("&")?.filterNot { param ->
                val p = param.lowercase()
                p.startsWith("utm_") || p.startsWith("ref=") || p.startsWith("source=")
            }?.joinToString("&")

            val queryString = if (!cleanQuery.isNullOrEmpty()) "?$cleanQuery" else ""
            "$scheme://$host$port$path$queryString"
        } catch (_: Exception) {
            url.trim()
        }
    }

    /**
     * Extracts a stable remote identifier from XHamster or generic video URLs.
     * E.g. https://xhamster.com/videos/amazing-video-title-1234567 -> 1234567
     * or https://xhamster.com/videos/1234567 -> 1234567
     */
    fun extractRemoteId(url: String): String {
        val cleanUrl = normalizeUrl(url)
        val path = try {
            URI(cleanUrl).path ?: cleanUrl
        } catch (_: Exception) {
            cleanUrl
        }
        val segments = path.split("/").filter { it.isNotBlank() }
        if (segments.isNotEmpty()) {
            val lastSegment = segments.last()
            // Check if last segment ends with a number (common in xhamster: title-xyz123456)
            val trailingDigitsMatch = Regex("(\\d{4,})$").find(lastSegment)
            if (trailingDigitsMatch != null) {
                return trailingDigitsMatch.value
            }
            val idMatch = Regex("([a-zA-Z0-9_-]{4,})").find(lastSegment)
            if (idMatch != null) {
                return idMatch.value
            }
            return lastSegment
        }
        return hashUrl(cleanUrl)
    }

    fun hashUrl(url: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }
}
