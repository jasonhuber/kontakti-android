package com.kontakti.data.repository

import com.kontakti.data.model.CreateDiscussionRequest
import com.kontakti.data.model.Discussion
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscussionsRepository @Inject constructor(private val api: ApiService) {
    suspend fun listForPerson(personId: String): List<Discussion> = api.getPersonDiscussions(personId)
    suspend fun create(req: CreateDiscussionRequest): Discussion = api.createDiscussion(req)
}
