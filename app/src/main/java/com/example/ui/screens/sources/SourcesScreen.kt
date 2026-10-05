package com.example.ui.screens.sources

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.core.model.Video
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.model.Source
import com.example.core.model.SourceType
import com.example.core.util.TimeFormatter
import com.example.ui.components.EditProfileDialog
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.GlassCard
import com.example.ui.components.ProfileCard
import com.example.ui.components.SyncStatusBadge
import com.example.ui.components.XHamsterLoginDialog
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel,
    onNavigateToHome: () -> Unit,
    onSourceClick: (Long) -> Unit,
    onVideoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    val singleVideos by viewModel.singleVideos.collectAsStateWithLifecycle()
    val syncingSourceId by viewModel.syncingSourceId.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    var showLoginDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Source?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Sources",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Manage and synchronize your imported links",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = AccentPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                        color = AccentPrimary
                    )
                },
                divider = {},
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                SourceType.entries.forEach { type ->
                    val isSelected = selectedTab == type
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.onTabSelected(type) },
                        text = {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) TextPrimary else TextMuted
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status notification banner
            AnimatedVisibility(visible = statusMessage != null) {
                statusMessage?.let { msg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
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
                                androidx.compose.material3.Button(
                                    onClick = { showLoginDialog = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
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

            // Source list or Single Videos grid
            if (selectedTab == SourceType.SINGLE_VIDEO) {
                if (singleVideos.isEmpty()) {
                    EmptyLibraryState(
                        title = "No single videos imported",
                        description = "Import a TXT file containing #Single video links.",
                        icon = Icons.Outlined.VideoLibrary,
                        onImportClick = onNavigateToHome,
                        buttonText = "Go to Import",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 300.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = singleVideos, key = { it.id }) { video ->
                            com.example.ui.components.VideoCard(
                                video = video,
                                onClick = { onVideoClick(video.id) }
                            )
                        }
                    }
                }
            } else if (selectedTab == SourceType.PROFILE) {
                // Profile collections in a 2-column box grid with custom DP and long press to edit
                if (sources.isEmpty()) {
                    EmptyLibraryState(
                        title = "No profiles imported",
                        description = "Import a TXT file containing #profile link entries to track channel uploads.",
                        icon = Icons.Outlined.Person,
                        onImportClick = onNavigateToHome,
                        buttonText = "Go to Import",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items = sources, key = { it.id }) { profile ->
                            ProfileCard(
                                source = profile,
                                onClick = { onSourceClick(profile.id) },
                                onLongClick = { editingProfile = profile }
                            )
                        }
                    }
                }
            } else {
                // Playlists collections
                if (sources.isEmpty()) {
                    EmptyLibraryState(
                        title = "No playlists imported",
                        description = "Import a TXT file containing #playlist links to add playlist sources.",
                        icon = Icons.Outlined.QueueMusic,
                        onImportClick = onNavigateToHome,
                        buttonText = "Go to Import",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 10.dp,
                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sources, key = { it.id }) { source ->
                            SourceCard(
                                source = source,
                                isSyncing = syncingSourceId == source.id,
                                onCardClick = { onSourceClick(source.id) },
                                onSyncClick = { viewModel.syncSource(source.id) },
                                onDeleteClick = { viewModel.deleteSource(source.id) }
                            )
                        }
                    }
                }
            }
        }

        editingProfile?.let { profile ->
            EditProfileDialog(
                source = profile,
                onDismiss = { editingProfile = null },
                onSave = { newName, newAvatarUri ->
                    viewModel.updateProfileDetails(profile.id, newName, newAvatarUri)
                    editingProfile = null
                }
            )
        }

        if (showLoginDialog) {
            XHamsterLoginDialog(
                onDismiss = { showLoginDialog = false },
                onLoginSuccess = {
                    showLoginDialog = false
                    syncingSourceId?.let { viewModel.syncSource(it) }
                }
            )
        }
    }
}

@Composable
fun SourceCard(
    source: Source,
    isSyncing: Boolean,
    onCardClick: () -> Unit,
    onSyncClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("source_card_${source.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onCardClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (source.type) {
                            SourceType.PLAYLIST -> Icons.Outlined.QueueMusic
                            SourceType.SINGLE_VIDEO -> Icons.Outlined.VideoLibrary
                            SourceType.PROFILE -> Icons.Outlined.Person
                        },
                        contentDescription = null,
                        tint = AccentPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name ?: source.url,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = source.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = onSyncClick,
                            modifier = Modifier.size(36.dp).testTag("sync_source_${source.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Sync",
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp).testTag("delete_source_${source.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                            tint = AccentError.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${source.videoCount} local video${if (source.videoCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                val syncLabel = when {
                    isSyncing -> "Syncing..."
                    source.lastError != null -> "Failed"
                    source.lastSuccessfulSyncAt != null -> "Synced ${TimeFormatter.formatRelativeTime(source.lastSuccessfulSyncAt)}"
                    else -> "Never synced"
                }

                SyncStatusBadge(
                    status = if (isSyncing) com.example.core.model.SyncStatus.SYNCING else source.syncStatus,
                    label = syncLabel
                )
            }
        }
    }
}
