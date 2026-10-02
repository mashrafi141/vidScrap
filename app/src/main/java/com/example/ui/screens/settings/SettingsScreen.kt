package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlassCard
import com.example.ui.components.XHamsterLoginDialog
import com.example.ui.screens.importtxt.ImportTxtDialog
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorderStroke
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val isBgSyncEnabled by viewModel.isBackgroundSyncEnabled.collectAsStateWithLifecycle()
    val syncInterval by viewModel.syncIntervalHours.collectAsStateWithLifecycle()
    val isWifiOnly by viewModel.isWifiOnly.collectAsStateWithLifecycle()
    val lastConfig by viewModel.lastConfigName.collectAsStateWithLifecycle()
    val cacheSize by viewModel.thumbnailCacheSize.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isUpdatingTools by viewModel.isUpdatingTools.collectAsStateWithLifecycle()
    val toolsVersion by viewModel.toolsVersion.collectAsStateWithLifecycle()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showXHamsterLoginDialog by remember { mutableStateOf(false) }

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
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Preferences, sync schedule, and cache storage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            AnimatedVisibility(visible = statusMessage != null) {
                statusMessage?.let { msg ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = msg, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            IconButton(
                                onClick = { viewModel.clearStatusMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Library & Sources
                Text(
                    text = "LIBRARY & CONFIGURATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Active Configuration",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = lastConfig ?: "No configuration imported",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { showImportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPrimary,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Outlined.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import / Replace")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Thumbnail Cache size & clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Thumbnail Cache",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Disk usage: $cacheSize",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearThumbnailCache() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextSecondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Cache", color = TextSecondary)
                            }
                        }
                    }
                }

                // Section: XHamster Session & Personal Playlists
                Text(
                    text = "XHAMSTER ACCOUNT & PLAYLISTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Account Session",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Sign in to synchronize private /my/favorites/ playlists",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Button(
                                onClick = { showXHamsterLoginDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentPrimary,
                                    contentColor = TextPrimary
                                )
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In")
                            }
                        }
                    }
                }

                // Section: Tools & Scraper Engine Update
                Text(
                    text = "TOOLS & ENGINE DEFINITIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Scraper & Playback Tools",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = toolsVersion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = com.example.ui.theme.AccentSuccess
                                )
                            }

                            if (isUpdatingTools) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = AccentPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.updateAllTools() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentPrimary,
                                        contentColor = TextPrimary
                                    )
                                ) {
                                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Update Tools")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Updates parsing rules, video resolvers, user-agent profiles, and network filters to prevent platform errors.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Section: Background Synchronization
                Text(
                    text = "BACKGROUND SYNCHRONIZATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Periodic Sync",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Automatically check playlists & profiles in background",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Switch(
                                checked = isBgSyncEnabled,
                                onCheckedChange = { viewModel.setBackgroundSyncEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TextPrimary,
                                    checkedTrackColor = AccentPrimary
                                ),
                                modifier = Modifier.testTag("bg_sync_switch")
                            )
                        }

                        if (isBgSyncEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wi-Fi Only",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Conserve mobile data",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                Switch(
                                    checked = isWifiOnly,
                                    onCheckedChange = { viewModel.setWifiOnly(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = AccentPrimary
                                    )
                                )
                            }

                            Column {
                                Text(
                                    text = "Sync Interval",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(6, 12, 24).forEach { hours ->
                                        FilterChip(
                                            selected = syncInterval == hours,
                                            onClick = { viewModel.setSyncIntervalHours(hours) },
                                            label = { Text("$hours hrs") },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = AccentPrimary,
                                                selectedLabelColor = TextPrimary,
                                                labelColor = TextSecondary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section: Privacy & Danger Zone
                Text(
                    text = "DATA & PRIVACY",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = AccentPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "100% Local Storage",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "All metadata, thumbnail caches, and configuration links are saved strictly on this device. No user accounts, analytics, or cloud tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Button(
                            onClick = { showClearConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentError.copy(alpha = 0.2f),
                                contentColor = AccentError
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("clear_all_data_button")
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear All Local Data")
                        }
                    }
                }

                // About card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Personal Video Library", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(text = "Version 1.0.0 • Open Source (MIT)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                }
            }
        }

        // Clear confirmation dialog
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                title = { Text("Clear All Data?", color = TextPrimary) },
                text = {
                    Text(
                        "This will delete all saved playlists, video records, thumbnails, and reset the library configuration. This action cannot be undone.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllLibraryData()
                            showClearConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentError)
                    ) {
                        Text("Delete Everything")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurfaceVariant
            )
        }

        // Import TXT Dialog
        if (showImportDialog) {
            ImportTxtDialog(
                onDismiss = { showImportDialog = false },
                onConfirmImport = { content, fileName ->
                    viewModel.importTxtConfig(content, fileName)
                    showImportDialog = false
                }
            )
        }

        // XHamster Account Login Dialog
        if (showXHamsterLoginDialog) {
            XHamsterLoginDialog(
                onDismiss = { showXHamsterLoginDialog = false },
                onLoginSuccess = {
                    showXHamsterLoginDialog = false
                    viewModel.updateAllTools()
                }
            )
        }
    }
}
