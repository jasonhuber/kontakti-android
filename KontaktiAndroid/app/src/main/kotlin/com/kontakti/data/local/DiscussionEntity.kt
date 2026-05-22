package com.kontakti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discussions")
data class DiscussionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    // Stored as String (DiscussionType.name)
    val type: String,
    val summary: String?,
    val body: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
