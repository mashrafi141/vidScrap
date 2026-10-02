package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.model.Video
import com.example.ui.components.EmptyLibraryState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.VideoCard
import com.example.ui.screens.importtxt.ImportTxtDialog
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorderStroke
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onVideoClick: (Long) -> Unit,
    onSourceClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val videos by viewModel.displayedVideos.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val allSources by viewModel.allSources.collectAsStateWithLifecycle()
    val playlistSources by viewModel.playlistSources.collectAsStateWithLifecycle()
    val profileSources by viewModel.profileSources.collectAsStateWithLifecycle()
    val singleVideoSources by viewModel.singleVideoSources.collectAsStateWithLifecycle()
    val recentVideos by viewModel.recentVideos.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncSummary by viewModel.syncSummary.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val showImportDialog by viewModel.showImportDialog.collectAsStateWithLifecycle()

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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Video Library",
                        style = MaterialTheme.typography.displayMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = if (totalCount > 0) "$totalCount videos • ${allSources.size} sources" else "Personal collection",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = AccentPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else if (allSources.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.syncAllSources() },
                            modifier = Modifier.testTag("home_sync_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Sync All",
                                tint = AccentPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.openImportDialog() },
                        modifier = Modifier.testTag("home_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileUpload,
                            contentDescription = "Import TXT",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Sync Summary Notification Banner
            AnimatedVisibility(visible = syncSummary != null) {
                syncSummary?.let { summary ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = Color(0x3310B981)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sync complete: ${summary.newVideos} new • ${summary.updatedVideos} updated" +
                                        if (summary.failedSources > 0) " • ${summary.failedSources} failed" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )

                            IconButton(
                                onClick = { viewModel.clearSyncSummary() },
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
                    }
                }
            }

            // Error Banner
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let { error ->
                    ErrorBanner(
                        errorMessage = error,
                        onRetry = { viewModel.syncAllSources() },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
            }

            // Search Bar (if videos exist or query entered)
            if (totalCount > 0 || searchQuery.isNotBlank()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    placeholder = { Text("Search by title or uploader...", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = TextMuted
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPrimary,
                        unfocusedBorderColor = GlassBorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.6f),
                        unfocusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("home_search_input")
                )

                // Category Tabs / Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(HomeCategoryFilter.entries) { filter ->
                        val isSelected = categoryFilter == filter
                        val bg = if (isSelected) AccentPrimary.copy(alpha = 0.25f) else Color.Transparent
                        val border = if (isSelected) AccentPrimary else GlassBorderStroke
                        val textCol = if (isSelected) TextPrimary else TextSecondary

                        val countLabel = when (filter) {
                            HomeCategoryFilter.ALL -> totalCount
                            HomeCategoryFilter.RECENT -> recentVideos.size
                            HomeCategoryFilter.PLAYLISTS -> playlistSources.size
                            HomeCategoryFilter.PROFILES -> profileSources.size
                            HomeCategoryFilter.SINGLE_VIDEOS -> singleVideoSources.size
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(12.dp))
                                .clickable { viewModel.onCategoryFilterChange(filter) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${filter.label} ($countLabel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = textCol
                            )
                        }
                    }
                }
            }

            // Main Content Body based on selected category filter
            if (totalCount == 0 && searchQuery.isBlank()) {
                EmptyLibraryState(
                    onImportClick = { viewModel.openImportDialog() },
                    modifier = Modifier.weight(1f)
                )
            } else {
                when (categoryFilter) {
                    HomeCategoryFilter.PLAYLISTS -> {
                        // Display Playlists list with tap to open
                        if (playlistSources.isEmpty()) {
                            EmptyLibraryState(
                                title = "No playlists imported",
                                description = "Import a TXT file containing #playlist links.",
                                icon = Icons.Outlined.QueueMusic,
                                onImportClick = { viewModel.openImportDialog() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(
                                    top = 6.dp,
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(playlistSources, key = { it.id }) { playlist ->
                                    HomeSourceCard(
                                        source = playlist,
                                        onClick = { onSourceClick(playlist.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeCategoryFilter.PROFILES -> {
                        // Display Profiles list with tap to open
                        if (profileSources.isEmpty()) {
                            EmptyLibraryState(
                                title = "No profiles imported",
                                description = "Import a TXT file containing #profile link entries.",
                                icon = Icons.Outlined.Person,
                                onImportClick = { viewModel.openImportDialog() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(
                                    top = 6.dp,
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(profileSources, key = { it.id }) { profile ->
                                    HomeSourceCard(
                                        source = profile,
                                        onClick = { onSourceClick(profile.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeCategoryFilter.SINGLE_VIDEOS -> {
                        // Display Single Videos sources with tap to open
                        if (singleVideoSources.isEmpty()) {
                            EmptyLibraryState(
                                title = "No single videos imported",
                                description = "Import a TXT file containing #Single video links.",
                                icon = Icons.Outlined.VideoLibrary,
                                onImportClick = { viewModel.openImportDialog() },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(
                                    top = 6.dp,
                                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(singleVideoSources, key = { it.id }) { singleSource ->
                                    HomeSourceCard(
                                        source = singleSource,
                                        onClick = { onSourceClick(singleSource.id) }
                                    )
                                }
                            }
                        }
                    }

                    HomeCategoryFilter.RECENT -> {
                        // Display Recent Videos Grid
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 6.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(recentVideos, key = { it.id }) { video ->
                                VideoCard(
                                    video = video,
                                    onClick = { onVideoClick(video.id) }
                                )
                            }
                        }
                    }

                    HomeCategoryFilter.ALL -> {
                        // Full Video Grid
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 300.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(
                                top = 6.dp,
                                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp
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
                }
            }
        }

        // Import TXT Dialog
        if (showImportDialog) {
            ImportTxtDialog(
                onDismiss = { viewModel.closeImportDialog() },
                onConfirmImport = { content, fileName ->
                    viewModel.importTxt(content, fileName)
                }
            )
        }
    }
}

@Composable
fun HomeSourceCard(
    source: Source,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_source_card_${source.id}"),
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name ?: source.url,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${source.videoCount} video${if (source.videoCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentPrimary
                )
                Text(
                    text = source.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Outlined.VideoLibrary,
                contentDescription = "View Videos",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
