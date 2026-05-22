package com.kontakti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOperation {
    CREATE_PERSON,
    LOG_DISCUSSION,
    COMPLETE_TASK
}

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operation: String, // SyncOperation.name
    val payload: String,   // JSON string
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
