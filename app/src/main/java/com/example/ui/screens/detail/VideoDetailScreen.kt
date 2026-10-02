package com.example.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.core.util.TimeFormatter
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTopAppBar
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VideoDetailScreen(
    videoId: Long,
    viewModel: VideoDetailViewModel,
    onBackClick: () -> Unit,
    onPlayClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    val video by viewModel.video.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopAppBar(
                title = "Video Details",
                onBackClick = onBackClick
            )

            if (video != null) {
                val item = video!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                        )
                ) {
                    // Hero Thumbnail Box with Play Action
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkSurfaceVariant)
                    ) {
                        if (!item.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = item.thumbnailUrl,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                    )
                                )
                        )

                        // Play Button Hero
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentPrimary.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                tint = TextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Title
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play Button Primary Action
                    Button(
                        onClick = { onPlayClick(item.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("detail_play_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Video",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Metadata Card
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (item.durationSeconds != null && item.durationSeconds!! > 0) {
                                MetadataRow(
                                    icon = Icons.Outlined.Schedule,
                                    label = "Duration",
                                    value = TimeFormatter.formatDuration(item.durationSeconds)
                                )
                            }

                            if (!item.uploaderName.isNullOrBlank()) {
                                MetadataRow(
                                    icon = Icons.Outlined.Person,
                                    label = "Uploader",
                                    value = item.uploaderName!!
                                )
                            }

                            if (item.views != null && item.views!! > 0) {
                                MetadataRow(
                                    icon = Icons.Outlined.Visibility,
                                    label = "Views",
                                    value = TimeFormatter.formatViewCount(item.views)
                                )
                            }

                            if (!item.publishedAt.isNullOrBlank()) {
                                MetadataRow(
                                    icon = Icons.Outlined.DateRange,
                                    label = "Published",
                                    value = item.publishedAt!!
                                )
                            }

                            MetadataRow(
                                icon = Icons.Outlined.Language,
                                label = "Source URL",
                                value = item.pageUrl
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Open Original Link in Browser
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.pageUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Original Page", color = TextSecondary)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading video details...", color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
