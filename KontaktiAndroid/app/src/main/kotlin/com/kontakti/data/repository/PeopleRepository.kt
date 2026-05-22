package com.kontakti.data.repository

import com.kontakti.data.local.KontaktiDao
import com.kontakti.data.local.PersonEntity
import com.kontakti.data.model.Person
import com.kontakti.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val api: ApiService,
    private val dao: KontaktiDao
) {
    /**
     * Emits the local Room list immediately. Pass a non-null [query] to filter
     * by name / email / company; null returns all people.
     */
    fun getPeople(query: String? = null): Flow<List<PersonEntity>> =
        if (query.isNullOrBlank()) dao.getPeople()
        else dao.searchPeople(query)

    /**
     * Fetches all pages from the API and upserts into Room. Silently swallows
     * network failures so the caller keeps reading stale Room data.
     */
    suspend fun refresh() {
        try {
            var page = 1
            var lastPage: Int
            do {
                val result = api.listPeople(page = page)
                lastPage = result.lastPage
                dao.upsertPeople(result.data.map { it.toEntity() })
                page++
            } while (page <= lastPage)
        } catch (_: Exception) {
            // Offline — Room data remains available
        }
    }
}

// ── Mapping ───────────────────────────────────────────────────────────────────

fun Person.toEntity() = PersonEntity(
    id = id,
    firstName = firstName,
    lastName = lastName,
    fullName = fullName,
    email = email,
    phone = phone,
    title = title,
    companyId = companyId,
    companyName = company?.name,
    avatarUrl = avatarUrl,
    relationshipStrength = relationshipStrength.name,
    lastContactedAt = lastContactedAt,
    nextFollowupAt = nextFollowupAt,
    updatedAt = updatedAt
)
