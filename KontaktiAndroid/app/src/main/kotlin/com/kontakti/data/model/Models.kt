package com.kontakti.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: UserProfile)
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val username: String?,
    @SerializedName("has_completed_onboarding") val hasCompletedOnboarding: Boolean? = null
)

data class RegisterRequest(
    val name: String,
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

data class GoogleLoginRequest(@SerializedName("id_token") val idToken: String)

data class Tag(val id: String, val name: String, val slug: String, val color: String)

enum class RelationshipStrength(val label: String, val color: Long) {
    cold("Cold", 0xFF71717A),
    warm("Warm", 0xFFD97706),
    hot("Hot", 0xFFEA580C),
    close("Close", 0xFF16A34A);
}

data class Person(
    val id: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    @SerializedName("full_name") val fullName: String,
    val nickname: String? = null,
    val email: String?,
    val phone: String?,
    val emails: List<PersonEmail> = emptyList(),
    val phones: List<PersonPhone> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val urls: List<PersonUrl> = emptyList(),
    val birthday: String? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String?,
    @SerializedName("instagram_handle") val instagramHandle: String? = null,
    @SerializedName("facebook_url") val facebookUrl: String? = null,
    @SerializedName("twitter_x_handle") val twitterXHandle: String? = null,
    @SerializedName("tiktok_handle") val tiktokHandle: String? = null,
    @SerializedName("whatsapp_phone") val whatsappPhone: String? = null,
    @SerializedName("previous_employers") val previousEmployers: List<String> = emptyList(),
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    @SerializedName("how_we_met") val howWeMet: String? = null,
    @SerializedName("introduced_by_id") val introducedById: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val photos: List<PersonPhoto>? = null,
    @SerializedName("company_id") val companyId: String?,
    val company: Company?,
    val title: String?,
    @SerializedName("job_department") val jobDepartment: String? = null,
    @SerializedName("relationship_strength") val relationshipStrength: RelationshipStrength,
    @SerializedName("last_contacted_at") val lastContactedAt: String?,
    @SerializedName("next_followup_at") val nextFollowupAt: String?,
    val notes: String?,
    @SerializedName("device_note") val deviceNote: String? = null,
    @SerializedName("do_not_contact") val doNotContact: Boolean = false,
    @SerializedName("do_not_contact_reason") val doNotContactReason: String? = null,
    val tags: List<Tag> = emptyList(),
    val metadata: Map<String, @JvmSuppressWildcards Any?>? = null,
    @SerializedName("discussions_count") val discussionsCount: Int?,
    @SerializedName("tasks_count") val tasksCount: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class PersonEmail(
    val id: String? = null,
    val value: String,
    val label: String? = "personal",
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

/**
 * A photo attached to a Person. The `url` is either an absolute external URL
 * (e.g. `https://media.licdn.com/...`) or a relative path like
 * `/photos/<personId>/<uuid>.<ext>` that needs to be prefixed with the API
 * host before loading.
 */
data class PersonPhoto(
    val id: String,
    @SerializedName("person_id") val personId: String,
    val url: String,
    val source: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null
)

/** JSON body for uploading a photo via data URL or external URL pointer. */
data class PhotoUploadBody(
    val data: String? = null,
    val url: String? = null,
    val source: String? = null
)

data class PersonPhone(
    val id: String? = null,
    val value: String,
    val label: String? = "mobile",
    @SerializedName("is_primary") val isPrimary: Boolean = false
)

data class Address(
    val label: String? = "home",
    val street: String? = null,
    val city: String? = null,
    val region: String? = null,
    @SerializedName("postal_code") val postalCode: String? = null,
    val country: String? = null
)

data class PersonUrl(
    val label: String? = "website",
    val value: String
)

/** Patch DTO for PATCH /people/{id}. All fields optional — only non-null sent. */
data class PersonPatch(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val nickname: String? = null,
    val title: String? = null,
    @SerializedName("job_department") val jobDepartment: String? = null,
    @SerializedName("company_id") val companyId: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val emails: List<PersonEmail>? = null,
    val phones: List<PersonPhone>? = null,
    val addresses: List<Address>? = null,
    val urls: List<PersonUrl>? = null,
    val birthday: String? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String? = null,
    @SerializedName("instagram_handle") val instagramHandle: String? = null,
    @SerializedName("facebook_url") val facebookUrl: String? = null,
    @SerializedName("twitter_x_handle") val twitterXHandle: String? = null,
    @SerializedName("tiktok_handle") val tiktokHandle: String? = null,
    @SerializedName("whatsapp_phone") val whatsappPhone: String? = null,
    @SerializedName("previous_employers") val previousEmployers: List<String>? = null,
    val city: String? = null,
    val region: String? = null,
    val country: String? = null,
    @SerializedName("how_we_met") val howWeMet: String? = null,
    @SerializedName("relationship_strength") val relationshipStrength: String? = null,
    @SerializedName("next_followup_at") val nextFollowupAt: String? = null,
    val notes: String? = null,
    @SerializedName("do_not_contact") val doNotContact: Boolean? = null,
    @SerializedName("do_not_contact_reason") val doNotContactReason: String? = null,
    val tags: List<String>? = null
)

// ── Today inbox ──────────────────────────────────────────────────────────────

enum class TodayItemKind {
    @SerializedName("birthday") birthday,
    @SerializedName("cadence_overdue") cadence_overdue,
    @SerializedName("follow_up_due") follow_up_due,
    @SerializedName("job_change") job_change,
    @SerializedName("social_signal") social_signal,
    @SerializedName("anniversary_met") anniversary_met,
    @SerializedName("rhythm_broken") rhythm_broken
}

// ── Daily contact quiz ───────────────────────────────────────────────────────

enum class QuestionKey {
    @SerializedName("notable") notable,
    @SerializedName("how_we_met") how_we_met,
    @SerializedName("last_contacted") last_contacted,
    @SerializedName("relationship_strength") relationship_strength,
    @SerializedName("birthday") birthday,
    @SerializedName("city") city,
    @SerializedName("company") company,
    @SerializedName("other") other
}

data class ContactPrompt(
    val id: String,
    val person: Person,
    @SerializedName("question_key") val questionKey: QuestionKey,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("suggested_responses") val suggestedResponses: List<String> = emptyList(),
    /** Populated by the history endpoint; null for unanswered prompts. */
    val answer: String? = null,
    @SerializedName("answered_at") val answeredAt: String? = null
)

data class AnswerQuizRequest(
    val answer: String,
    val structured: Map<String, @JvmSuppressWildcards Any?>? = null
)

data class AnswerQuizResponse(val person: Person)

data class RhythmInsight(
    @SerializedName("person_id") val personId: String? = null,
    val person: Person? = null,
    val kind: String? = null,
    val message: String? = null,
    val cadence: String? = null,
    @SerializedName("expected_at") val expectedAt: String? = null,
    val data: Map<String, @JvmSuppressWildcards Any?>? = null
)

data class TodayItem(
    val id: String,
    val key: String? = null,
    val kind: TodayItemKind,
    val person: Person,
    val reason: String? = null,
    val priority: Double = 0.0,
    val signal: Map<String, @JvmSuppressWildcards Any?>? = null,
    @SerializedName("suggested_message") val suggestedMessage: String? = null
)

data class TodayResponse(
    val items: List<TodayItem>,
    val count: Int,
    val quiz: List<ContactPrompt> = emptyList(),
    @SerializedName("rhythm_insights") val rhythmInsights: List<RhythmInsight> = emptyList()
)

data class TodayDraftResponse(val draft: String)

data class TodayLogRequest(val via: String, val note: String? = null)
data class TodayLogResponse(
    @SerializedName("last_contacted_at") val lastContactedAt: String?,
    @SerializedName("next_followup_at") val nextFollowupAt: String?
)

// ── Social Activity ──────────────────────────────────────────────────────────

enum class ActivitySource {
    @SerializedName("instagram") instagram,
    @SerializedName("facebook") facebook,
    @SerializedName("linkedin") linkedin,
    @SerializedName("twitter_x") twitter_x,
    @SerializedName("tiktok") tiktok,
    @SerializedName("whatsapp") whatsapp,
    @SerializedName("other") other
}

data class SocialActivity(
    val id: String,
    val source: ActivitySource,
    val kind: String? = null,
    @SerializedName("occurred_at") val occurredAt: String? = null,
    val content: String? = null,
    val location: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("external_url") val externalUrl: String? = null,
    @SerializedName("acknowledged_at") val acknowledgedAt: String? = null
)

// ── Social Groups ────────────────────────────────────────────────────────────

data class SocialGroup(
    val id: String,
    val source: String,
    @SerializedName("external_id") val externalId: String,
    val name: String? = null,
    @SerializedName("member_count") val memberCount: Int? = null,
    @SerializedName("last_synced_at") val lastSyncedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class CreateSocialGroupRequest(
    val source: String,
    @SerializedName("external_id") val externalId: String,
    val name: String? = null
)

data class FacebookGroup(
    val id: String,
    val name: String,
    val url: String? = null,
    @SerializedName("member_count") val memberCount: Int? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class FacebookGroupsResponse(
    val groups: List<FacebookGroup>,
    val error: String? = null,
    val remediation: String? = null
)

data class WhatsappStatus(
    val paired: Boolean,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("qr_required") val qrRequired: Boolean = false
)

data class WhatsappQR(
    val paired: Boolean,
    @SerializedName("qr_data_url") val qrDataUrl: String? = null,
    @SerializedName("expires_in_seconds") val expiresInSeconds: Int? = null
)

data class WhatsappGroup(
    val id: String,
    val name: String,
    @SerializedName("member_count") val memberCount: Int? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null
)

data class WhatsappGroupsResponse(val groups: List<WhatsappGroup>)

// ── Duplicates ───────────────────────────────────────────────────────────────

data class DuplicateCandidate(
    val id: String,
    val people: List<Person> = emptyList(),
    val score: Double? = null,
    val status: String? = null,
    val reason: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class DuplicatesResponse(val data: List<DuplicateCandidate>)

data class MergeDuplicateRequest(
    @SerializedName("primary_id") val primaryId: String,
    val merged: List<String>
)

// ── Voice capture ────────────────────────────────────────────────────────────

data class VoiceCaptureResult(
    val transcript: String? = null,
    val summary: String? = null,
    val discussions: List<Discussion> = emptyList(),
    val tasks: List<Task> = emptyList(),
    @SerializedName("person_refs") val personRefs: List<Person> = emptyList()
)

// ── Natural search ───────────────────────────────────────────────────────────

data class NaturalSearchRequest(val query: String)

data class NaturalSearchHit(
    val person: Person,
    val score: Double? = null,
    val reasoning: String? = null
)

data class NaturalSearchResponse(
    val query: String,
    val results: List<NaturalSearchHit>
)

// ── Push ─────────────────────────────────────────────────────────────────────

data class PushRegisterRequest(
    val platform: String = "android",
    val token: String,
    @SerializedName("device_id") val deviceId: String? = null
)

data class PushUnregisterRequest(val token: String)

// ── Google Accounts ──────────────────────────────────────────────────────────

data class GoogleAccount(
    val id: String,
    val email: String,
    val label: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean = false,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class LinkGoogleAccountRequest(
    @SerializedName("id_token") val idToken: String,
    val label: String? = null
)

data class UpdateGoogleAccountRequest(
    val label: String? = null,
    @SerializedName("is_primary") val isPrimary: Boolean? = null
)

data class Company(
    val id: String,
    val name: String,
    val domain: String?,
    @SerializedName("logo_url") val logoUrl: String?,
    val industry: String?,
    @SerializedName("size_range") val sizeRange: String?,
    val website: String?,
    val notes: String?,
    val tags: List<Tag> = emptyList(),
    @SerializedName("people_count") val peopleCount: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

enum class DiscussionType(val label: String, val emoji: String) {
    call("Call", "📞"),
    meeting("Meeting", "🤝"),
    email("Email", "📧"),
    message("Message", "💬"),
    event("Event", "📅"),
    other("Other", "💡")
}

data class Discussion(
    val id: String,
    val title: String,
    val date: String,
    val type: DiscussionType,
    val summary: String?,
    val body: String?,
    val participants: List<Person>?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class CreateDiscussionRequest(
    val title: String,
    val date: String,
    val type: String,
    val summary: String?,
    @SerializedName("participant_ids") val participantIds: List<String>?
)

data class Note(
    val id: String,
    val title: String?,
    val body: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

enum class TaskPriority { low, medium, high, urgent }

data class Task(
    val id: String,
    val title: String,
    val description: String?,
    @SerializedName("due_at") val dueAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    val priority: TaskPriority,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    val isComplete: Boolean get() = completedAt != null
}

data class TimelineEvent(
    val type: String,
    val date: String,
    val data: Map<String, Any?>
)

data class FeedItem(
    val id: String,
    @SerializedName("subject_type") val subjectType: String,
    @SerializedName("subject_id") val subjectId: String,
    val verb: String,
    val payload: Map<String, Any?>,
    @SerializedName("created_at") val createdAt: String
) {
    val entityType: String get() = subjectType.substringAfterLast("\\").lowercase()
    val verbLabel: String get() = when (verb) {
        "created" -> "created"
        "updated" -> "updated"
        "contacted" -> "contacted"
        "task_completed" -> "completed task on"
        else -> verb
    }
}

data class SearchResult(
    val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val url: String
)

data class SearchResponse(
    val query: String,
    val results: List<SearchResult>
)

data class ImportResult(
    val imported: Int,
    val skipped: Int
)

// ── People health (Contact Review) ───────────────────────────────────────────

data class PeopleHealth(
    val total: Int,
    val buckets: Map<String, HealthBucket> = emptyMap()
)

data class HealthBucket(
    val count: Int,
    val samples: List<HealthSample> = emptyList()
)

data class HealthSample(
    val id: String,
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null
) {
    val displayName: String
        get() {
            val combined = listOfNotNull(firstName, lastName)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" ")
            if (combined.isNotEmpty()) return combined
            return email ?: "Unnamed contact"
        }
}

data class CreateCompanyRequest(
    val name: String,
    val domain: String? = null,
    val industry: String? = null,
    val website: String? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String? = null,
    val notes: String? = null
)

data class CreatePersonRequest(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    /** Optional multi-value lists. When provided, server replaces person_emails/person_phones rows. */
    val emails: List<PersonEmail>? = null,
    val phones: List<PersonPhone>? = null,
    @SerializedName("linkedin_url") val linkedinUrl: String? = null,
    val title: String? = null,
    @SerializedName("company_name") val companyName: String? = null,
    val notes: String? = null
)

data class CreateNoteRequest(
    val title: String?,
    val body: String,
    @SerializedName("notable_type") val notableType: String = "App\\Models\\Person",
    @SerializedName("notable_id") val notableId: String
)

data class UpdateNoteRequest(
    val title: String? = null,
    val body: String? = null
)

data class CreateTaskRequest(
    val title: String,
    val description: String? = null,
    @SerializedName("due_at") val dueAt: String? = null,
    val priority: String? = null,
    @SerializedName("taskable_type") val taskableType: String = "App\\Models\\Person",
    @SerializedName("taskable_id") val taskableId: String
)

data class Paginated<T>(
    val data: List<T>,
    val total: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int
)
