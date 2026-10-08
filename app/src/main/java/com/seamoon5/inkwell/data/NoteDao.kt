package com.seamoon5.inkwell.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 AND vault = 0 ORDER BY pinned DESC, updatedAt DESC")
    fun activeNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 0 AND vault = 0 AND favorite = 1 ORDER BY pinned DESC, updatedAt DESC")
    fun favoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 0 AND archived = 1 AND vault = 0 ORDER BY updatedAt DESC")
    fun archivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY deletedAt DESC")
    fun trashedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE trashed = 1 ORDER BY deletedAt DESC")
    suspend fun trashedNotesOnce(): List<Note>

    @Query("SELECT * FROM notes WHERE vault = 1 AND trashed = 0 ORDER BY updatedAt DESC")
    fun vaultNotes(): Flow<List<Note>>

    @Query(
        """
        SELECT * FROM notes
        WHERE trashed = 0 AND vault = 0
        AND (:archivedOnly = 0 AND archived = :archivedOnly OR :archivedOnly = 1 AND archived = 1)
        AND (:favOnly = 0 OR favorite = 1)
        AND (:tag = '' OR tags LIKE '%' || :tag || '%')
        AND (:query = '' OR title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR checklist LIKE '%' || :query || '%')
        ORDER BY pinned DESC, updatedAt DESC
        """
    )
    fun filterNotes(
        query: String,
        tag: String,
        archivedOnly: Int,
        favOnly: Int
    ): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT DISTINCT tags FROM notes WHERE tags != '' AND trashed = 0")
    fun allTags(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 0 AND vault = 0")
    fun noteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 0 AND vault = 0 AND favorite = 1")
    fun favoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 1")
    fun trashCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notes WHERE trashed = 0 AND archived = 1 AND vault = 0")
    fun archiveCount(): Flow<Int>

    @Query("SELECT * FROM notes")
    suspend fun getAllForBackup(): List<Note>

    @Query("SELECT * FROM note_images")
    suspend fun getAllImagesForBackup(): List<NoteImage>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY position ASC")
    fun imagesForNote(noteId: Long): Flow<List<NoteImage>>

    @Query("SELECT * FROM note_images WHERE noteId = :noteId ORDER BY position ASC")
    suspend fun imagesForNoteOnce(noteId: Long): List<NoteImage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<NoteImage>)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Delete
    suspend fun deleteImage(image: NoteImage)

    @Query("UPDATE notes SET trashed = 1, deletedAt = :deletedAt, pinned = 0, favorite = 0 WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletedAt: Long)

    @Query("UPDATE notes SET trashed = 0, deletedAt = 0, updatedAt = :now WHERE id = :id")
    suspend fun restoreFromTrash(id: Long, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deletePermanently(id: Long)

    @Query("DELETE FROM notes WHERE trashed = 1 AND deletedAt < :cutoff")
    suspend fun purgeTrashOlderThan(cutoff: Long)

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Query("UPDATE notes SET archived = 1, pinned = 0 WHERE id = :id")
    suspend fun archiveNote(id: Long)

    @Query("UPDATE notes SET archived = 0 WHERE id = :id")
    suspend fun unarchiveNote(id: Long)

    @Query("UPDATE notes SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("UPDATE notes SET tags = :tags WHERE id = :id")
    suspend fun setTags(id: Long, tags: String)

    @Query("UPDATE notes SET hasImages = 1 WHERE id = :id")
    suspend fun markHasImages(id: Long)

    @Query("SELECT MAX(updatedAt) FROM notes")
    fun lastUpdated(): Flow<Long?>
}