package com.kontakti.data.repository

import com.kontakti.data.local.CompanyEntity
import com.kontakti.data.local.KontaktiDao
import com.kontakti.data.model.Company
import com.kontakti.data.network.ApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanyRepository @Inject constructor(
    private val api: ApiService,
    private val dao: KontaktiDao
) {
    fun getCompanies(query: String? = null): Flow<List<CompanyEntity>> =
        if (query.isNullOrBlank()) dao.getCompanies()
        else dao.searchCompanies(query)

    suspend fun refresh() {
        try {
            var page = 1
            var lastPage: Int
            do {
                val result = api.listCompanies(page = page)
                lastPage = result.lastPage
                dao.upsertCompanies(result.data.map { it.toEntity() })
                page++
            } while (page <= lastPage)
        } catch (_: Exception) {
            // Offline — Room data remains available
        }
    }
}

// ── Mapping ───────────────────────────────────────────────────────────────────

fun Company.toEntity() = CompanyEntity(
    id = id,
    name = name,
    domain = domain,
    logoUrl = logoUrl,
    industry = industry,
    sizeRange = sizeRange,
    website = website,
    peopleCount = peopleCount,
    updatedAt = updatedAt
)
