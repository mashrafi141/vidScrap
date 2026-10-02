package com.example.core.model

sealed class ScrapeResult {
    data class Success(
        val sourceName: String? = null,
        val videos: List<Video>,
        val newVideosCount: Int = 0,
        val updatedVideosCount: Int = 0
    ) : ScrapeResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
        val isRetryable: Boolean = true
    ) : ScrapeResult()
}
