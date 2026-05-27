package com.kontakti.data.repository

import com.kontakti.data.model.PushRegisterRequest
import com.kontakti.data.model.PushUnregisterRequest
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRepository @Inject constructor(private val api: ApiService) {
    suspend fun register(token: String, deviceId: String? = null) =
        runCatching {
            api.registerPush(PushRegisterRequest(token = token, deviceId = deviceId))
        }

    suspend fun unregister(token: String) =
        runCatching { api.unregisterPush(PushUnregisterRequest(token)) }
}
