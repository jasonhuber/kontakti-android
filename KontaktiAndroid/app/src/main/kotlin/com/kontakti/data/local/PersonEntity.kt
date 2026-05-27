package com.kontakti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    val email: String?,
    val phone: String?,
    val title: String?,
    @ColumnInfo(name = "company_id") val companyId: String?,
    @ColumnInfo(name = "company_name") val companyName: String?,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    // Stored as String to avoid enum migration headaches
    @ColumnInfo(name = "relationship_strength") val relationshipStrength: String,
    @ColumnInfo(name = "last_contacted_at") val lastContactedAt: String?,
    @ColumnInfo(name = "next_followup_at") val nextFollowupAt: String?,
    @ColumnInfo(name = "do_not_contact", defaultValue = "0") val doNotContact: Boolean = false,
    @ColumnInfo(name = "do_not_contact_reason") val doNotContactReason: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
