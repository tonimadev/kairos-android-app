package digital.tonima.core.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.analytics.Analytics
import digital.tonima.core.repository.AppPreferencesRepositoryImpl
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import logcat.logcat
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SchedulerEntryPoint {
        fun scheduler(): EventAlarmScheduler
    }

    companion object {
        const val ACTION_ALARM_TRIGGERED = "digital.tonima.core.ALARM_TRIGGERED"
        const val EXTRA_EVENT_TITLE = "EXTRA_EVENT_TITLE"
        const val EXTRA_UNIQUE_ID = "EXTRA_UNIQUE_ID"
        const val EXTRA_EVENT_ID = "EXTRA_EVENT_ID"
        const val EXTRA_EVENT_START_TIME = "EXTRA_EVENT_START_TIME"
        const val EXTRA_MEETING_URL = "EXTRA_MEETING_URL"
        const val EXTRA_EVENT_END_TIME = "EXTRA_EVENT_END_TIME"
        const val ACTION_SNOOZE = "digital.tonima.core.ACTION_SNOOZE"
        const val ACTION_FOCUS_END = "digital.tonima.core.ACTION_FOCUS_END"
        const val EXTRA_SOURCE = "EXTRA_SOURCE"
    }

    @Inject
    lateinit var scheduler: EventAlarmScheduler

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var appStatusRepository: digital.tonima.core.repository.AppPreferencesRepository

    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_SNOOZE -> handleSnooze(context, intent)
            ACTION_FOCUS_END -> handleFocusEnd(context)
            ACTION_ALARM_TRIGGERED -> handleAlarmTriggered(context, intent)
            else -> return
        }
    }

    private fun handleSnooze(
        context: Context,
        intent: Intent,
    ) {
        val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""
        val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, -1)
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)
        val source = intent.getStringExtra(EXTRA_SOURCE) ?: Analytics.SOURCE_NOTIFICATION

        if (source == Analytics.SOURCE_NOTIFICATION) {
            analytics.logEvent(
                Analytics.EVENT_ALARM_SNOOZE,
                mapOf(Analytics.PARAM_SOURCE to Analytics.SOURCE_NOTIFICATION),
            )
        }

        kotlinx.coroutines.GlobalScope.launch {
            try {
                appStatusRepository.incrementSnoozeCount()
            } catch (e: Exception) {
                logcat { "Failed to increment snooze count: ${e.message}" }
            }
        }

        AlarmSoundAndVibrateService.stopAlarm(context)

        try {
            val schedulerEntryPoint =
                fromApplication(
                    context.applicationContext,
                    SchedulerEntryPoint::class.java,
                )
            schedulerEntryPoint.scheduler().scheduleSnooze(eventTitle, uniqueId, eventId, startTime, meetingUrl)
        } catch (e: Exception) {
            logcat { "Failed to access EventAlarmScheduler via Hilt: ${e.message}" }
        }
    }

    private fun handleFocusEnd(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                logcat { "Auto Focus Mode: DND disabled (meeting ended)" }
            }
        } catch (e: Exception) {
            logcat { "Auto Focus Mode: Failed to disable DND: ${e.message}" }
        }
    }

    private fun handleAlarmTriggered(
        context: Context,
        intent: Intent,
    ) {
        val eventTitle =
            intent.getStringExtra(EXTRA_EVENT_TITLE) ?: context.getString(R.string.upcoming_event)
        val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, System.currentTimeMillis().toInt())
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)
        val meetingUrl = intent.getStringExtra(EXTRA_MEETING_URL)
        val endTime = intent.getLongExtra(EXTRA_EVENT_END_TIME, -1L)

        analytics.logEvent(
            Analytics.EVENT_ALARM_FIRED,
            mapOf(Analytics.PARAM_HAS_MEETING_URL to !meetingUrl.isNullOrEmpty()),
        )

        // ── Auto Focus Mode: enable DND + schedule end ──────────────────────
        val isAutoFocusEnabled =
            try {
                runBlocking {
                    AppPreferencesRepositoryImpl(context.applicationContext)
                        .isAutoFocusModeEnabled().first()
                }
            } catch (_: Exception) {
                false
            }

        if (isAutoFocusEnabled && endTime > 0L) {
            enableAutoFocusMode(context, endTime, uniqueId)
        }

        // ── Auto Join: skip alarm screen and open meeting URL ───────────────
        val isAutoJoinEnabled =
            try {
                runBlocking {
                    AppPreferencesRepositoryImpl(context.applicationContext)
                        .isAutoJoinEnabled().first()
                }
            } catch (_: Exception) {
                false
            }

        if (isAutoJoinEnabled && !meetingUrl.isNullOrEmpty()) {
            logcat { "Auto-Join: Opening meeting URL directly for '$eventTitle'" }
            analytics.logEvent(
                Analytics.EVENT_ALARM_STOP,
                mapOf(
                    Analytics.PARAM_SOURCE to "AUTO_JOIN",
                    "action" to "join_meeting",
                ),
            )
            analytics.logEvent(Analytics.EVENT_JOIN_MEETING)

            try {
                val meetIntent =
                    Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl)).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                context.startActivity(meetIntent)
            } catch (e: Exception) {
                logcat { "Auto-Join: Failed to open meeting URL: ${e.message}" }
                // Fallback to normal alarm flow
                startNormalAlarm(context, eventTitle, uniqueId, eventId, startTime, meetingUrl)
            }
            return
        }

        // ── Normal alarm flow ───────────────────────────────────────────────
        startNormalAlarm(context, eventTitle, uniqueId, eventId, startTime, meetingUrl)
    }

    private fun enableAutoFocusMode(
        context: Context,
        endTime: Long,
        uniqueId: Int,
    ) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                logcat { "Auto Focus Mode: DND enabled (meeting started)" }

                // Schedule DND disable at meeting end
                val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                val focusEndIntent =
                    Intent(context, AlarmReceiver::class.java).apply {
                        action = ACTION_FOCUS_END
                    }
                val pendingIntent =
                    android.app.PendingIntent.getBroadcast(
                        context,
                        uniqueId + 0x0F0C05, // offset to avoid collision with alarm PendingIntents
                        focusEndIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
                    )

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            endTime,
                            pendingIntent,
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP,
                            endTime,
                            pendingIntent,
                        )
                    }
                    logcat { "Auto Focus Mode: Scheduled DND disable at $endTime" }
                } catch (e: SecurityException) {
                    logcat { "Auto Focus Mode: Cannot schedule focus-end alarm: ${e.message}" }
                }
            } else {
                logcat { "Auto Focus Mode: No notification policy access" }
            }
        } catch (e: Exception) {
            logcat { "Auto Focus Mode: Failed to enable DND: ${e.message}" }
        }
    }

    private fun startNormalAlarm(
        context: Context,
        eventTitle: String,
        uniqueId: Int,
        eventId: Long,
        startTime: Long,
        meetingUrl: String?,
    ) {
        // No Wear OS (ou se o contexto indicar hardware.type.watch), iniciamos a Activity diretamente
        // para evitar ForegroundServiceStartNotAllowedException
        val isWatch = context.packageManager.hasSystemFeature("android.hardware.type.watch")

        if (isWatch) {
            try {
                val activityClass = Class.forName("digital.tonima.kairos.wear.WearAlarmActivity")
                val activityIntent =
                    Intent(context, activityClass).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        putExtra(EXTRA_EVENT_TITLE, eventTitle)
                        putExtra(EXTRA_UNIQUE_ID, uniqueId)
                        putExtra(EXTRA_EVENT_ID, eventId)
                        putExtra(EXTRA_EVENT_START_TIME, startTime)
                        putExtra(EXTRA_MEETING_URL, meetingUrl)
                    }
                context.startActivity(activityIntent)
                return
            } catch (e: Exception) {
                logcat(logcat.LogPriority.ERROR) { "Failed to start WearAlarmActivity: ${e.message}" }
                // Fallback para o serviço se a atividade falhar
            }
        }

        AlarmSoundAndVibrateService.startAlarm(context, eventTitle, uniqueId, eventId, startTime, meetingUrl)
    }
}
