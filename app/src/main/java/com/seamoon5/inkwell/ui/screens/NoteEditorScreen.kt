package com.seamoon5.inkwell.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.seamoon5.inkwell.data.Checklist
import com.seamoon5.inkwell.data.Note
import com.seamoon5.inkwell.data.NoteImage
import com.seamoon5.inkwell.ui.theme.NoteColors
import com.seamoon5.inkwell.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    onBack: () -> Unit,
    onShare: (String) -> Unit
) {
    val stored by viewModel.currentNote.collectAsState()
    val images by viewModel.currentImages.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0) }
    var isVault by remember { mutableStateOf(false) }
    var checklistMode by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<Checklist.Item>() }
    var loadedId by remember { mutableStateOf(0L) }
    var sheet by remember { mutableStateOf(false) }
    var vaultMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    LaunchedEffect(stored?.id) {
        val note = stored ?: return@LaunchedEffect
        if (loadedId != note.id) {
            loadedId = note.id
            title = note.title
            content = note.content
            tags = note.tags
            color = note.color
            isVault = note.vault
            checklistItems.clear()
            checklistItems.addAll(Checklist.parse(note.checklist))
            checklistMode = checklistItems.isNotEmpty()
        }
    }

    LaunchedEffect(toast) { toast?.let { snackbarHostState.showSnackbar(it) } }

    val noteColor = NoteColors[color % NoteColors.size]
    val ink = Color.Black.copy(alpha = 0.87f)
    val words = remember(content, title) { countWords(title, content, checklistItems.toList()) }

    fun workingNote() = (stored ?: Note()).copy(
        title = title.trim(),
        content = content,
        tags = NoteViewModel.normalizeTags(tags),
        color = color,
        vault = isVault,
        checklist = Checklist.serialize(checklistItems.toList()),
        updatedAt = System.currentTimeMillis()
    )

    fun commit() {
        val note = workingNote()
        viewModel.autoSave(note)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(6)
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        val targetId = loadedId
        if (targetId == 0L) {
            viewModel.saveNote(workingNote()) { newId ->
                loadedId = newId
                viewModel.addImages(newId, uris.map { it.toString() })
            }
        } else {
            viewModel.addImages(targetId, uris.map { it.toString() })
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isVault) "Private Note" else if (noteId == 0L) "New Note" else "Edit Note",
                        color = ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ink)
                    }
                },
                actions = {
                    IconButton(onClick = { commit(); onBack() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = ink)
                    }
                    Box {
                        IconButton(onClick = { vaultMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Tools", tint = ink)
                        }
                        DropdownMenu(vaultMenu, onDismissRequest = { vaultMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isVault) "Remove from Private Vault" else "Move to Private Vault") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                onClick = {
                                    vaultMenu = false
                                    isVault = !isVault
                                    commit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (checklistMode) "Turn off checklist" else "Turn on checklist") },
                                leadingIcon = { Icon(Icons.Default.Checklist, null) },
                                onClick = {
                                    vaultMenu = false
                                    checklistMode = !checklistMode
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (stored?.archived == true) "Remove from archive" else "Archive note") },
                                leadingIcon = { Icon(Icons.Default.Archive, null) },
                                onClick = {
                                    vaultMenu = false
                                    stored?.let { note ->
                                        if (note.archived) viewModel.unarchive(note) else viewModel.archive(note)
                                    }
                                    commit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Recycle Bin") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    vaultMenu = false
                                    commit()
                                    stored?.let { viewModel.trash(it) }
                                    onBack()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = noteColor,
                    titleContentColor = ink,
                    navigationIconContentColor = ink,
                    actionIconContentColor = ink
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(noteColor)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it; commit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.45f)) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = ink),
                colors = transparentField(),
                singleLine = true
            )

            TextField(
                value = tags,
                onValueChange = { tags = it; commit() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("tags, comma separated", style = MaterialTheme.typography.bodySmall, color = Color.Black.copy(alpha = 0.4f)) },
                textStyle = MaterialTheme.typography.bodySmall.copy(color = ink),
                colors = transparentField(),
                singleLine = true
            )

            if (checklistMode) {
                ChecklistEditor(
                    items = checklistItems,
                    ink = ink,
                    onToggle = { idx ->
                        val updated = Checklist.toggle(checklistItems.toList(), idx)
                        checklistItems.clear(); checklistItems.addAll(updated); commit()
                    },
                    onRemove = { idx ->
                        val updated = Checklist.removeAt(checklistItems.toList(), idx)
                        checklistItems.clear(); checklistItems.addAll(updated); commit()
                    },
                    onAdd = { text ->
                        val updated = Checklist.add(checklistItems.toList(), text)
                        checklistItems.clear(); checklistItems.addAll(updated); commit()
                    }
                )
            } else {
                TextField(
                    value = content,
                    onValueChange = { content = it; commit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Start writing...", style = MaterialTheme.typography.bodyLarge, color = Color.Black.copy(alpha = 0.45f)) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                    colors = transparentField()
                )
            }

            if (images.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images, key = { it.id }) { img ->
                        ImageChip(img, ink) { viewModel.deleteImage(img) }
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(noteColor.copy(alpha = 0.75f))
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$words words",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) { Icon(Icons.Default.AddPhotoAlternate, "Insert image", modifier = Modifier.size(20.dp), tint = ink) }
                    IconButton(onClick = { sheet = true }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.TextFields, "Tools", modifier = Modifier.size(20.dp), tint = ink)
                    }
                }

                Text(
                    "Color",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(NoteColors.size) { index ->
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(NoteColors[index])
                                .border(2.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
                                .then(
                                    if (index == color) Modifier.border(3.dp, Color.Black, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = index; commit() }
                        )
                    }
                }
            }
        }
    }

    if (sheet) {
        ModalBottomSheet(
            onDismissRequest = { sheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ToolsSheet(
                words = words,
                chars = (title + content).length,
                onInsertDate = {
                    content += SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
                    sheet = false; commit()
                },
                onCopy = { sheet = false },
                onShare = {
                    sheet = false
                    onShare(shareText(title, content, checklistItems.toList(), tags))
                }
            )
        }
    }
}

