package com.seamoon5.inkwell.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seamoon5.inkwell.data.Checklist
import com.seamoon5.inkwell.data.Note
import com.seamoon5.inkwell.ui.theme.NoteColors
import com.seamoon5.inkwell.ui.viewmodel.NoteSection
import com.seamoon5.inkwell.ui.viewmodel.NoteViewModel
import com.seamoon5.inkwell.ui.viewmodel.SortOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (Long) -> Unit,
    onNewNote: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val notes by viewModel.notes.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val section by viewModel.section.collectAsState()
    val sort by viewModel.sortOrder.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val counts by viewModel.counts.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var isGrid by remember { mutableStateOf(true) }
    var sortMenu by remember { mutableStateOf(false) }
    var overflowMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toast) {
        toast?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = sectionTitle(section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = { isGrid = !isGrid }) {
                        Icon(
                            imageVector = if (isGrid) Icons.Default.ViewAgenda else Icons.Default.GridView,
                            contentDescription = if (isGrid) "List view" else "Grid view"
                        )
                    }
                    Box {
                        IconButton(onClick = { overflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(overflowMenu, onDismissRequest = { overflowMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (section == NoteSection.TRASH) "Empty Recycle Bin" else "Select all tags") },
                                onClick = {
                                    overflowMenu = false
                                    if (section == NoteSection.TRASH) viewModel.emptyTrash()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, null) },
                                onClick = { overflowMenu = false; onOpenSettings() }
                            )
                        }
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(order)) },
                                trailingIcon = {
                                    if (order == sort) Icon(Icons.Default.Check, null)
                                },
                                onClick = { sortMenu = false; viewModel.setSortOrder(order) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = section == NoteSection.ALL,
                    onClick = { viewModel.setSection(NoteSection.ALL) },
                    icon = { Icon(Icons.Default.Inventory2, null) },
                    label = { Text("Notes") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = section == NoteSection.FAVORITES,
                    onClick = { viewModel.setSection(NoteSection.FAVORITES) },
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("Starred") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = section == NoteSection.ARCHIVE,
                    onClick = { viewModel.setSection(NoteSection.ARCHIVE) },
                    icon = { Icon(Icons.Default.Archive, null) },
                    label = { Text("Archive") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = section == NoteSection.TRASH,
                    onClick = { viewModel.setSection(NoteSection.TRASH) },
                    icon = {
                        Box {
                            Icon(Icons.Default.Delete, null)
                            if (counts.trash > 0) {
                                Text(
                                    text = counts.trash.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    label = { Text("Bin") },
                    colors = navColors()
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("More") },
                    colors = navColors()
                )
            }
        },
        floatingActionButton = {
            if (section != NoteSection.TRASH) {
                FloatingActionButton(
                    onClick = onNewNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, contentDescription = "New note") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (section != NoteSection.TRASH) {
                TextField(
                    value = query,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Search notes, tags, checklist...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                if (tags.isNotEmpty() && section != NoteSection.VAULT) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = {
                                    viewModel.setTag(if (selectedTag.isEmpty()) "__none__" else "")
                                },
                                label = { Text("All") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedTag.isEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (selectedTag.isEmpty()) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                        items(tags.flatMap { it.split(",") }.filter { it.isNotBlank() }.distinct()) { tag ->
                            AssistChip(
                                onClick = { viewModel.setTag(if (selectedTag == tag) "" else tag) },
                                label = { Text("#$tag") },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selectedTag == tag) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = if (selectedTag == tag) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = emptyTitle(section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = emptyHint(section),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (isGrid) 2 else 1),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            section = section,
                            onClick = { onNoteClick(note.id) },
                            onPin = { viewModel.togglePinned(note) },
                            onFav = { viewModel.toggleFavorite(note) },
                            onArchive = {
                                if (section == NoteSection.ARCHIVE) viewModel.unarchive(note)
                                else viewModel.archive(note)
                            },
                            onTrash = { viewModel.trash(note) },
                            onRestore = { viewModel.restore(note) },
                            onDeleteForever = { viewModel.deleteForever(note) },
                            onDuplicate = { viewModel.duplicate(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    indicatorColor = MaterialTheme.colorScheme.primaryContainer
)

private fun sectionTitle(section: NoteSection) = when (section) {
    NoteSection.ALL -> "Inkwell"
    NoteSection.FAVORITES -> "Starred"
    NoteSection.ARCHIVE -> "Archive"
    NoteSection.TRASH -> "Recycle Bin"
    NoteSection.VAULT -> "Private Vault"
}

private fun sortLabel(order: SortOrder) = when (order) {
    SortOrder.UPDATED -> "Last edited"
    SortOrder.CREATED -> "Date created"
    SortOrder.TITLE_ASC -> "Title A-Z"
    SortOrder.TITLE_DESC -> "Title Z-A"
}

private fun emptyTitle(section: NoteSection) = when (section) {
    NoteSection.TRASH -> "Recycle Bin is empty"
    NoteSection.ARCHIVE -> "No archived notes"
    NoteSection.FAVORITES -> "No starred notes"
    else -> "No notes yet"
}

private fun emptyHint(section: NoteSection) = when (section) {
    NoteSection.TRASH -> "Deleted notes stay here for 30 days"
    NoteSection.ARCHIVE -> "Archive a note to keep it out of the way"
    else -> "Tap + to create one"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    section: NoteSection,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onFav: () -> Unit,
    onArchive: () -> Unit,
    onTrash: () -> Unit,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit,
    onDuplicate: () -> Unit
) {
    val noteColor = NoteColors[note.color % NoteColors.size]
    var menu by remember { mutableStateOf(false) }
    val titleColor = Color.Black.copy(alpha = 0.87f)
    val bodyColor = Color.Black.copy(alpha = 0.74f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { menu = true }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = noteColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Note actions",
                            modifier = Modifier.size(18.dp),
                            tint = titleColor
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (section != NoteSection.TRASH) {
                            DropdownMenuItem(
                                text = { Text("Star") },
                                leadingIcon = { Icon(Icons.Default.Star, null) },
                                onClick = { menu = false; onFav() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.pinned) "Unpin" else "Pin to top") },
                                leadingIcon = { Icon(Icons.Default.PushPin, null) },
                                onClick = { menu = false; onPin() }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = { menu = false; onDuplicate() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.archived) "Remove from archive" else "Archive") },
                                leadingIcon = {
                                    Icon(
                                        if (note.archived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        null
                                    )
                                },
                                onClick = { menu = false; onArchive() }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Move to Recycle Bin") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = { menu = false; onTrash() }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Restore") },
                                leadingIcon = { Icon(Icons.Default.Restore, null) },
                                onClick = { menu = false; onRestore() }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete forever") },
                                leadingIcon = { Icon(Icons.Default.DeleteForever, null) },
                                onClick = { menu = false; onDeleteForever() }
                            )
                        }
                    }
                }
            }

            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = bodyColor,
                    maxLines = if (note.title.isBlank()) 8 else 5,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.checklist.isNotBlank()) {
                val items = Checklist.parse(note.checklist)
                Spacer(Modifier.height(8.dp))
                items.take(3).forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (item.done) Color.Black.copy(alpha = 0.55f) else Color.Transparent)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = bodyColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None
                        )
                    }
                }
                if (items.size > 3) {
                    Text(
                        text = "+${items.size - 3} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = titleColor.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (section == NoteSection.TRASH) deletedAgo(note.deletedAt) else formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = titleColor.copy(alpha = 0.55f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.hasImages) {
                        Text("IMG", style = MaterialTheme.typography.labelSmall, color = titleColor.copy(alpha = 0.7f))
                        Spacer(Modifier.size(8.dp))
                    }
                    if (note.tags.isNotBlank()) {
                        Text(
                            text = note.tags.split(",").take(2).joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = titleColor.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.size(8.dp))
                    }
                    if (note.favorite) {
                        Icon(
                            Icons.Default.Star, "Starred", modifier = Modifier.size(14.dp), tint = titleColor
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    if (note.pinned) {
                        Icon(Icons.Default.PushPin, "Pinned", modifier = Modifier.size(14.dp), tint = titleColor)
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun deletedAgo(timestamp: Long): String {
    if (timestamp <= 0) return "Deleted"
    val days = ((System.currentTimeMillis() - timestamp) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Deleted today"
        days == 1 -> "Deleted yesterday"
        days >= 30 -> "Will be removed"
        else -> "Deleted $days days ago"
    }
}