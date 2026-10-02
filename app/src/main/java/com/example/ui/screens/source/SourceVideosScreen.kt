package com.example.ui.screens.source

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.SourceType
import com.example.core.model.SyncStatus
import com.example.core.util.TimeFormatter
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTopAppBar
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.VideoCard
import com.example.ui.components.XHamsterLoginDialog
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SourceVideosScreen(
    sourceId: Long,
    viewModel: SourceVideosViewModel,
    onBackClick: () -> Unit,
    onVideoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LaunchedEffect(sourceId) {
        viewModel.loadSource(sourceId)
    }

    val source by viewModel.source.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    var showLoginDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassTopAppBar(
                title = source?.name ?: "Source Videos",
                onBackClick = onBackClick
            )

            if (source != null) {
                val currentSource = source!!

                // Status message notification banner
                AnimatedVisibility(visible = statusMessage != null) {
                    statusMessage?.let { msg ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.clearStatusMessage() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Dismiss",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                if (msg.contains("Sign In") || msg.contains("account") || msg.contains("login")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showLoginDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPrimary,
                                            contentColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Sign In to XHamster")
                                    }
                                }
                            }
                        }
                    }
                }

                // Header Info Card
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(DarkSurfaceVariant, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (currentSource.type) {
                                            SourceType.PLAYLIST -> Icons.Outlined.QueueMusic
                                            SourceType.SINGLE_VIDEO -> Icons.Outlined.VideoLibrary
                                            SourceType.PROFILE -> Icons.Outlined.Person
                                        },
                                        contentDescription = null,
                                        tint = AccentPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "${videos.size} video${if (videos.size == 1) "" else "s"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = currentSource.type.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = AccentPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.syncCurrentSource() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = AccentPrimary,
                                            contentColor = TextPrimary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp).testTag("sync_source_button")
                                    ) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sync", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentSource.url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.OpenInNew,
                                        contentDescription = "Open Source Link",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentSource.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            val syncLabel = when {
                                isSyncing -> "Syncing..."
                                currentSource.lastError != null -> "Failed"
                                currentSource.lastSuccessfulSyncAt != null -> "Synced ${TimeFormatter.formatRelativeTime(currentSource.lastSuccessfulSyncAt)}"
                                else -> "Never synced"
                            }

                            SyncStatusBadge(
                                status = if (isSyncing) SyncStatus.SYNCING else currentSource.syncStatus,
                                label = syncLabel
                            )
                        }
                    }
                }

                // Video Grid or Empty State
                if (videos.isEmpty()) {
                    EmptyLibraryState(
                        title = "No videos synced for this source",
                        description = if (isSyncing) "Syncing videos from ${currentSource.name ?: "source"}..." else "Tap Sync to retrieve all videos from this source.",
                        buttonText = "Sync Source Now",
                        onImportClick = { viewModel.syncCurrentSource() },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 6.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(videos, key = { it.id }) { video ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentPrimary)
                }
            }
        }

        if (showLoginDialog) {
            XHamsterLoginDialog(
                onDismiss = { showLoginDialog = false },
                onLoginSuccess = {
                    showLoginDialog = false
                    viewModel.syncCurrentSource()
                }
            )
        }
    }
}
