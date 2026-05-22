package com.kontakti.data.sync

import com.google.gson.Gson
import com.kontakti.data.local.KontaktiDao
import com.kontakti.data.local.PendingSyncEntity
import com.kontakti.data.local.SyncOperation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncQueue @Inject constructor(
    private val dao: KontaktiDao,
    private val gson: Gson
) {
    /** Serialize any payload object as JSON and enqueue it. */
    suspend fun <T> enqueue(operation: SyncOperation, payload: T) {
        dao.enqueueSyncItem(
            PendingSyncEntity(
                operation = operation.name,
                payload = gson.toJson(payload)
            )
        )
    }

    suspend fun getPending(): List<PendingSyncEntity> = dao.getPendingSyncItems()

    suspend fun remove(item: PendingSyncEntity) = dao.deleteSyncItem(item)
}