@Composable
private fun transparentField() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)

@Composable
private fun ChecklistEditor(
    items: List<Checklist.Item>,
    ink: Color,
    onToggle: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        LazyColumn(Modifier.weight(1f)) {
            itemsIndexed(items) { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.done,
                        onCheckedChange = { onToggle(index) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color.Black.copy(alpha = 0.6f),
                            uncheckedColor = Color.Black.copy(alpha = 0.6f)
                        )
                    )
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ink,
                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(
                            Icons.Default.Close, "Remove item",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add item", color = Color.Black.copy(alpha = 0.45f)) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                colors = transparentField(),
                singleLine = true
            )
            IconButton(onClick = { onAdd(draft); draft = "" }) {
                Icon(Icons.Default.Add, "Add item", tint = ink)
            }
        }
    }
}

@Composable
private fun ImageChip(image: NoteImage, ink: Color, onDelete: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.12f))
    ) {
        Text(
            text = "IMG ${image.position + 1}",
            style = MaterialTheme.typography.labelSmall,
            color = ink,
            modifier = Modifier.align(Alignment.Center)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
        ) {
            Icon(
                Icons.Default.Close, "Remove image",
                modifier = Modifier.size(14.dp),
                tint = Color.Black.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ToolsSheet(
    words: Int,
    chars: Int,
    onInsertDate: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        ToolRow("Word count", "$words words")
        ToolRow("Character count", "$chars characters")
        ToolRow("Reading time", "${(words / 200).coerceAtLeast(1)} min read")
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(Modifier.height(6.dp))
        SheetAction(Icons.Default.DateRange, "Insert date and time", onInsertDate)
        SheetAction(Icons.Default.Share, "Share note", onShare)
        SheetAction(Icons.Default.ContentCopy, "Copy statistics", onCopy)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ToolRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun countWords(title: String, content: String, items: List<Checklist.Item>): Int {
    val text = buildString {
        append(title); append(' '); append(content)
        items.forEach { append(' '); append(it.text) }
    }
    return text.split(Regex("\\s+")).count { it.isNotBlank() }
}

private fun shareText(
    title: String,
    content: String,
    items: List<Checklist.Item>,
    tags: String
): String = buildString {
    if (title.isNotBlank()) { append(title); append("\n\n") }
    if (content.isNotBlank()) { append(content); append("\n\n") }
    if (items.isNotEmpty()) { append(Checklist.toText(items)); append("\n\n") }
    if (tags.isNotBlank()) {
        append("Tags: ")
        append(tags.split(",").filter { it.isNotBlank() }.joinToString(" ") { "#$it" })
    }
}.trim()