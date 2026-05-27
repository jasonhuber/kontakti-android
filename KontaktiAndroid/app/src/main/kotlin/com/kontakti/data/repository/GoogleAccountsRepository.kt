package com.kontakti.data.repository

import com.kontakti.data.model.GoogleAccount
import com.kontakti.data.model.LinkGoogleAccountRequest
import com.kontakti.data.model.UpdateGoogleAccountRequest
import com.kontakti.data.network.ApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAccountsRepository @Inject constructor(private val api: ApiService) {
    suspend fun list(): List<GoogleAccount> = api.listGoogleAccounts()
    suspend fun link(idToken: String, label: String?): GoogleAccount =
        api.linkGoogleAccount(LinkGoogleAccountRequest(idToken, label))
    suspend fun update(id: String, label: String? = null, isPrimary: Boolean? = null): GoogleAccount =
        api.updateGoogleAccount(id, UpdateGoogleAccountRequest(label, isPrimary))
    suspend fun unlink(id: String) = api.unlinkGoogleAccount(id)
}
