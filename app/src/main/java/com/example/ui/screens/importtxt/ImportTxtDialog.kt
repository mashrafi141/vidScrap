package com.example.ui.screens.importtxt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.core.model.ParsedImportSummary
import com.example.core.util.TxtConfigParser
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentSecondary
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GlassBorderStroke
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.BufferedReader
import java.io.InputStreamReader

@Composable
fun ImportTxtDialog(
    onDismiss: () -> Unit,
    onConfirmImport: (content: String, fileName: String) -> Unit
) {
    val context = LocalContext.current
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf("config.txt") }
    var parsedSummary by remember { mutableStateOf<ParsedImportSummary?>(null) }
    var manualTextMode by remember { mutableStateOf(false) }
    var rawInputText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    fileContent = content
                    fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "imported_config.txt"
                    parsedSummary = TxtConfigParser.parse(content)
                }
            } catch (_: Exception) {
                // Safely handled
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (parsedSummary == null) "Import TXT Sources" else "Import Ready",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (parsedSummary == null) {
                    Text(
                        text = "Select a .txt file containing #playlist, #Single video, and #profile link entries, or paste the content directly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (!manualTextMode) {
                        Button(
                            onClick = { filePickerLauncher.launch("text/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentPrimary,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_txt_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileOpen,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select .TXT File")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { manualTextMode = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste TXT Directly", color = TextSecondary)
                        }
                    } else {
                        OutlinedTextField(
                            value = rawInputText,
                            onValueChange = { rawInputText = it },
                            label = { Text("Paste #playlist, #Single video, #profile links") },
                            placeholder = {
                                Text(
                                    "#playlist\nhttps://...\n\n#Single video\nhttps://...\n\n#profile link\nhttps://...",
                                    color = TextMuted
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = GlassBorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .testTag("paste_txt_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (rawInputText.isNotBlank()) {
                                    fileContent = rawInputText
                                    fileName = "pasted_sources.txt"
                                    parsedSummary = TxtConfigParser.parse(rawInputText)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = rawInputText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Analyze Content")
                        }
                    }
                } else {
                    // Summary display
                    val summary = parsedSummary!!

                    Text(
                        text = "File: $fileName",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SummaryRow(label = "Playlists", count = summary.playlistCount)
                    SummaryRow(label = "Single Videos", count = summary.singleVideoCount)
                    SummaryRow(label = "Profiles", count = summary.profileCount)

                    if (summary.duplicateCount > 0) {
                        SummaryRow(label = "Duplicates ignored", count = summary.duplicateCount)
                    }

                    if (summary.invalidLinesCount > 0) {
                        SummaryRow(label = "Invalid lines ignored", count = summary.invalidLinesCount, isWarning = true)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                parsedSummary = null
                                fileContent = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("import_cancel_button")
                        ) {
                            Text("Back", color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Button(
                            onClick = {
                                fileContent?.let { content ->
                                    onConfirmImport(content, fileName)
                                }
                            },
                            enabled = summary.totalValidCount > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentSuccess,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("import_confirm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import (${summary.totalValidCount})")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (parsedSummary == null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    count: Int,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isWarning) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = AccentSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isWarning) AccentSecondary else TextSecondary
            )
        }

        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = if (isWarning) AccentSecondary else TextPrimary
        )
    }
}
