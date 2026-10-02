package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.model.SyncStatus
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.TextPrimary

@Composable
fun DurationBadge(
    durationText: String,
    modifier: Modifier = Modifier
) {
    if (durationText.isBlank()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.75f))
            .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = durationText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    label: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, dotColor) = when (status) {
        SyncStatus.IDLE -> Color(0x3364748B) to Color(0xFF94A3B8)
        SyncStatus.SYNCING -> Color(0x336366F1) to AccentPrimary
        SyncStatus.SUCCESS -> Color(0x3310B981) to AccentSuccess
        SyncStatus.ERROR -> Color(0x33EF4444) to AccentError
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.8.dp, dotColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary
        )
    }
}
