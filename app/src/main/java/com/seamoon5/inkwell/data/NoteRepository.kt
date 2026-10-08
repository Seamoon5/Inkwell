package com.seamoon5.inkwell.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun activeNotes() = noteDao.activeNotes()
    fun favoriteNotes() = noteDao.favoriteNotes()
    fun archivedNotes() = noteDao.archivedNotes()
    fun trashedNotes() = noteDao.trashedNotes()
    fun vaultNotes() = noteDao.vaultNotes()
    fun allTags() = noteDao.allTags()

    fun noteCount() = noteDao.noteCount()
    fun favoriteCount() = noteDao.favoriteCount()
    fun trashCount() = noteDao.trashCount()
    fun archiveCount() = noteDao.archiveCount()

    fun filterNotes(query: String, tag: String, archivedOnly: Int, favOnly: Int) =
        noteDao.filterNotes(query, tag, archivedOnly, favOnly)

    fun imagesForNote(noteId: Long) = noteDao.imagesForNote(noteId)

    suspend fun getNoteById(id: Long) = noteDao.getNoteById(id)
    suspend fun imagesForNoteOnce(noteId: Long) = noteDao.imagesForNoteOnce(noteId)

    suspend fun save(note: Note): Long = noteDao.insertNote(note)
    suspend fun update(note: Note) = noteDao.updateNote(note)
    suspend fun delete(note: Note) = noteDao.deleteNote(note)

    suspend fun trash(id: Long) = noteDao.moveToTrash(id, System.currentTimeMillis())
    suspend fun restore(id: Long) = noteDao.restoreFromTrash(id, System.currentTimeMillis())
    suspend fun deleteForever(id: Long) = noteDao.deletePermanently(id)
    suspend fun purgeOldTrash(days: Int = 30) =
        noteDao.purgeTrashOlderThan(System.currentTimeMillis() - days * 86_400_000L)

    suspend fun archive(id: Long) = noteDao.archiveNote(id)
    suspend fun unarchive(id: Long) = noteDao.unarchiveNote(id)
    suspend fun setFavorite(id: Long, value: Boolean) = noteDao.setFavorite(id, value)
    suspend fun setPinned(id: Long, value: Boolean) = noteDao.setPinned(id, value)
    suspend fun setTags(id: Long, tags: String) = noteDao.setTags(id, tags)
    suspend fun markHasImages(id: Long) = noteDao.markHasImages(id)

    suspend fun addImages(images: List<NoteImage>) {
        noteDao.insertImages(images)
        images.firstOrNull()?.let { noteDao.markHasImages(it.noteId) }
    }

    suspend fun deleteAll() = noteDao.deleteAllNotes()

    suspend fun trashedNotesOnce() = noteDao.trashedNotesOnce()

    suspend fun deleteImage(image: NoteImage) = noteDao.deleteImage(image)

    suspend fun exportSnapshot(): Pair<List<Note>, List<NoteImage>> =
        noteDao.getAllForBackup() to noteDao.getAllImagesForBackup()

    suspend fun importSnapshot(notes: List<Note>, images: List<NoteImage>) {
        noteDao.insertNotes(notes)
        noteDao.insertImages(images)
    }
}