package digital.tonima.kairos.wear.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.model.Event
import kotlinx.coroutines.flow.firstOrNull
import logcat.LogPriority
import logcat.logcat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
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

                val offsetMinutes = appPreferencesRepository.getAlarmOffsetMinutes().firstOrNull() ?: 0L
                val allDayAlarmsEnabled = appPreferencesRepository.isAllDayAlarmsEnabled().firstOrNull() ?: true
                val allDayAlarmHour = appPreferencesRepository.getAllDayAlarmHour().firstOrNull() ?: 9

                val events = WearEventCache.load(applicationContext).sortedBy { it.startTime }
                val disabledInstanceIds = appPreferencesRepository.getDisabledEventIds().firstOrNull() ?: emptySet()
                val disabledSeriesIds = appPreferencesRepository.getDisabledSeriesIds().firstOrNull() ?: emptySet()

                val now = System.currentTimeMillis()
                // PhoneEventSyncWorker only ever sends events starting within the next 24h
                // (PATH_EVENTS_24H), so the cache never holds anything further out than that.
                // Keep this window aligned with that sender-side cutoff — a longer window here
                // is dead code that can never match anything in the cache.
                val scheduleWindowEnd = now + TimeUnit.HOURS.toMillis(24)
                val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

                logcat {
                    "Wear: Evaluating ${events.size} cached events for scheduling window ${sdf.format(
                        Date(now),
                    )}..${sdf.format(Date(scheduleWindowEnd))}"
                }

                val toSchedule =
                    events
                        .filter { event ->
                            val alarmFireTime =
                                if (event.isAllDay) {
                                    if (!allDayAlarmsEnabled) return@filter false
                                    val eventDate =
                                        Instant
                                            .ofEpochMilli(event.startTime)
                                            .atZone(ZoneId.of("UTC"))
                                            .toLocalDate()
                                    eventDate
                                        .atTime(LocalTime.of(allDayAlarmHour, 0))
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                                        .toEpochMilli()
                                } else {
                                    event.startTime - TimeUnit.MINUTES.toMillis(offsetMinutes)
                                }
                            alarmFireTime in (now + 1)..scheduleWindowEnd
                        }.filter { e ->
                            val instanceDisabled = disabledInstanceIds.contains(e.uniqueIntentId.toString())
                            val seriesDisabled = disabledSeriesIds.contains(e.id.toString())
                            !(instanceDisabled || seriesDisabled)
                        }

                cancelRemovedEvents(events, now)

                if (toSchedule.isEmpty()) {
                    logcat { "Wear: No cached events to schedule in window." }
                } else {
                    toSchedule.forEach { e ->
                        logcat { "Wear: Scheduling '${e.title}' at ${sdf.format(Date(e.startTime))} (from cache)" }
                        // Same traffic-aware trigger the phone uses; a departure time already in the
                        // past falls back to the regular alarm instead of dropping it.
                        scheduler.schedule(e, e.departureTime?.takeIf { it > now })
                    }
                }
                rememberScheduled(toSchedule, events, now)
                Result.success()
            } catch (t: Throwable) {
                logcat(LogPriority.ERROR) { "Wear: CachedEventSchedulingWorker failed: ${t.localizedMessage}" }
                Result.failure()
            }
        }

        /**
         * Cancels alarms the watch scheduled for events the phone no longer sends (deleted or
         * moved). Events that already started are left alone: the phone stops sending them once
         * they start, and cancelling would also kill a snooze the user is waiting for.
         */
        private fun cancelRemovedEvents(
            cachedEvents: List<Event>,
            now: Long,
        ) {
            val cachedIds = cachedEvents.map { it.uniqueIntentId }.toSet()
            WearEventCache
                .loadScheduled(applicationContext)
                .filter { it.uniqueIntentId !in cachedIds && it.startTime > now }
                .forEach { e ->
                    logcat { "Wear: Cancelling alarm for '${e.title}', no longer sent by the phone." }
                    scheduler.cancel(e)
                }
        }

        private fun rememberScheduled(
            scheduled: List<Event>,
            cachedEvents: List<Event>,
            now: Long,
        ) {
            val cachedIds = cachedEvents.map { it.uniqueIntentId }.toSet()
            val stillTracked =
                WearEventCache.loadScheduled(applicationContext).filter {
                    it.uniqueIntentId in cachedIds && it.startTime > now
                }
            WearEventCache.saveScheduled(
                applicationContext,
                (scheduled + stillTracked).distinctBy { it.uniqueIntentId },
            )
        }
    }
