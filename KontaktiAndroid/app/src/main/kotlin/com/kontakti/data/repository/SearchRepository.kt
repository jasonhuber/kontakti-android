package com.kontakti.data.repository

import com.kontakti.data.model.NaturalSearchRequest
import com.kontakti.data.model.NaturalSearchResponse
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(private val api: ApiService) {
    suspend fun natural(query: String): NaturalSearchResponse =
        api.searchNatural(NaturalSearchRequest(query))
}
