package digital.tonima.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.model.AlarmOffset
import digital.tonima.core.model.Event
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.receiver.AlarmReceiver.Companion.ACTION_ALARM_TRIGGERED
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_ID
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_START_TIME
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_MEETING_URL
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import logcat.logcat
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = EventAlarmScheduler::class)
class EventAlarmSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesRepository: AppPreferencesRepository,
    ) : EventAlarmScheduler {
        @VisibleForTesting
        internal var clock: Clock = Clock.systemDefaultZone()

        private val alarmManager = context.getSystemService(AlarmManager::class.java)

        override fun schedule(
            event: Event,
            triggerTime: Long?,
        ) {
            val skipWeekends =
                runBlocking {
                    preferencesRepository.isSkipWeekendsEnabled().firstOrNull() ?: false
                }

            if (skipWeekends) {
                val eventDayOfWeek =
                    if (event.isAllDay) {
                        Instant.ofEpochMilli(event.startTime).atZone(ZoneId.of("UTC")).dayOfWeek
                    } else {
                        Instant.ofEpochMilli(event.startTime).atZone(ZoneId.systemDefault()).dayOfWeek
                    }
                if (eventDayOfWeek == java.time.DayOfWeek.SATURDAY || eventDayOfWeek == java.time.DayOfWeek.SUNDAY) {
                    logcat { "Skipping alarm for event on weekend: ${event.title}" }
                    return
                }
            }

            val alarmTime =
                if (triggerTime != null) {
                    triggerTime
                } else if (event.isAllDay) {
                    val allDayAlarmsEnabled =
                        runBlocking {
                            preferencesRepository.isAllDayAlarmsEnabled().firstOrNull() ?: true
                        }

                    if (!allDayAlarmsEnabled) {
                        logcat {
                            "Skipping alarm for all-day event (disabled in settings): ${event.title}"
                        }
                        return
                    }

                    val alarmHour =
                        runBlocking {
                            preferencesRepository.getAllDayAlarmHour().firstOrNull() ?: 9
                        }

                    val instant = Instant.ofEpochMilli(event.startTime)
                    val localDate = instant.atZone(ZoneId.of("UTC")).toLocalDate()
                    val alarmDateTime = localDate.atTime(LocalTime.of(alarmHour, 0))
                    alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } else {
                    val offsetMinutes =
                        runBlocking {
                            preferencesRepository.getAlarmOffsetMinutes().firstOrNull() ?: 0L
                        }
                    val offset = AlarmOffset.fromMinutes(offsetMinutes)
                    event.startTime - java.util.concurrent.TimeUnit.MINUTES.toMillis(offset.minutes)
                }

            logcat {
                "Scheduling alarm for event: ${event.title} at $alarmTime " +
                    "(original: ${event.startTime}, isAllDay: ${event.isAllDay}) " +
                    "with ID: ${event.uniqueIntentId}"
            }

            if (alarmTime <= clock.millis()) {
                logcat {
                    "Skipping alarm for event '${event.title}': alarm time ($alarmTime) is in the past."
                }
                return
            }

            val canScheduleExact =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

            val intent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    data = "kairos://alarm/${event.uniqueIntentId}".toUri()

                    putExtra(EXTRA_EVENT_TITLE, event.title)
                    putExtra(EXTRA_UNIQUE_ID, event.uniqueIntentId)
                    putExtra(EXTRA_EVENT_ID, event.id)
                    putExtra(EXTRA_EVENT_START_TIME, event.startTime)
                    putExtra(EXTRA_MEETING_URL, event.meetingUrl)
                    putExtra(AlarmReceiver.EXTRA_EVENT_END_TIME, event.endTime)
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    event.uniqueIntentId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            try {
                if (canScheduleExact) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val showIntent =
                        PendingIntent.getActivity(
                            context,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(alarmTime, showIntent),
                        pendingIntent,
                    )
                    logcat {
                        "Alarm scheduled EXACT for event: ${event.title}"
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent,
                    )
                    logcat {
                        "FALLBACK: Alarm scheduled INEXACT (exact alarms not permitted) for event: ${event.title}"
                    }
                }
            } catch (e: SecurityException) {
                logcat {
                    "ERROR: Could not schedule alarm for event ${event.title}: ${e.message}"
                }
            }
        }

        override fun scheduleSnooze(
            eventTitle: String,
            uniqueId: Int,
            eventId: Long,
            startTime: Long,
            meetingUrl: String?,
        ) {
            val snoozeMinutes =
                runBlocking {
                    preferencesRepository.getSnoozeTimeMinutes().firstOrNull() ?: 10
                }
            val alarmTime = clock.millis() + java.util.concurrent.TimeUnit.MINUTES.toMillis(snoozeMinutes.toLong())

            logcat {
                "Scheduling SNOOZE for event: $eventTitle at $alarmTime with ID: $uniqueId"
            }

            val intent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    data = "kairos://alarm/$uniqueId/snooze".toUri()

                    putExtra(EXTRA_EVENT_TITLE, eventTitle)
                    putExtra(EXTRA_UNIQUE_ID, uniqueId)
                    putExtra(EXTRA_EVENT_ID, eventId)
                    putExtra(EXTRA_EVENT_START_TIME, startTime)
                    putExtra(EXTRA_MEETING_URL, meetingUrl)
                }

            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    uniqueId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val canScheduleExact =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }

            try {
                if (canScheduleExact) {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    val showIntent =
                        PendingIntent.getActivity(
                            context,
                            0,
                            launchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                        )
                    alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarmTime, showIntent), pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
                }
            } catch (e: SecurityException) {
                logcat { "ERROR: Could not schedule snooze for $eventTitle: ${e.message}" }
            }
        }

        override fun cancel(event: Event) {
            logcat {
                "Cancelling alarm for event ID: ${event.uniqueIntentId}"
            }
            val intent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    data = "kairos://alarm/${event.uniqueIntentId}".toUri()
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    event.uniqueIntentId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            alarmManager.cancel(pendingIntent)
        }
    }
