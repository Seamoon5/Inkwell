package com.seamoon5.inkwell.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index("trashed"), Index("archived"), Index("favorite")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val color: Int = 0,
    val pinned: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val favorite: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val archived: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val trashed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val vault: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val deletedAt: Long = 0,
    @ColumnInfo(defaultValue = "''")
    val tags: String = "",
    @ColumnInfo(defaultValue = "''")
    val checklist: String = "",
    @ColumnInfo(defaultValue = "0")
    val hasImages: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)