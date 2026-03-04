package digital.tonima.kairos.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import digital.tonima.core.repository.CalendarRepository
import digital.tonima.core.sync.WearSyncSchema.KEY_ALL_DAY
import digital.tonima.core.sync.WearSyncSchema.KEY_EVENTS
import digital.tonima.core.sync.WearSyncSchema.KEY_GENERATED_AT
import digital.tonima.core.sync.WearSyncSchema.KEY_ID
import digital.tonima.core.sync.WearSyncSchema.KEY_RECUR
import digital.tonima.core.sync.WearSyncSchema.KEY_START
import digital.tonima.core.sync.WearSyncSchema.KEY_TITLE
import digital.tonima.core.sync.WearSyncSchema.PATH_EVENTS_24H
import kotlinx.coroutines.tasks.await
import logcat.LogPriority
import logcat.logcat
import java.util.concurrent.TimeUnit

@HiltWorker
class PhoneEventSyncWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val calendarRepository: CalendarRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            try {
                val now = System.currentTimeMillis()
                val end = now + TimeUnit.HOURS.toMillis(24)
                val ymNow = java.time.YearMonth.now()
                val ymNext = ymNow.plusMonths(1)
                val monthEvents =
                    (
                        calendarRepository.getEventsForMonth(ymNow) + calendarRepository.getEventsForMonth(ymNext)
                    ).filter { it.startTime in now..end }
                        .sortedBy { it.startTime }
                val events = monthEvents
                logcat { "Phone→Wear sync: sending ${events.size} events." }

                val dataClient: DataClient = Wearable.getDataClient(applicationContext)
                val putReq = PutDataMapRequest.create(PATH_EVENTS_24H)
                val map = putReq.dataMap
                val list = ArrayList<com.google.android.gms.wearable.DataMap>()
                events.forEach { e ->
                    val dm =
                        com.google.android.gms.wearable
                            .DataMap()
                    dm.putLong(KEY_ID, e.id)
                    dm.putString(KEY_TITLE, e.title)
                    dm.putLong(KEY_START, e.startTime)
                    dm.putBoolean(KEY_RECUR, e.isRecurring)
                    dm.putBoolean(KEY_ALL_DAY, e.isAllDay)
                    list.add(dm)
                }
                map.putDataMapArrayList(KEY_EVENTS, list)
                map.putLong(KEY_GENERATED_AT, now)
                val request = putReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(request).await()
                Result.success()
            } catch (t: Throwable) {
                logcat(LogPriority.ERROR) { "PhoneEventSyncWorker failed: ${t.localizedMessage}" }
                Result.retry()
            }

        companion object {
            const val UNIQUE_WORK_NAME = "phone-event-sync"

            fun enqueuePeriodic(context: Context) {
                val constraints = Constraints.Builder().build()
                val periodic =
                    PeriodicWorkRequestBuilder<PhoneEventSyncWorker>(15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodic,
                )
                // Use expedited work for immediate sync to bypass work profile throttling
                val once =
                    OneTimeWorkRequestBuilder<PhoneEventSyncWorker>()
                        .setConstraints(constraints)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                WorkManager.getInstance(context).enqueue(once)
            }
        }
    }
