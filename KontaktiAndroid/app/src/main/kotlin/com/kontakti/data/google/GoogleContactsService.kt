package com.kontakti.data.google

import android.content.Context
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.api.services.people.v1.PeopleService
import com.kontakti.data.contacts.ImportCandidate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleContactsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val transport = NetHttpTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val appName = "Kontakti"

    /**
     * Given a valid OAuth access token, returns a merged deduplicated list of
     * [ImportCandidate] from:
     *  1. Google Contacts (People API)
     *  2. Frequent Gmail senders (top 100 recent messages)
     */
    suspend fun fetchCandidates(accessToken: String): List<ImportCandidate> =
        withContext(Dispatchers.IO) {
            val credential = GoogleCredential().setAccessToken(accessToken)

            val contactCandidates = fetchGoogleContacts(credential)
            val gmailCandidates = fetchGmailSenders(credential)

            mergeCandidates(contactCandidates, gmailCandidates)
        }

    // ── Google Contacts (People API) ──────────────────────────────────────────

    private fun fetchGoogleContacts(credential: GoogleCredential): List<ImportCandidate> {
        return try {
            val people = PeopleService.Builder(transport, jsonFactory, credential)
                .setApplicationName(appName)
                .build()

            val response = people.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,organizations,phoneNumbers")
                .setPageSize(1000)
                .execute()

            (response.connections ?: emptyList()).mapNotNull { person ->
                val name = person.names?.firstOrNull()?.displayName
                    ?: person.names?.firstOrNull()?.let { n ->
                        "${n.givenName.orEmpty()} ${n.familyName.orEmpty()}".trim()
                    }
                    ?: return@mapNotNull null
                if (name.isBlank()) return@mapNotNull null

                ImportCandidate.fromDisplayName(
                    displayName = name,
                    email = person.emailAddresses?.firstOrNull()?.value,
                    phone = person.phoneNumbers?.firstOrNull()?.value,
                    company = person.organizations?.firstOrNull()?.name,
                    source = "google_contacts"
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Gmail frequent senders ────────────────────────────────────────────────

    private fun fetchGmailSenders(credential: GoogleCredential): List<ImportCandidate> {
        return try {
            val gmail = Gmail.Builder(transport, jsonFactory, credential)
                .setApplicationName(appName)
                .build()

            // Fetch the 100 most recent messages
            val listResponse = gmail.users().messages()
                .list("me")
                .setMaxResults(100L)
                .setLabelIds(listOf("INBOX"))
                .execute()

            val messageIds = listResponse.messages?.map { it.id } ?: return emptyList()

            // Extract From headers — use batch-style sequential fetches with minimal fields
            val fromHeaders = messageIds.mapNotNull { id ->
                try {
                    val msg = gmail.users().messages()
                        .get("me", id)
                        .setFormat("metadata")
                        .setMetadataHeaders(listOf("From"))
                        .execute()
                    msg.payload?.headers?.firstOrNull { it.name == "From" }?.value
                } catch (_: Exception) {
                    null
                }
            }

            // Parse "Display Name <email@example.com>" format, deduplicate by email
            parseFromHeaders(fromHeaders)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun parseFromHeaders(headers: List<String>): List<ImportCandidate> {
        val seen = mutableSetOf<String>()
        return headers.mapNotNull { header ->
            val emailMatch = Regex("<([^>]+)>").find(header)
            val email = emailMatch?.groupValues?.get(1)?.trim()?.lowercase()
            if (email == null || email.endsWith("@googlemail.com") && email.contains("noreply")) {
                return@mapNotNull null
            }
            if (!seen.add(email)) return@mapNotNull null

            val name = if (emailMatch != null) {
                header.substringBefore("<").trim().removeSurrounding("\"").trim()
            } else {
                header.trim()
            }

            if (name.isBlank() && email.isBlank()) return@mapNotNull null

            ImportCandidate.fromDisplayName(
                displayName = name.ifBlank { email },
                email = email.ifBlank { null },
                source = "gmail"
            )
        }
    }

    // ── Merge + deduplicate ───────────────────────────────────────────────────

    private fun mergeCandidates(
        contacts: List<ImportCandidate>,
        gmail: List<ImportCandidate>
    ): List<ImportCandidate> {
        val result = mutableListOf<ImportCandidate>()
        val seenEmails = mutableSetOf<String>()

        for (candidate in contacts + gmail) {
            val key = candidate.email?.lowercase()
            if (key == null || seenEmails.add(key)) {
                result.add(candidate)
            }
        }

        return result.sortedBy { it.name }
    }
}
