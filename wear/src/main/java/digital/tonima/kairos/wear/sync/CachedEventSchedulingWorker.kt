package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import kotlinx.coroutines.flow.firstOrNull
import logcat.LogPriority
import logcat.logcat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class CachedEventSchedulingWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val appPreferencesRepository: AppPreferencesRepository,
        private val scheduler: EventAlarmScheduler,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            return try {
                val isGlobalAlarmEnabled = appPreferencesRepository.isGlobalAlarmEnabled().firstOrNull() ?: true
                if (!isGlobalAlarmEnabled) {
                    logcat(LogPriority.INFO) { "Wear: Global alarms disabled; not scheduling." }
                    return Result.success()
                }

                val events = WearEventCache.load(applicationContext).sortedBy { it.startTime }
                val disabledInstanceIds = appPreferencesRepository.getDisabledEventIds().firstOrNull() ?: emptySet()
                val disabledSeriesIds = appPreferencesRepository.getDisabledSeriesIds().firstOrNull() ?: emptySet()

                val now = System.currentTimeMillis()
                val scheduleWindowEnd = now + TimeUnit.MINUTES.toMillis(75)
                val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

                logcat {
                    "Wear: Evaluating ${events.size} cached events for scheduling window ${sdf.format(
                        Date(now),
                    )}..${sdf.format(Date(scheduleWindowEnd))}"
                }

                val toSchedule =
                    events
                        .filter { it.startTime in (now + 1)..scheduleWindowEnd }
                        .filter { e ->
                            val instanceDisabled = disabledInstanceIds.contains(e.uniqueIntentId.toString())
                            val seriesDisabled = disabledSeriesIds.contains(e.id.toString())
                            !(instanceDisabled || seriesDisabled)
                        }

                if (toSchedule.isEmpty()) {
                    logcat { "Wear: No cached events to schedule in window." }
                } else {
                    toSchedule.forEach { e ->
                        logcat { "Wear: Scheduling '${e.title}' at ${sdf.format(Date(e.startTime))} (from cache)" }
                        scheduler.schedule(e)
                    }
                }
                Result.success()
            } catch (t: Throwable) {
                logcat(LogPriority.ERROR) { "Wear: CachedEventSchedulingWorker failed: ${t.localizedMessage}" }
                Result.failure()
            }
        }
    }
