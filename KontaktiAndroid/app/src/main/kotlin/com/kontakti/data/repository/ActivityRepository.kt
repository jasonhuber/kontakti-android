package com.kontakti.data.repository

import com.kontakti.data.model.SocialActivity
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(personId: String): List<SocialActivity> = api.getPersonActivity(personId)
    suspend fun refresh(personId: String) = api.refreshPersonActivity(personId)
    suspend fun acknowledge(activityId: String) = api.acknowledgeActivity(activityId)
}
