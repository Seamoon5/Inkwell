package com.seamoon5.inkwell.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.seamoon5.inkwell.data.BackupManager
import com.seamoon5.inkwell.data.Checklist
import com.seamoon5.inkwell.data.Note
import com.seamoon5.inkwell.data.NoteImage
import com.seamoon5.inkwell.data.NoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NoteSection { ALL, FAVORITES, ARCHIVE, TRASH, VAULT }

enum class SortOrder { UPDATED, CREATED, TITLE_ASC, TITLE_DESC }

@OptIn(ExperimentalCoroutinesApi::class)
class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow("")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    private val _section = MutableStateFlow(NoteSection.ALL)
    val section: StateFlow<NoteSection> = _section.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.UPDATED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _currentNote = MutableStateFlow<Note?>(null)
    val currentNote: StateFlow<Note?> = _currentNote.asStateFlow()

    private val _currentImages = MutableStateFlow<List<NoteImage>>(emptyList())
    val currentImages: StateFlow<List<NoteImage>> = _currentImages.asStateFlow()

    val tags: StateFlow<List<String>> = repository.allTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val counts: StateFlow<Counts> = combine(
        repository.noteCount(), repository.favoriteCount(),
        repository.archiveCount(), repository.trashCount()
    ) { all, fav, arch, trash -> Counts(all, fav, arch, trash) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Counts(0, 0, 0, 0))

    data class Counts(val all: Int, val favorites: Int, val archive: Int, val trash: Int)

    val notes: StateFlow<List<Note>> = combine(
        _searchQuery, _selectedTag, _section, _sortOrder
    ) { query, tag, section, sort ->
        Query(query, tag, section, sort)
    }.flatMapLatest { q ->
        val listFlow = when (q.section) {
            NoteSection.ALL -> repository.filterNotes(q.query, q.tag, 0, 0)
            NoteSection.FAVORITES -> repository.filterNotes(q.query, q.tag, 0, 1)
            NoteSection.ARCHIVE -> repository.filterNotes(q.query, q.tag, 1, 0)
            NoteSection.VAULT -> repository.vaultNotes()
            NoteSection.TRASH -> repository.trashedNotes()
        }
        when (q.sort) {
            SortOrder.UPDATED -> listFlow
            else -> listFlow.map { list -> sortList(list, q.sort) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private data class Query(
        val query: String,
        val tag: String,
        val section: NoteSection,
        val sort: SortOrder
    )

    private fun sortList(list: List<Note>, order: SortOrder): List<Note> = when (order) {
        SortOrder.UPDATED -> list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
        SortOrder.CREATED -> list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt })
        SortOrder.TITLE_ASC -> list.sortedWith(compareByDescending<Note> { it.pinned }.thenBy { it.title.lowercase() })
        SortOrder.TITLE_DESC -> list.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.title.lowercase() })
    }

    private var autosaveJob: Job? = null

    init {
        viewModelScope.launch { repository.purgeOldTrash(30) }
    }

    fun setSearchQuery(value: String) { _searchQuery.value = value }
    fun setTag(value: String) { _selectedTag.value = value }
    fun setSection(value: NoteSection) { _section.value = value }
    fun setSortOrder(value: SortOrder) { _sortOrder.value = value }
    fun showToast(message: String) {
        _toast.value = message
        viewModelScope.launch {
            delay(2200)
            _toast.value = null
        }
    }

    fun loadNote(id: Long) {
        viewModelScope.launch {
            if (id == 0L) {
                _currentNote.value = null
                _currentImages.value = emptyList()
            } else {
                _currentNote.value = repository.getNoteById(id)
                _currentImages.value = repository.imagesForNoteOnce(id)
            }
        }
    }

    fun clearCurrentNote() {
        autosaveJob?.cancel()
        _currentNote.value = null
        _currentImages.value = emptyList()
    }

    /**
     * Debounced auto-save. The editor calls this on every keystroke; only the
     * last change inside 600 ms touches the database, so typing stays smooth.
     */
    fun autoSave(note: Note) {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(600)
            if (note.id == 0L) repository.save(note) else repository.update(note)
        }
    }

    fun saveNote(note: Note, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = if (note.id == 0L) {
                repository.save(note)
            } else {
                repository.update(note)
                note.id
            }
            if (note.id == 0L) _currentNote.value = note.copy(id = id)
            onSaved(id)
        }
    }

    fun duplicate(note: Note) {
        viewModelScope.launch {
            repository.save(
                note.copy(
                    id = 0,
                    title = note.title + " (copy)",
                    pinned = false,
                    trashed = false,
                    deletedAt = 0,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )
            showToast("Note duplicated")
        }
    }

    fun trash(note: Note) {
        viewModelScope.launch {
            repository.trash(note.id)
            showToast("Moved to Recycle Bin")
        }
    }

    fun restore(note: Note) {
        viewModelScope.launch { repository.restore(note.id); showToast("Note restored") }
    }

    fun deleteForever(note: Note) {
        viewModelScope.launch { repository.deleteForever(note.id); showToast("Deleted forever") }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.trashedNotesOnce().forEach { repository.deleteForever(it.id) }
            showToast("Recycle Bin emptied")
        }
    }

    fun archive(note: Note) {
        viewModelScope.launch { repository.archive(note.id); showToast("Note archived") }
    }

    fun unarchive(note: Note) {
        viewModelScope.launch { repository.unarchive(note.id); showToast("Restored from archive") }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch { repository.setFavorite(note.id, !note.favorite) }
    }

    fun togglePinned(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.pinned) }
    }

    fun saveTags(note: Note, tags: String) {
        viewModelScope.launch {
            repository.setTags(note.id, normalizeTags(tags))
            if (_currentNote.value?.id == note.id) {
                _currentNote.value = note.copy(tags = normalizeTags(tags), updatedAt = System.currentTimeMillis())
            }
        }
    }

    fun addImages(noteId: Long, uris: List<String>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val start = repository.imagesForNoteOnce(noteId).size
            val rows = uris.mapIndexed { index, uri ->
                NoteImage(noteId = noteId, uri = uri, position = start + index)
            }
            repository.addImages(rows)
            _currentImages.value = repository.imagesForNoteOnce(noteId)
        }
    }

    fun deleteImage(image: NoteImage) {
        viewModelScope.launch {
            repository.deleteImage(image)
            _currentImages.value = repository.imagesForNoteOnce(image.noteId)
        }
    }

    fun saveChecklist(note: Note, items: List<Checklist.Item>) {
        viewModelScope.launch {
            val updated = note.copy(
                checklist = Checklist.serialize(items),
                updatedAt = System.currentTimeMillis()
            )
            repository.update(updated)
            if (_currentNote.value?.id == note.id) _currentNote.value = updated
        }
    }

    fun importSnapshot(notes: List<Note>, images: List<NoteImage>) {
        viewModelScope.launch {
            repository.importSnapshot(notes, images)
            showToast("Restored ${notes.size} notes")
        }
    }

    fun deleteAllNotes() {
        viewModelScope.launch {
            repository.deleteAll()
            showToast("All notes deleted")
        }
    }

    fun backupToFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            val (notes, images) = repository.exportSnapshot()
            val json = BackupManager.buildJson(notes, images)
            try {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                showToast("Backup saved")
            } catch (e: Exception) {
                showToast("Could not write the backup file")
            }
        }
    }

    companion object {
        fun normalizeTags(raw: String): String = raw
            .split(",", " ")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(",")
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}