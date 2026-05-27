package com.kontakti.data.model

import com.google.gson.annotations.SerializedName

data class EnrichRequest(val url: String)

data class EnrichedCompany(
    val name: String?,
    val domain: String?,
    val industry: String?,
    @SerializedName("size_range") val sizeRange: String?,
    val website: String?
)

data class EnrichedPerson(
    @SerializedName("first_name") val firstName: String?,
    @SerializedName("last_name") val lastName: String?,
    val email: String?,
    val phone: String?,
    @SerializedName("linkedin_url") val linkedinUrl: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val title: String?,
    val company: EnrichedCompany?,
    val metadata: Map<String, Any?>?
)

data class EnrichmentResult(
    val person: EnrichedPerson,
    val source: String,
    val model: String
)
