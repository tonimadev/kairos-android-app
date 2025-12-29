package digital.tonima.core.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.paulrybitskyi.hiltbinder.BindType
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.core.model.Event
import digital.tonima.core.receiver.AlarmReceiver
import digital.tonima.core.receiver.AlarmReceiver.Companion.ACTION_ALARM_TRIGGERED
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_ID
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_START_TIME
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_EVENT_TITLE
import digital.tonima.core.receiver.AlarmReceiver.Companion.EXTRA_UNIQUE_ID
import digital.tonima.core.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@BindType(installIn = BindType.Component.SINGLETON, to = EventAlarmScheduler::class)
class EventAlarmSchedulerImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val appPreferencesRepository: AppPreferencesRepository
) : EventAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(event: Event) {
        // Verificar se alarmes para eventos all-day estão habilitados
        val allDayAlarmsEnabled = runBlocking {
            appPreferencesRepository.isAllDayAlarmsEnabled().firstOrNull() ?: false
        }

        if (event.isAllDay && !allDayAlarmsEnabled) {
            logcat {
                "Skipping alarm scheduling for all-day event (disabled by user): ${event.title}"
            }
            return
        }

        // Calcular o horário correto do alarme
        val alarmTime = if (event.isAllDay) {
            // Para eventos all-day, usar o horário configurado pelo usuário (padrão: 9h)
            val alarmHour = runBlocking {
                appPreferencesRepository.getAllDayAlarmHour().firstOrNull() ?: 9
            }

            // Converter o timestamp UTC do evento para a data local
            val eventDate = Instant.ofEpochMilli(event.startTime)
                .atZone(ZoneId.of("UTC"))
                .toLocalDate()

            // Criar timestamp para o horário configurado no timezone local
            val alarmDateTime = eventDate.atTime(alarmHour, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            logcat {
                "All-day event '${event.title}': original=${event.startTime}, alarm=$alarmDateTime (${alarmHour}:00)"
            }

            alarmDateTime
        } else {
            event.startTime
        }

        logcat {
            "Scheduling alarm for event: ${event.title} at $alarmTime with ID: ${event.uniqueIntentId}"
        }
        val canSchedule =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

        if (canSchedule) {
            val intent =
                Intent(context, AlarmReceiver::class.java).apply {
                    action = ACTION_ALARM_TRIGGERED
                    data = "kairos://alarm/${event.uniqueIntentId}".toUri()

                    putExtra(EXTRA_EVENT_TITLE, event.title)
                    putExtra(EXTRA_UNIQUE_ID, event.uniqueIntentId)
                    putExtra(EXTRA_EVENT_ID, event.id)
                    putExtra(EXTRA_EVENT_START_TIME, event.startTime)
                }
            val pendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    event.uniqueIntentId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
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
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        alarmManager.cancel(pendingIntent)
    }
}
