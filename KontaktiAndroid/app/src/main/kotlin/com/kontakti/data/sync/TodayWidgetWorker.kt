package com.kontakti.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kontakti.data.repository.TodayRepository
import com.kontakti.widget.TodayWidgetState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic worker that refreshes the home-screen widget data in the background.
 * Calls /today, writes a slim snapshot into the widget DataStore, and triggers
 * a Glance re-render. Scheduled by [TodayWidgetReceiver] when the widget is added.
 */
@HiltWorker
class TodayWidgetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val todayRepo: TodayRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val resp = todayRepo.load()
            TodayWidgetState.update(applicationContext, resp.items, resp.count)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "kontakti_today_widget_refresh"
    }
}
