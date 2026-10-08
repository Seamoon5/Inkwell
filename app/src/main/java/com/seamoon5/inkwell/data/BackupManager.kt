package com.seamoon5.inkwell.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * Plain-JSON backup file. This is what the user saves to Google Drive from the
 * Android file picker, and what a restore reads back. No account, no network,
 * no credentials involved.
 */
object BackupManager {

    private const val FORMAT_VERSION = 2

    fun buildJson(notes: List<Note>, images: List<NoteImage>): String {
        val root = JSONObject()
        root.put("app", "Inkwell")
        root.put("formatVersion", FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("noteCount", notes.size)

        val notesArray = JSONArray()
        notes.forEach { note ->
            notesArray.put(
                JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("content", note.content)
                    put("color", note.color)
                    put("pinned", note.pinned)
                    put("favorite", note.favorite)
                    put("archived", note.archived)
                    put("vault", note.vault)
                    put("tags", note.tags)
                    put("checklist", note.checklist)
                    put("hasImages", note.hasImages)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                }
            )
        }
        root.put("notes", notesArray)

        val imagesArray = JSONArray()
        images.forEach { img ->
            imagesArray.put(
                JSONObject().apply {
                    put("noteId", img.noteId)
                    put("uri", img.uri)
                    put("position", img.position)
                    put("addedAt", img.addedAt)
                }
            )
        }
        root.put("images", imagesArray)

        return root.toString(2)
    }

    /**
     * Returns null on success, or a short plain-English reason when the file is
     * not an Inkwell backup. Callers show the string directly to the user.
     */
    fun readJson(context: Context, uri: Uri): Pair<List<Note>, List<NoteImage>>? {
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (e: Exception) {
            null
        } ?: return null

        return try {
            val root = JSONObject(text)
            val notesArray = root.optJSONArray("notes") ?: JSONArray()
            val imagesArray = root.optJSONArray("images") ?: JSONArray()

            val notes = ArrayList<Note>(notesArray.length())
            for (i in 0 until notesArray.length()) {
                val o = notesArray.getJSONObject(i)
                notes.add(
                    Note(
                        id = o.optLong("id", 0),
                        title = o.optString("title", ""),
                        content = o.optString("content", ""),
                        color = o.optInt("color", 0),
                        pinned = o.optBoolean("pinned", false),
                        favorite = o.optBoolean("favorite", false),
                        archived = o.optBoolean("archived", false),
                        trashed = false,
                        vault = o.optBoolean("vault", false),
                        deletedAt = 0,
                        tags = o.optString("tags", ""),
                        checklist = o.optString("checklist", ""),
                        hasImages = o.optBoolean("hasImages", false),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            val images = ArrayList<NoteImage>(imagesArray.length())
            for (i in 0 until imagesArray.length()) {
                val o = imagesArray.getJSONObject(i)
                images.add(
                    NoteImage(
                        noteId = o.optLong("noteId", 0),
                        uri = o.optString("uri", ""),
                        position = o.optInt("position", 0),
                        addedAt = o.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }

            notes to images
        } catch (e: Exception) {
            null
        }
    }
}