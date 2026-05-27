package com.kontakti

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessaging
import com.kontakti.data.repository.PushRepository
import com.kontakti.data.sync.SyncWorker
import com.kontakti.util.NetworkMonitor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KontaktiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var workManager: WorkManager
    @Inject lateinit var pushRepository: PushRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        observeConnectivity()
        registerFcmToken()
    }

    private fun registerFcmToken() {
        // Best-effort: only succeeds if google-services.json is configured.
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result ?: return@addOnCompleteListener
                appScope.launch { pushRepository.register(token) }
            }
        }
    }

    private fun observeConnectivity() {
        appScope.launch {
            var previouslyOffline = false
            networkMonitor.isOnline.collect { online ->
                if (online && previouslyOffline) {
                    workManager.enqueueUniqueWork(
                        SyncWorker.WORK_NAME,
                        ExistingWorkPolicy.KEEP,
                        SyncWorker.buildRequest()
                    )
                }
                previouslyOffline = !online
            }
        }
    }
}
