package digital.tonima.core.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import digital.tonima.core.ai.repository.AiErrorType
import digital.tonima.core.ai.usecases.BriefingResult
import digital.tonima.core.ai.usecases.GenerateDailyBriefingUseCase
import digital.tonima.core.data.repository.CalendarRepository
import digital.tonima.core.delegates.ProUserProvider
import digital.tonima.core.utils.NotificationHelper
import digital.tonima.core.utils.WidgetUpdater
import digital.tonima.kairos.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import logcat.LogPriority
import logcat.logcat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId.systemDefault
import java.time.format.DateTimeFormatter.ofPattern

@HiltWorker
class DailyBriefingWorker
    @AssistedInject
    constructor(
        @Assisted private val appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val generateDailyBriefingUseCase: GenerateDailyBriefingUseCase,
        private val calendarRepository: CalendarRepository,
        private val widgetUpdater: WidgetUpdater,
        private val proUserProvider: ProUserProvider,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                logcat { "DailyBriefingWorker iniciado." }

                try {
                    // Verificar se o usuário tem acesso à IA
                    val isAiUser = proUserProvider.isAiUser.first()
                    if (!isAiUser) {
                        logcat(LogPriority.INFO) { "Usuário não possui plano de IA. Ignorando Daily Briefing." }
                        return@withContext Result.success()
                    }

                    // Buscar eventos de hoje
                    val today = LocalDate.now()
                    val events =
                        calendarRepository.getEventsForMonth(
                            YearMonth.from(today).atDay(1).toEpochDay(),
                        )
                            .filter { event ->
                                val eventDate =
                                    java.time.Instant.ofEpochMilli(event.startTime)
                                        .atZone(systemDefault())
                                        .toLocalDate()
                                eventDate == today
                            }

                    if (events.isEmpty()) {
                        logcat(LogPriority.INFO) { "Nenhum evento para hoje. Pulando notificação." }
                        return@withContext Result.success()
                    }

                    // Gerar resumo via IA
                    val languageInstruction = appContext.getString(R.string.ai_briefing_instruction)
                    val wakeUpTime = java.time.LocalTime.now()
                    val wakeUpTimeStr = wakeUpTime.format(ofPattern("HH:mm"))

                    val briefingResult =
                        generateDailyBriefingUseCase.invoke(events, languageInstruction, wakeUpTimeStr)
                    when (briefingResult) {
                        is BriefingResult.Success -> {
                            widgetUpdater.updateDailyBriefingWidget()
                            val title = appContext.getString(R.string.daily_briefing_title)
                            NotificationHelper.showDailyBriefingNotification(appContext, title, briefingResult.text)
                            logcat(LogPriority.INFO) { "Notificação de Daily Briefing enviada com sucesso." }
                            Result.success()
                        }
                        is BriefingResult.Cached -> {
                            // Already generated (and notified, if applicable) earlier today.
                            logcat(LogPriority.INFO) { "Resumo de hoje já existe em cache. Nada a fazer." }
                            Result.success()
                        }
                        is BriefingResult.Error -> {
                            logcat(LogPriority.WARN) { "Falha ao gerar resumo da IA: ${briefingResult.errorType}" }
                            val isTransient =
                                briefingResult.errorType == AiErrorType.NETWORK ||
                                    briefingResult.errorType == AiErrorType.RATE_LIMITED
                            if (isTransient) {
                                Result.retry()
                            } else {
                                Result.failure()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR) { "Erro no DailyBriefingWorker: ${e.localizedMessage}" }
                    Result.retry()
                }
            }
    }
