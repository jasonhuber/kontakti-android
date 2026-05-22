package com.kontakti.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class CompanyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val domain: String?,
    @ColumnInfo(name = "logo_url") val logoUrl: String?,
    val industry: String?,
    @ColumnInfo(name = "size_range") val sizeRange: String?,
    val website: String?,
    @ColumnInfo(name = "people_count") val peopleCount: Int?,
    @ColumnInfo(name = "updated_at") val updatedAt: String
)
