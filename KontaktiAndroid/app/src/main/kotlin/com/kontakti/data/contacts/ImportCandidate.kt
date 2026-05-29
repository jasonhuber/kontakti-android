package com.kontakti.data.contacts

import com.google.gson.annotations.SerializedName

/**
 * A candidate contact, either from the device address book or a Google/Gmail
 * lookup. Backend expects snake_case field names — the @SerializedName
 * annotations drive the wire shape.
 *
 * The legacy `name` field is kept as a Kotlin convenience so call sites that
 * collected a single full-name string don't have to manually split. If you
 * already have separated names, pass `firstName`/`lastName` directly and
 * leave `name` empty.
 */
data class ImportCandidate(
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("company_name") val company: String? = null,
    val source: String? = null
) {
    /**
     * Convenience accessor for callers that just want a display name.
     */
    val name: String get() = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")

    companion object {
        /**
         * Build from a single display name by splitting on the first space.
         * Returns `null` if `displayName` trims to empty.
         */
        fun fromDisplayName(
            displayName: String,
            email: String? = null,
            phone: String? = null,
            company: String? = null,
            source: String? = null
        ): ImportCandidate? {
            val trimmed = displayName.trim()
            if (trimmed.isEmpty()) return null
            val parts = trimmed.split(' ', limit = 2)
            return ImportCandidate(
                firstName = parts[0],
                lastName = parts.getOrNull(1)?.trim().orEmpty(),
                email = email?.trim()?.ifBlank { null },
                phone = phone?.trim()?.ifBlank { null },
                company = company?.trim()?.ifBlank { null },
                source = source
            )
        }
    }
}

/**
 * Wraps a list of candidates with optional `google_account_id` for the
 * `POST /contacts/import` endpoint.
 */
data class BulkImportRequest(
    val contacts: List<ImportCandidate>,
    @SerializedName("google_account_id") val googleAccountId: Int? = null
)
