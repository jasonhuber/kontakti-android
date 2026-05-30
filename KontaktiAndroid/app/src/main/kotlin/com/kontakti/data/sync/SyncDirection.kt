package com.kontakti.data.sync

enum class SyncDirection {
    TWO_WAY,
    DOWNLOAD_ONLY,
    UPLOAD_ONLY;

    val label: String get() = when (this) {
        TWO_WAY -> "Two-way"
        DOWNLOAD_ONLY -> "Download only"
        UPLOAD_ONLY -> "Upload only"
    }
}
