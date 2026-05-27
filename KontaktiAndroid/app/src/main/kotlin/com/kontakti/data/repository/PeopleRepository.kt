package com.kontakti.data.repository

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.kontakti.data.local.KontaktiDao
import com.kontakti.data.local.PersonEntity
import com.kontakti.data.model.CreatePersonRequest
import com.kontakti.data.model.Note
import com.kontakti.data.model.Paginated
import com.kontakti.data.model.Person
import com.kontakti.data.model.PersonPatch
import com.kontakti.data.model.Task
import com.kontakti.data.model.TimelineEvent
import com.kontakti.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val api: ApiService,
    private val dao: KontaktiDao,
    private val gson: Gson
) {
    suspend fun getPerson(id: String): Person = api.getPerson(id)

    suspend fun listPeoplePage(query: String? = null, page: Int = 1): Paginated<Person> =
        api.listPeople(query, page)

    suspend fun createPerson(req: CreatePersonRequest): Person = api.createPerson(req)

    suspend fun deletePerson(id: String) = api.deletePerson(id)

    suspend fun listNotes(personId: String): List<Note> = api.listPersonNotes(personId)
    suspend fun listTasks(personId: String): List<Task> = api.getPersonTasks(personId)
    suspend fun getTimeline(personId: String): List<TimelineEvent> = api.getTimeline(personId)

    /**
     * Serializes the patch with Gson (respects @SerializedName + non-nulls only),
     * then PATCHes the person. `emails`/`phones` are replace-list semantics.
     */
    suspend fun updatePerson(id: String, patch: PersonPatch): Person {
        // Use a Gson instance that drops nulls, so callers can pass null = "unchanged"
        val compactGson = com.google.gson.GsonBuilder()
            .registerTypeAdapter(
                com.kontakti.data.model.RelationshipStrength::class.java,
                object : com.google.gson.TypeAdapter<com.kontakti.data.model.RelationshipStrength>() {
                    override fun write(out: com.google.gson.stream.JsonWriter, value: com.kontakti.data.model.RelationshipStrength?) {
                        out.value(value?.name)
                    }
                    override fun read(input: com.google.gson.stream.JsonReader): com.kontakti.data.model.RelationshipStrength? = null
                }
            )
            .create()
        val body = compactGson.toJsonTree(patch).asJsonObject
        return api.updatePerson(id, body)
    }

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
    doNotContact = doNotContact,
    doNotContactReason = doNotContactReason,
    updatedAt = updatedAt
)
