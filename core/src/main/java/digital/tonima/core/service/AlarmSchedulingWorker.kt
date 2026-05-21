package digital.tonima.core.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.permissions.PermissionManager
import digital.tonima.core.repository.AppPreferencesRepository
import digital.tonima.core.repository.AudioWarningState
import digital.tonima.core.repository.RingerModeRepository
import digital.tonima.core.usecases.CalculateDepartureTimeUseCase
import digital.tonima.core.usecases.GetEventsForMonthUseCase
import digital.tonima.core.utils.NotificationHelper
import digital.tonima.kairos.core.R.string
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class AlarmSchedulingWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
        private val scheduler: EventAlarmScheduler,
        private val permissionManager: PermissionManager,
        private val appPreferencesRepository: AppPreferencesRepository,
        private val proUserProvider: ProUserProvider,
        private val calculateDepartureTimeUseCase: CalculateDepartureTimeUseCase,
        private val ringerModeRepository: RingerModeRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                logcat { "Worker iniciado para verificação periódica." }
                try {
                    if (!permissionManager.hasCalendarPermission()) {
                        logcat(LogPriority.WARN) {
                            "Permissão READ_CALENDAR não concedida. O Worker não pode continuar."
                        }
                        return@withContext Result.success()
                    }

                    val isGlobalAlarmEnabled = appPreferencesRepository.isGlobalAlarmEnabled().firstOrNull() ?: true
                    if (!isGlobalAlarmEnabled) {
                        logcat(LogPriority.INFO) { "Alarmes globais estão desativados. Worker encerrando sem agendar." }
                        return@withContext Result.success()
                    }

                    val offsetMinutes = appPreferencesRepository.getAlarmOffsetMinutes().firstOrNull() ?: 0L
                    val allDayAlarmsEnabled = appPreferencesRepository.isAllDayAlarmsEnabled().firstOrNull() ?: true
                    val allDayAlarmHour = appPreferencesRepository.getAllDayAlarmHour().firstOrNull() ?: 9

                    val currentMonthEvents = getEventsForMonthUseCase.invoke(YearMonth.now())
                    val nextMonthEvents = getEventsForMonthUseCase.invoke(YearMonth.now().plusMonths(1))
                    val allUpcomingEvents = currentMonthEvents + nextMonthEvents

                    logcat { "Encontrados ${allUpcomingEvents.size} eventos no total para os próximos 2 meses." }

                    val disabledInstanceIds = appPreferencesRepository.getDisabledEventIds().firstOrNull() ?: emptySet()
                    val disabledSeriesIds = appPreferencesRepository.getDisabledSeriesIds().firstOrNull() ?: emptySet()

                    val now = System.currentTimeMillis()
                    val scheduleWindowEnd =
                        now +
                            TimeUnit.MINUTES.toMillis(75) +
                            TimeUnit.MINUTES.toMillis(offsetMinutes)
                    val sdf = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

                    logcat {
                        "Verificando eventos na janela de tempo: " +
                            "${sdf.format(Date(now))} até ${sdf.format(Date(scheduleWindowEnd))}"
                    }

                    val eventsToSchedule =
                        allUpcomingEvents
                            .filter { event ->
                                if (event.isAllDay) {
                                    if (!allDayAlarmsEnabled) return@filter false

                                    val eventDate =
                                        Instant.ofEpochMilli(event.startTime)
                                            .atZone(ZoneId.of("UTC"))
                                            .toLocalDate()
                                    val alarmFireTime =
                                        eventDate
                                            .atTime(LocalTime.of(allDayAlarmHour, 0))
                                            .atZone(ZoneId.systemDefault())
                                            .toInstant()
                                            .toEpochMilli()
                                    alarmFireTime in (now + 1)..scheduleWindowEnd
                                } else {
                                    val alarmFireTime = event.startTime - TimeUnit.MINUTES.toMillis(offsetMinutes)
                                    alarmFireTime in (now + 1)..scheduleWindowEnd
                                }
                            }
                            .filter { event ->
                                val isInstanceDisabled = disabledInstanceIds.contains(event.uniqueIntentId.toString())
                                val isSeriesDisabled = disabledSeriesIds.contains(event.id.toString())
                                !isInstanceDisabled && !isSeriesDisabled
                            }

                    if (eventsToSchedule.isEmpty()) {
                        logcat(LogPriority.INFO) { "Nenhum evento encontrado para agendar nesta janela de tempo." }
                    } else {
                        logcat(LogPriority.INFO) { "Encontrados ${eventsToSchedule.size} evento(s) para agendar:" }
                        val isAiUser = proUserProvider.isAiUser.first()

                        eventsToSchedule.forEach { event ->
                            var triggerTime: Long? = null
                            if (isAiUser && event.location != null) {
                                logcat {
                                    "Dynamic Traffic Watch: Calculando " +
                                        "tempo de deslocamento para '${event.title}'"
                                }
                                triggerTime = calculateDepartureTimeUseCase(event)?.departureTime
                            }

                            logcat {
                                "Agendando alarme para o evento '${event.title}' em " +
                                    sdf.format(Date(event.startTime)) +
                                    if (triggerTime != null) {
                                        " (Trigger " +
                                            "dinâmico: ${sdf.format(Date(triggerTime))})"
                                    } else {
                                        ""
                                    }
                            }
                            scheduler.schedule(event, triggerTime)
                        }
                        if (eventsToSchedule.isNotEmpty()) {
                            checkDeviceHealth(isAiUser, eventsToSchedule.first())
                        }
                    }
                    logcat { "Worker concluído com sucesso." }
                    Result.success()
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "Worker falhou com uma exceção: ${e.localizedMessage}" }
                    Result.failure()
                }
            }

        private fun checkDeviceHealth(
            isAiUser: Boolean,
            nextEvent: digital.tonima.core.model.Event,
        ) {
            if (!isAiUser) return

            val batteryStatus: Intent? =
                IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                    applicationContext.registerReceiver(null, ifilter)
                }

            val batteryPct: Int =
                batteryStatus?.let { intent ->
                    val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    (level * 100 / scale.toFloat()).toInt()
                } ?: 100

            val isCharging: Boolean =
                batteryStatus?.let { intent ->
                    val status: Int = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                } ?: true

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val eventTimeStr = sdf.format(Date(nextEvent.startTime))

            if (batteryPct < 20 && !isCharging) {
                NotificationHelper.showDeviceHealthAlertNotification(
                    applicationContext,
                    applicationContext
                        .getString(
                            string.battery_low_warning,
                            batteryPct,
                            eventTimeStr,
                        ),
                )
            }

            val ringerMode = ringerModeRepository.ringerMode.value
            if (ringerMode == AudioWarningState.SILENT || ringerMode == AudioWarningState.ALARM_MUTED) {
                NotificationHelper.showDeviceHealthAlertNotification(
                    applicationContext,
                    applicationContext.getString(string.silent_mode_warning, eventTimeStr),
                )
            }
        }
    }
