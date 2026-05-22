package com.kontakti.data.contacts

import android.content.Context
import android.provider.ContactsContract
import com.kontakti.data.local.KontaktiDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceContactsImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: KontaktiDao
) {
    /**
     * Reads all device contacts and returns only those not already present in Room
     * (matched by email, case-insensitive). Contacts with no name are skipped.
     */
    suspend fun fetchNewCandidates(): List<ImportCandidate> = withContext(Dispatchers.IO) {
        val deviceContacts = readDeviceContacts()

        // Collect all emails we have locally so we can deduplicate
        val knownEmails = if (deviceContacts.any { it.email != null }) {
            val emails = deviceContacts.mapNotNull { it.email?.lowercase() }
            dao.getPeopleByEmail(emails).map { it.email?.lowercase() }.toSet()
        } else {
            emptySet()
        }

        deviceContacts.filter { candidate ->
            candidate.email == null || candidate.email.lowercase() !in knownEmails
        }
    }

    private fun readDeviceContacts(): List<ImportCandidate> {
        val results = mutableMapOf<String, MutableContact>()

        // Query display name, email, phone, and organization in one pass each
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Data.DATA1,
            ContactsContract.Data.DATA2,
            ContactsContract.Data.DATA4,
        )

        val cursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            "${ContactsContract.Data.MIMETYPE} IN (?, ?, ?, ?)",
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            ),
            null
        ) ?: return emptyList()

        cursor.use {
            val idIdx = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val mimeIdx = it.getColumnIndex(ContactsContract.Data.MIMETYPE)
            val data1Idx = it.getColumnIndex(ContactsContract.Data.DATA1)
            val data2Idx = it.getColumnIndex(ContactsContract.Data.DATA2)
            val data4Idx = it.getColumnIndex(ContactsContract.Data.DATA4)

            while (it.moveToNext()) {
                val id = it.getString(idIdx) ?: continue
                val mime = it.getString(mimeIdx) ?: continue
                val contact = results.getOrPut(id) { MutableContact(id) }

                when (mime) {
                    ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                        val given = it.getString(data2Idx).orEmpty()
                        val family = it.getString(data4Idx).orEmpty()
                        val full = "$given $family".trim()
                        if (full.isNotBlank()) contact.name = full
                    }
                    ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                        val email = it.getString(data1Idx)
                        if (!email.isNullOrBlank() && contact.email == null) {
                            contact.email = email.trim()
                        }
                    }
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                        val phone = it.getString(data1Idx)
                        if (!phone.isNullOrBlank() && contact.phone == null) {
                            contact.phone = phone.trim()
                        }
                    }
                    ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                        val org = it.getString(data1Idx)
                        if (!org.isNullOrBlank() && contact.company == null) {
                            contact.company = org.trim()
                        }
                    }
                }
            }
        }

        return results.values
            .filter { it.name.isNotBlank() }
            .map { ImportCandidate(it.name, it.email, it.phone, it.company) }
    }

    private data class MutableContact(
        val id: String,
        var name: String = "",
        var email: String? = null,
        var phone: String? = null,
        var company: String? = null
    )
}
