package com.example

import com.example.core.model.SourceType
import com.example.core.util.TxtConfigParser
import com.example.core.util.UrlUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TxtConfigParserTest {

    @Test
    fun parseTxtFile_extractsCategoriesCorrectly() {
        val sampleTxt = """
            #playlist
            https://xhamster.com/playlists/favorite-collection-123
            https://xhamster.com/playlists/top-picks-456

            #Single video
            https://xhamster.com/videos/awesome-clip-7890123
            https://xhamster.com/videos/nature-documentary-4567890

            #profile link
            https://xhamster.com/users/creator-channel
        """.trimIndent()

        val summary = TxtConfigParser.parse(sampleTxt)

        assertEquals(2, summary.playlistCount)
        assertEquals(2, summary.singleVideoCount)
        assertEquals(1, summary.profileCount)
        assertEquals(5, summary.totalValidCount)
        assertEquals(0, summary.invalidLinesCount)
        assertEquals(0, summary.duplicateCount)

        assertEquals(SourceType.PLAYLIST, summary.items[0].type)
        assertEquals(SourceType.SINGLE_VIDEO, summary.items[2].type)
        assertEquals(SourceType.PROFILE, summary.items[4].type)
    }

    @Test
    fun parseTxtFile_handlesDuplicatesAndInvalidLines() {
        val sampleTxt = """
            #playlist
            https://xhamster.com/playlists/playlist-one
            https://xhamster.com/playlists/playlist-one
            not-a-valid-url

            #Single video
            https://xhamster.com/videos/video-12345
        """.trimIndent()

        val summary = TxtConfigParser.parse(sampleTxt)

        assertEquals(1, summary.playlistCount)
        assertEquals(1, summary.singleVideoCount)
        assertEquals(1, summary.duplicateCount)
        assertEquals(1, summary.invalidLinesCount)
        assertEquals(2, summary.totalValidCount)
    }

    @Test
    fun urlUtils_normalizesAndExtractsRemoteId() {
        val url = "https://XHamster.com/videos/cool-video-title-9876543/?utm_source=twitter"
        val normalized = UrlUtils.normalizeUrl(url)

        assertTrue(normalized.startsWith("https://xhamster.com/videos/cool-video-title-9876543"))
        assertFalse(normalized.contains("utm_source"))

        val remoteId = UrlUtils.extractRemoteId(url)
        assertEquals("9876543", remoteId)
    }

    @Test
    fun urlUtils_validatesUrlsAccurately() {
        assertTrue(UrlUtils.isValidUrl("https://xhamster.com/videos/123"))
        assertTrue(UrlUtils.isValidUrl("http://example.com/playlist"))
        assertFalse(UrlUtils.isValidUrl("random text"))
        assertFalse(UrlUtils.isValidUrl("ftp://files.example.com"))
        assertFalse(UrlUtils.isValidUrl(""))
    }

    @Test
    fun urlUtils_preservesValidUrlsWithoutCorruptingPath() {
        val favUrl = "https://xhamster.com/my/favorites/videos/6aaab14cc229e5b140045275-dick-ridder"
        val normalized = UrlUtils.normalizeUrl(favUrl)
        assertEquals("https://xhamster.com/my/favorites/videos/6aaab14cc229e5b140045275-dick-ridder", normalized)
    }
}
