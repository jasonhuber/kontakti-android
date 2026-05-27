package com.kontakti.data.repository

import com.kontakti.data.model.*
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialGroupRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(): List<SocialGroup> = api.listSocialGroups()
    suspend fun create(source: String, externalId: String, name: String?): SocialGroup =
        api.createSocialGroup(CreateSocialGroupRequest(source, externalId, name))
    suspend fun sync(id: String) = api.syncSocialGroup(id)
    suspend fun delete(id: String) = api.deleteSocialGroup(id)

    suspend fun facebookGroups(): FacebookGroupsResponse = api.getFacebookGroups()
    suspend fun whatsappStatus(): WhatsappStatus = api.getWhatsappStatus()
    suspend fun whatsappQR(): WhatsappQR = api.getWhatsappQR()
    suspend fun whatsappGroups(): WhatsappGroupsResponse = api.getWhatsappGroups()
}
