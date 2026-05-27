package com.kontakti.data.repository

import com.kontakti.data.model.TodayDraftResponse
import com.kontakti.data.model.TodayLogRequest
import com.kontakti.data.model.TodayLogResponse
import com.kontakti.data.model.TodayResponse
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodayRepository @Inject constructor(private val api: ApiService) {
    suspend fun load(limit: Int = 10): TodayResponse = api.getToday(limit)
    suspend fun draft(key: String): TodayDraftResponse = api.draftTodayMessage(key)
    suspend fun log(key: String, via: String, note: String? = null): TodayLogResponse =
        api.logTodayContact(key, TodayLogRequest(via, note))
    suspend fun snooze(key: String) = api.snoozeTodayItem(key)
    suspend fun skip(key: String) = api.skipTodayItem(key)
}
