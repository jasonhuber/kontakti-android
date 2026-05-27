package com.kontakti.data.network

import com.google.gson.Gson
import com.kontakti.data.model.EnrichmentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrichmentService @Inject constructor() {

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val baseUrl = "https://enrich.kontakti.app"
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun enrich(linkedinUrl: String): EnrichmentResult = withContext(Dispatchers.IO) {
        val body = gson.toJson(mapOf("url" to linkedinUrl))
            .toRequestBody(json)

        val request = Request.Builder()
            .url("$baseUrl/api/enrich")
            .addHeader("Accept", "application/json")
            .addHeader("X-Api-Key", EnrichmentSecrets.API_KEY)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful || responseBody == null) {
            throw EnrichmentException(
                "Enrichment failed (HTTP ${response.code}): ${responseBody ?: "empty response"}"
            )
        }

        gson.fromJson(responseBody, EnrichmentResult::class.java)
            ?: throw EnrichmentException("Could not parse enrichment response")
    }
}

class EnrichmentException(message: String) : Exception(message)
