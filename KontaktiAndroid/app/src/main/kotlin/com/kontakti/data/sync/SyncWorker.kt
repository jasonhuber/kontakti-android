package com.kontakti.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.gson.Gson
import com.kontakti.data.local.PendingSyncEntity
import com.kontakti.data.local.SyncOperation
import com.kontakti.data.model.CreateDiscussionRequest
import com.kontakti.data.network.ApiService
import com.kontakti.data.repository.PeopleRepository
import com.kontakti.data.repository.CompanyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueue: SyncQueue,
    private val api: ApiService,
    private val gson: Gson,
    private val peopleRepo: PeopleRepository,
    private val companyRepo: CompanyRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pending = syncQueue.getPending()
        var anyFailed = false

        for (item in pending) {
            val success = processSyncItem(item)
            if (success) {
                syncQueue.remove(item)
            } else {
                anyFailed = true
            }
        }

        // After flushing mutations, refresh caches
        peopleRepo.refresh()
        companyRepo.refresh()

        if (anyFailed) Result.retry() else Result.success()
    }

    private suspend fun processSyncItem(item: PendingSyncEntity): Boolean {
        return try {
            when (SyncOperation.valueOf(item.operation)) {
                SyncOperation.LOG_DISCUSSION -> {
                    val req = gson.fromJson(item.payload, CreateDiscussionRequest::class.java)
                    api.createDiscussion(req)
                    Unit
                }
                SyncOperation.CREATE_PERSON -> {
                    // Placeholder: extend ApiService with POST /people when backend supports it
                }
                SyncOperation.COMPLETE_TASK -> {
                    // Placeholder: extend ApiService with PATCH /tasks/{id}/complete
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        const val WORK_NAME = "kontakti_sync"

        fun buildRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
    }
}
