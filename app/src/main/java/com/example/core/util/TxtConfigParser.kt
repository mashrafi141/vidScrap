package com.example.core.util

import com.example.core.model.ParsedImportItem
import com.example.core.model.ParsedImportSummary
import com.example.core.model.SourceType

object TxtConfigParser {

    /**
     * Parses the contents of a user-provided .txt configuration file.
     * Respects case-insensitive headers:
     * #playlist
     * #Single video
     * #profile link
     */
    fun parse(content: String): ParsedImportSummary {
        val lines = content.lines()
        var currentSection: SourceType? = null
        val items = mutableListOf<ParsedImportItem>()
        val seenUrls = mutableSetOf<String>()

        var playlistCount = 0
        var singleVideoCount = 0
        var profileCount = 0
        var invalidLinesCount = 0
        var duplicateCount = 0

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) {
                continue
            }

            // Check if this line is a section header
            if (line.startsWith("#")) {
                val detectedSection = SourceType.fromHeader(line)
                if (detectedSection != null) {
                    currentSection = detectedSection
                } else {
                    // Unknown header or comment, safely ignore or mark
                    invalidLinesCount++
                }
                continue
            }

            // If we have an active section, process URL
            if (currentSection != null) {
                if (UrlUtils.isValidUrl(line)) {
                    val normalized = UrlUtils.normalizeUrl(line)
                    if (seenUrls.contains(normalized)) {
                        duplicateCount++
                    } else {
                        seenUrls.add(normalized)
                        val item = ParsedImportItem(type = currentSection, url = normalized)
                        items.add(item)
                        when (currentSection) {
                            SourceType.PLAYLIST -> playlistCount++
                            SourceType.SINGLE_VIDEO -> singleVideoCount++
                            SourceType.PROFILE -> profileCount++
                        }
                    }
                } else {
                    invalidLinesCount++
                }
            } else {
                // URL without preceding header, attempt to infer or mark as invalid
                if (UrlUtils.isValidUrl(line)) {
                    val normalized = UrlUtils.normalizeUrl(line)
                    val inferred = inferTypeFromUrl(normalized)
                    if (seenUrls.contains(normalized)) {
                        duplicateCount++
                    } else {
                        seenUrls.add(normalized)
                        val item = ParsedImportItem(type = inferred, url = normalized)
                        items.add(item)
                        when (inferred) {
                            SourceType.PLAYLIST -> playlistCount++
                            SourceType.SINGLE_VIDEO -> singleVideoCount++
                            SourceType.PROFILE -> profileCount++
                        }
                    }
                } else {
                    invalidLinesCount++
                }
            }
        }

        return ParsedImportSummary(
            items = items,
            playlistCount = playlistCount,
            singleVideoCount = singleVideoCount,
            profileCount = profileCount,
            invalidLinesCount = invalidLinesCount,
            duplicateCount = duplicateCount
        )
    }

    private fun inferTypeFromUrl(url: String): SourceType {
        val lower = url.lowercase()
        return when {
            lower.contains("/playlist") -> SourceType.PLAYLIST
            lower.contains("/user") || lower.contains("/profile") || lower.contains("/channel") -> SourceType.PROFILE
            else -> SourceType.SINGLE_VIDEO
        }
    }
}
