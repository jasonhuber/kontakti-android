package com.kontakti

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        observeConnectivity()
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
