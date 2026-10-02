package com.example.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    fun formatDuration(seconds: Long?): String {
        if (seconds == null || seconds <= 0) return ""
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
        }
    }

    fun formatRelativeTime(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return "Never"
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 60_000) {
            return "Just now"
        }
        val minutes = diff / 60_000
        if (minutes < 60) {
            return "$minutes min ago"
        }
        val hours = minutes / 60
        if (hours < 24) {
            return "$hours hr${if (hours > 1) "s" else ""} ago"
        }
        val days = hours / 24
        if (days < 7) {
            return "$days day${if (days > 1) "s" else ""} ago"
        }

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatViewCount(views: Long?): String {
        if (views == null || views <= 0) return ""
        return when {
            views >= 1_000_000 -> String.format(Locale.US, "%.1fM views", views / 1_000_000.0)
            views >= 1_000 -> String.format(Locale.US, "%.1fK views", views / 1_000.0)
            else -> "$views views"
        }
    }
}
