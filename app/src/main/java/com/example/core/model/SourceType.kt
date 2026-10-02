package com.example.core.model

enum class SourceType(val displayName: String) {
    PLAYLIST("Playlists"),
    SINGLE_VIDEO("Single Videos"),
    PROFILE("Profiles");

    companion object {
        fun fromHeader(header: String): SourceType? {
            val cleaned = header.trim().removePrefix("#").trim().lowercase()
            return when {
                cleaned.startsWith("playlist") -> PLAYLIST
                cleaned.startsWith("single video") || cleaned == "video" || cleaned == "videos" -> SINGLE_VIDEO
                cleaned.startsWith("profile") || cleaned.startsWith("channel") || cleaned == "user" || cleaned == "users" -> PROFILE
                else -> null
            }
        }
    }
}
