package com.kontakti.data.repository

import com.kontakti.data.model.DuplicateCandidate
import com.kontakti.data.model.MergeDuplicateRequest
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicatesRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(status: String = "pending"): List<DuplicateCandidate> =
        api.listDuplicates(status)
    suspend fun scan() = api.scanDuplicates()
    suspend fun merge(id: String, primaryId: String, merged: List<String>) =
        api.mergeDuplicate(id, MergeDuplicateRequest(primaryId, merged))
    suspend fun dismiss(id: String) = api.dismissDuplicate(id)
}
