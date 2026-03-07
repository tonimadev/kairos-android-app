package digital.tonima.core.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import digital.tonima.core.service.AlarmSoundAndVibrateService
import digital.tonima.core.service.EventAlarmScheduler
import digital.tonima.kairos.core.R
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
        const val ACTION_SNOOZE = "digital.tonima.core.ACTION_SNOOZE"
    }

    @Inject
    lateinit var scheduler: EventAlarmScheduler

    @SuppressLint("MissingPermission")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == ACTION_SNOOZE) {
            val eventTitle = intent.getStringExtra(EXTRA_EVENT_TITLE) ?: ""
            val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, -1)
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)

            AlarmSoundAndVibrateService.stopAlarm(context)

            try {
                val schedulerEntryPoint =
                    fromApplication(
                        context.applicationContext,
                        SchedulerEntryPoint::class.java,
                    )
                schedulerEntryPoint.scheduler().scheduleSnooze(eventTitle, uniqueId, eventId, startTime)
            } catch (e: Exception) {
                logcat { "Failed to access EventAlarmScheduler via Hilt: ${e.message}" }
            }
            return
        }

        val eventTitle =
            intent.getStringExtra(EXTRA_EVENT_TITLE) ?: context.getString(R.string.upcoming_event)
        val uniqueId = intent.getIntExtra(EXTRA_UNIQUE_ID, System.currentTimeMillis().toInt())
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
        val startTime = intent.getLongExtra(EXTRA_EVENT_START_TIME, -1L)

        // No Wear OS (ou se o contexto indicar hardware.type.watch), iniciamos a Activity diretamente
        // para evitar ForegroundServiceStartNotAllowedException
        val isWatch = context.packageManager.hasSystemFeature("android.hardware.type.watch")

        if (isWatch) {
            try {
                val activityClass = Class.forName("digital.tonima.kairos.wear.WearAlarmActivity")
                val activityIntent =
                    Intent(context, activityClass).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra(EXTRA_EVENT_TITLE, eventTitle)
                        putExtra(EXTRA_UNIQUE_ID, uniqueId)
                        putExtra(EXTRA_EVENT_ID, eventId)
                        putExtra(EXTRA_EVENT_START_TIME, startTime)
                    }
                context.startActivity(activityIntent)
                return
            } catch (e: Exception) {
                logcat(logcat.LogPriority.ERROR) { "Failed to start WearAlarmActivity: ${e.message}" }
                // Fallback para o serviço se a atividade falhar
            }
        }

        AlarmSoundAndVibrateService.startAlarm(context, eventTitle, uniqueId, eventId, startTime)
    }
}
