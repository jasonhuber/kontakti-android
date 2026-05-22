package com.kontakti.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: UserProfile)
data class UserProfile(val id: String, val name: String, val email: String, val username: String?)

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
    val email: String?,
    val phone: String?,
    @SerializedName("linkedin_url") val linkedinUrl: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("company_id") val companyId: String?,
    val company: Company?,
    val title: String?,
    @SerializedName("relationship_strength") val relationshipStrength: RelationshipStrength,
    @SerializedName("last_contacted_at") val lastContactedAt: String?,
    @SerializedName("next_followup_at") val nextFollowupAt: String?,
    val notes: String?,
    val tags: List<Tag> = emptyList(),
    @SerializedName("discussions_count") val discussionsCount: Int?,
    @SerializedName("tasks_count") val tasksCount: Int?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
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

data class Paginated<T>(
    val data: List<T>,
    val total: Int,
    @SerializedName("per_page") val perPage: Int,
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int
)
