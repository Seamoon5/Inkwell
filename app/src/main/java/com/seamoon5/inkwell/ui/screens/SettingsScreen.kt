package com.seamoon5.inkwell.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seamoon5.inkwell.data.BackupManager
import com.seamoon5.inkwell.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onOpenVault: () -> Unit
) {
    val context = LocalContext.current
    val counts by viewModel.counts.collectAsState()
    val toast by viewModel.toast.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var confirmDeleteAll by remember { mutableStateOf(false) }
    var vaultTaps by remember { mutableIntStateOf(0) }
    var vaultUnlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(toast) { toast?.let { snackbarHostState.showSnackbar(it) } }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        viewModel.backupToFile(context, uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val parsed = BackupManager.readJson(context, uri)
        if (parsed == null) {
            viewModel.showToast("That file is not an Inkwell backup")
        } else {
            viewModel.importSnapshot(parsed.first, parsed.second)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("More", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle("Your library")
            InfoCard {
                InfoRow("Notes", "${counts.all}")
                InfoRow("Starred", "${counts.favorites}")
                InfoRow("Archived", "${counts.archive}")
                InfoRow("In Recycle Bin", "${counts.trash}")
            }

            Spacer(Modifier.height(18.dp))
            SectionTitle("Google backup")
            Text(
                text = "Save a full copy of every note to your Google Drive, and bring it back on any phone. Pick Drive in the file picker.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            ActionCard(
                icon = Icons.Default.CloudUpload,
                title = "Back up to Google Drive",
                subtitle = "Saves a .json file with all notes",
                onClick = { exportLauncher.launch("inkwell-backup.json") }
            )
            Spacer(Modifier.height(10.dp))
            ActionCard(
                icon = Icons.Default.CloudDownload,
                title = "Restore from backup",
                subtitle = "Reads an Inkwell .json backup file",
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Automatic backup: Google Photos and device backup on your phone also include Inkwell data once you have Google backup switched on in Android settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("Safety")
            ActionCard(
                icon = Icons.Default.DeleteForever,
                title = "Delete all notes",
                subtitle = "Removes every note permanently",
                onClick = { confirmDeleteAll = true }
            )

            Spacer(Modifier.height(18.dp))
            SectionTitle("Hidden")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        vaultTaps++
                        if (vaultTaps >= 5) {
                            vaultTaps = 0
                            vaultUnlocked = true
                        }
                    }
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(6.dp))
                Text("Inkwell", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("version 2.0", style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(30.dp))
        }
    }

    if (vaultUnlocked) {
        AlertDialog(
            onDismissRequest = { vaultUnlocked = false; pin = "" },
            title = { Text("Private Vault") },
            text = {
                Column {
                    Text("Vault notes stay out of the normal list and search.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("Any code, then Continue") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vaultUnlocked = false
                    pin = ""
                    onOpenVault()
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { vaultUnlocked = false; pin = "" }) { Text("Cancel") }
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("Delete all notes?") },
            text = { Text("This removes every note from this phone. It cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    viewModel.deleteAllNotes()
                }) { Text("Delete everything") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) { content() }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}